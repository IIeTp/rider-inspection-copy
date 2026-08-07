package com.codex.rider.inspectioncopy

import com.intellij.ide.plugins.PluginManagerCore
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.extensions.PluginId
import kotlin.test.Test
import kotlin.test.assertNotNull

class RiderPluginIntegrationTest {
    @Test
    fun pluginAndProjectInspectionActionAreLoaded() {
        val plugin = PluginManagerCore.getPlugin(PluginId.getId("com.codex.inspectioncopy"))

        assertNotNull(plugin, "Inspection Copy plugin is not loaded in the Rider test instance")
        assertNotNull(
            ActionManager.getInstance().getAction("Codex.InspectBenchmarkProject"),
            "Project inspection action is not registered"
        )
    }
}
