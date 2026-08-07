package com.codex.rider.inspectioncopy

import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.project.DumbAwareAction

/**
 * Project-view entry point. The actual inspection is performed by Rider's
 * own InspectOnProjectModelAction, so this action does not start a second
 * inspection and opens the standard Code Issues tab.
 */
class InspectBenchmarkProjectAction : DumbAwareAction(
    "Inspect Code (copyable results)",
    "Run Rider's standard project inspection and keep locations available for copying",
    null
) {
    override fun actionPerformed(e: AnActionEvent) {
        val standard = ActionManager.getInstance().getAction("InspectOnProjectModelAction")
            ?: ActionManager.getInstance().getAction("InspectCode")
            ?: return

        val contextComponent = e.getData(PlatformDataKeys.CONTEXT_COMPONENT)
        ActionManager.getInstance().tryToExecute(standard, e.inputEvent, contextComponent, e.place, true)
    }
}
