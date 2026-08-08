package com.codex.rider.inspectioncopy

import com.intellij.ide.plugins.PluginManagerCore
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.application.ApplicationInfo
import com.intellij.openapi.extensions.PluginId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class RiderPluginIntegrationTest {
    @Test
    fun riderProductAndPluginAreLoaded() {
        assertEquals(
            "RD",
            ApplicationInfo.getInstance().build.productCode,
            "The integration test must run inside Rider"
        )

        val plugin = PluginManagerCore.getPlugin(PluginId.getId("com.codex.inspectioncopy"))
        assertNotNull(
            plugin,
            "Inspection Copy plugin is not loaded in the Rider test instance"
        )
    }

    @Test
    fun projectInspectionActionIsRegistered() {
        val action = ActionManager.getInstance().getAction("Codex.InspectBenchmarkProject")
        assertNotNull(
            action,
            "Project inspection action is not registered"
        )
        assertEquals(
            "com.codex.rider.inspectioncopy.InspectBenchmarkProjectAction",
            action.javaClass.name,
            "Project inspection action is registered with an unexpected implementation"
        )
    }
}
