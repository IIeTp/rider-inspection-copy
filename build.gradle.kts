import com.jetbrains.plugin.structure.base.utils.isFile
import dev.detekt.gradle.Detekt
import org.gradle.api.tasks.Exec
import org.gradle.api.tasks.Sync
import org.jetbrains.intellij.platform.gradle.TestFrameworkType
import org.jetbrains.intellij.platform.gradle.tasks.aware.SplitModeAware

plugins {
    id("java")
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.detekt)
    id("org.jetbrains.intellij.platform") version "2.18.1"
    id("me.filippov.gradle.jvm.wrapper") version "0.16.0"
}

val DotnetSolution = providers.gradleProperty("DotnetSolution").get()
val BuildConfiguration = providers.gradleProperty("BuildConfiguration").get()
val ProductVersion = providers.gradleProperty("ProductVersion").get()
val DotnetPluginId = providers.gradleProperty("DotnetPluginId").get()
val RiderPluginId = providers.gradleProperty("RiderPluginId").get()
val PublishToken = providers.gradleProperty("PublishToken").get()
val riderPath: String? = providers.gradleProperty("riderPath").orNull

allprojects {
    repositories {
        maven { setUrl("https://cache-redirector.jetbrains.com/maven-central") }
    }
}

repositories {
    intellijPlatform {
        defaultRepositories()
        jetbrainsRuntime()
    }
}

intellijPlatform {
    buildSearchableOptions = false
}

version = extra["PluginVersion"] as String

tasks.processResources {
    from("dependencies.json") { into("META-INF") }
}

sourceSets {
    main {
        kotlin.srcDir("src/rider/main/kotlin")
        resources.srcDir("src/rider/main/resources")
    }
    test {
        kotlin.srcDir("src/rider/test/kotlin")
    }
    create("ideTest") {
        kotlin.srcDir("src/rider/ideTest/kotlin")
    }
}

val ideTestSourceSet = sourceSets["ideTest"]
configurations["ideTestImplementation"].extendsFrom(configurations["testImplementation"])
configurations["ideTestRuntimeOnly"].extendsFrom(configurations["testRuntimeOnly"])
val mainSourceSet = sourceSets.main.get()
ideTestSourceSet.compileClasspath += mainSourceSet.output + mainSourceSet.compileClasspath
ideTestSourceSet.runtimeClasspath += mainSourceSet.output + mainSourceSet.runtimeClasspath

val compileDotNet = tasks.register<Exec>("compileDotNet") {
    description = "Build the ReSharper backend plugin"
    workingDir(layout.projectDirectory)
    executable("dotnet")
    args(
        "msbuild",
        DotnetSolution,
        "/t:Restore;Rebuild",
        "/p:Configuration=${BuildConfiguration}",
        "/p:HostFullIdentifier="
    )
}

val copyPluginDistribution = tasks.register<Sync>("copyPluginDistribution") {
    from(layout.buildDirectory.file("distributions/${rootProject.name}-${version}.zip"))
    into(layout.projectDirectory.dir("output"))
}

tasks.buildPlugin {
    finalizedBy(copyPluginDistribution)
}

dependencies {
    testImplementation(kotlin("test-junit5"))
    testRuntimeOnly(libs.junit4)

    intellijPlatform {
        if (riderPath.isNullOrBlank()) {
            rider(ProductVersion) {
                useInstaller = false
            }
        } else {
            local(riderPath!!)
        }
    }
}

tasks.runIde {
    maxHeapSize = "1500m"
}

tasks.patchPluginXml {
    val changelogText = file("${rootDir}/CHANGELOG.md").readText()
    val changelogMatches = Regex("(?s)(-.+?)(?=##|\$)").findAll(changelogText)

    changeNotes.set(changelogMatches.map {
        it.groups[1]!!.value.replace("(?s)\r?\n".toRegex(), "<br />\n")
    }.take(1).joinToString())
}

tasks.prepareSandbox {
    dependsOn(compileDotNet)

    val outputFolder = layout.projectDirectory.dir(
        "src/dotnet/${DotnetPluginId}/bin/${DotnetPluginId}.Rider/${BuildConfiguration}"
    )
    from(outputFolder.file("${DotnetPluginId}.dll")) { into("${rootProject.name}/dotnet") }
    from(outputFolder.file("${DotnetPluginId}.pdb")) { into("${rootProject.name}/dotnet") }
}

detekt {
    source.setFrom(files("src/rider/main/kotlin", "src/rider/test/kotlin"))
    config.setFrom(files("config/detekt/detekt.yml"))
    buildUponDefaultConfig = true
}

tasks.withType<Detekt>().configureEach {
    exclude("**/InspectionCopyModel.Generated.kt")
    reports {
        html.required.set(true)
        sarif.required.set(true)
    }
}

tasks.named("check") {
    dependsOn("detekt")
}

tasks.test {
    useJUnitPlatform {
        includeEngines("junit-jupiter")
    }
    jvmArgs("-Xshare:off")
}

val pluginDistributionPath = layout.buildDirectory.file(
    "distributions/${rootProject.name}-${project.version}.zip"
)

val riderIdeTest = intellijPlatformTesting.testIde.register("riderIdeTest") {
    splitMode = true
    pluginInstallationTarget = SplitModeAware.PluginInstallationTarget.FRONTEND

    // Rider's test-framework.jar is bundled in the Rider distribution and is not
    // published as a standalone Maven artifact. This dependency belongs to this
    // custom testIde entry so its test runtime gets the Rider framework.
    testFramework(TestFrameworkType.Bundled)

    plugins {
        localPlugin(pluginDistributionPath)
    }

    task {
        description = "Runs Rider integration tests with the built plugin installed"
        dependsOn(tasks.buildPlugin)
        testClassesDirs = ideTestSourceSet.output.classesDirs
        classpath += ideTestSourceSet.runtimeClasspath
        useJUnitPlatform {
            includeEngines("junit-jupiter")
        }
        jvmArgs("-Xshare:off")
    }
}

tasks.publishPlugin {
    dependsOn(tasks.buildPlugin)
    token.set("${PublishToken}")
}

val riderModel = configurations.create("riderModel") {
    isCanBeConsumed = true
    isCanBeResolved = false
}

artifacts {
    add(riderModel.name, provider {
        file("${rootDir}/tools/rider-model.jar").also {
            check(it.isFile) {
                "rider-model.jar is not found at $it"
            }
        }
    })
}
