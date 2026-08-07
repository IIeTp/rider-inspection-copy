import com.jetbrains.plugin.structure.base.utils.isFile
import dev.detekt.gradle.Detekt
import org.gradle.api.tasks.Exec
import org.gradle.api.tasks.Sync
import org.jetbrains.intellij.platform.gradle.TestFrameworkType

plugins {
    id("java")
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.detekt)
    id("org.jetbrains.intellij.platform") version "2.18.1"     // See https://github.com/JetBrains/intellij-platform-gradle-plugin/releases
    id("me.filippov.gradle.jvm.wrapper") version "0.15.0"
}

val DotnetSolution: String by project
val BuildConfiguration: String by project
val ProductVersion: String by project
val DotnetPluginId: String by project
val RiderPluginId: String by project
val PublishToken: String by project
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
    // This plugin does not contribute Settings pages. Running the searchable-options
    // indexer starts unrelated Rider settings pages and produces Rider SDK errors.
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
ideTestSourceSet.compileClasspath += sourceSets.main.get().output
ideTestSourceSet.runtimeClasspath += sourceSets.main.get().output

val compileDotNet by tasks.registering(Exec::class) {
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

val copyPluginDistribution by tasks.registering(Sync::class) {
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
        // CI resolves the Rider SDK from JetBrains' repository. For local development,
        // pass -PriderPath="C:/Program Files/JetBrains/Rider2026.1" to use an existing IDE.
        if (riderPath.isNullOrBlank()) {
            rider(ProductVersion) {
                useInstaller = false
            }
        } else {
            local(riderPath!!)
        }

        // Rider does not publish its platform test framework as a Maven artifact.
        // Use the framework bundled in the Rider distribution for platform tests.
        testFramework(TestFrameworkType.Bundled)

        // TODO: add plugins
        // bundledPlugin("uml")
        // bundledPlugin("com.jetbrains.ChooseRuntime:1.0.9")
    }
}

tasks.runIde {
    // Match Rider's default heap size of 1.5Gb (default for runIde is 512Mb)
    maxHeapSize = "1500m"
}

tasks.patchPluginXml {
    // TODO: See also org.jetbrains.changelog: https://github.com/JetBrains/gradle-changelog-plugin
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

val riderIdeTest by intellijPlatformTesting.testIde.registering {
    task {
        description = "Runs Rider platform tests with the built plugin installed"
        testClassesDirs = ideTestSourceSet.output.classesDirs
        classpath = ideTestSourceSet.runtimeClasspath
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

val riderModel: Configuration by configurations.creating {
    isCanBeConsumed = true
    isCanBeResolved = false
}

artifacts {
    add(riderModel.name, provider {
        file("${rootDir}/tools/rider-model.jar").also {
            check(it.isFile) {
                "rider-model.jar is not found at $riderModel"
            }
        }
    })
}
