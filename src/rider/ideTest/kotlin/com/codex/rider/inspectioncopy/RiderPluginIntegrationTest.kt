package com.codex.rider.inspectioncopy

import com.intellij.openapi.actionSystem.AnAction
import java.nio.file.Files
import java.nio.file.Path
import java.util.jar.JarFile
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class RiderPluginIntegrationTest {
    @Test
    fun riderSandboxUsesRiderPlatform() {
        val ideaHomePath = requireNotNull(System.getProperty("idea.home.path")) {
            "The Rider test task must provide idea.home.path"
        }
        val productInfo = Files.readString(Path.of(ideaHomePath).resolve("product-info.json"))

        assertContains(
            productInfo,
            "\"productCode\": \"RD\"",
            "The integration test must use Rider rather than another IntelliJ Platform product"
        )
    }

    @Test
    fun pluginArchiveContainsActionRegistrationAndBackend() {
        val actionClass = InspectBenchmarkProjectAction::class.java
        assertTrue(
            AnAction::class.java.isAssignableFrom(actionClass),
            "The Inspect Code action must be an IntelliJ action"
        )

        val pluginJar = Path.of(actionClass.protectionDomain.codeSource.location.toURI())
        assertTrue(Files.isRegularFile(pluginJar), "The action must be loaded from the packaged plugin JAR")

        JarFile(pluginJar.toFile()).use { archive ->
            val descriptorEntry = assertNotNull(
                archive.getJarEntry("META-INF/plugin.xml"),
                "The packaged plugin JAR must contain META-INF/plugin.xml"
            )
            val descriptor = archive.getInputStream(descriptorEntry).bufferedReader().use { it.readText() }

            assertContains(descriptor, "<id>com.codex.inspectioncopy</id>")
            assertContains(descriptor, "id=\"Codex.InspectBenchmarkProject\"")
            assertContains(
                descriptor,
                "com.codex.rider.inspectioncopy.InspectBenchmarkProjectAction",
                "plugin.xml must register the Inspect Code action implementation"
            )
        }

        val pluginDirectory = pluginJar.parent.parent
        val backendDirectory = pluginDirectory.resolve("dotnet")
        assertTrue(
            Files.isRegularFile(backendDirectory.resolve("ReSharperPlugin.InspectionCopyBackend.dll")),
            "The Rider sandbox must include the ReSharper backend assembly"
        )
        assertTrue(
            Files.isRegularFile(backendDirectory.resolve("ReSharperPlugin.InspectionCopyBackend.pdb")),
            "The Rider sandbox must include backend debugging symbols"
        )
    }
}
