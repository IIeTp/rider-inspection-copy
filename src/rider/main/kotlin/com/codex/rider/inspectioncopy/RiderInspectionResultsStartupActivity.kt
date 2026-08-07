package com.codex.rider.inspectioncopy

import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupActivity
import com.intellij.openapi.ui.SimpleToolWindowPanel
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.ui.PopupHandler
import com.intellij.ui.content.ContentManager
import java.awt.Component
import java.awt.Container
import java.util.Collections
import java.util.IdentityHashMap
import javax.swing.JComponent
import javax.swing.JTree
import javax.swing.Timer

/** Adds a copy-only action to Rider's standard Code Issues result panel. */
class RiderInspectionResultsStartupActivity : StartupActivity.DumbAware {
    override fun runActivity(project: Project) {
        val installer = Installer(project)
        Disposer.register(project, installer)
        installer.start()
    }

    private class Installer(private val project: Project) : com.intellij.openapi.Disposable {
        private val installedPanels: MutableSet<JComponent> =
            Collections.newSetFromMap(IdentityHashMap())
        private var timer: Timer? = null

        fun start() {
            ApplicationManager.getApplication().invokeLater {
                if (project.isDisposed) return@invokeLater

                timer = Timer(500) { scan() }.apply {
                    initialDelay = 250
                    start()
                }
                scan()
            }
        }

        private fun scan() {
            if (project.isDisposed) return

            val toolWindow = ToolWindowManager.getInstance(project)
                .getToolWindow(PROBLEMS_TOOL_WINDOW_ID) ?: return
            val contentManager: ContentManager = toolWindow.contentManager

            for (content in contentManager.contents) {
                val panel = findRiderResultPanel(content.component)
                if (panel != null && installedPanels.add(panel)) {
                    install(panel)
                }
            }
        }

        private fun install(panel: JComponent) {
            if (panel !is SimpleToolWindowPanel) return

            val tree = findTree(panel) ?: return
            val copyAction = CopyCurrentInspectionResultsAction(panel, tree)
            val actionGroup = DefaultActionGroup(copyAction)
            PopupHandler.installPopupMenu(tree, actionGroup, "Codex.RiderInspectionCopy.Popup")
        }

        override fun dispose() {
            timer?.stop()
            timer = null
            installedPanels.clear()
        }

        companion object {
            private const val PROBLEMS_TOOL_WINDOW_ID = "Problems View"
            private const val RIDER_RESULT_PANEL =
                "com.jetbrains.rider.inspections.RiderInspectionsResultPanel"

            private fun findRiderResultPanel(component: Component): JComponent? {
                if (component is JComponent && component.javaClass.name == RIDER_RESULT_PANEL) {
                    return component
                }
                if (component is Container) {
                    for (child in component.components) {
                        findRiderResultPanel(child)?.let { return it }
                    }
                }
                return null
            }

            private fun findTree(component: Component): JTree? {
                if (component is JTree) return component
                if (component is Container) {
                    for (child in component.components) {
                        findTree(child)?.let { return it }
                    }
                }
                return null
            }
        }
    }
}
