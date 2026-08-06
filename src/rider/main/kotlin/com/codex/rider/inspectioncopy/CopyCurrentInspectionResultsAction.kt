package com.codex.rider.inspectioncopy

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.openapi.project.DumbAwareAction
import com.jetbrains.rd.ide.model.Solution
import com.jetbrains.rd.ide.model.inspectionCopyModel
import com.jetbrains.rd.util.lifetime.Lifetime
import java.awt.datatransfer.StringSelection
import java.lang.reflect.Method
import java.util.Collections
import java.util.IdentityHashMap
import java.util.LinkedHashSet
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import javax.swing.JComponent
import javax.swing.JTree
import javax.swing.tree.TreeNode

/** Copies the already displayed Code Issues occurrences; it never runs or navigates. */
class CopyCurrentInspectionResultsAction(
    private val panel: JComponent,
    private val tree: JTree
) : DumbAwareAction(
    "Copy Inspection Results with Line Numbers",
    "Copy the displayed Rider inspection results with file paths and line numbers",
    AllIcons.Actions.Copy
) {
    private val log = Logger.getInstance(CopyCurrentInspectionResultsAction::class.java)
    private val waiting = AtomicBoolean(false)
    private val pendingRequest = AtomicReference<String?>(null)
    private var subscribed = false

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabled = tree.rowCount > 0 && !waiting.get()
    }

    override fun actionPerformed(e: AnActionEvent) {
        val indices = collectIssueIndices()
        log.info(
            "Copy invoked: tree=${tree.javaClass.name}, model=${tree.model.javaClass.name}, " +
                "root=${tree.model.root?.javaClass?.name}, rows=${tree.rowCount}, " +
                "selection=${tree.selectionCount}, indices=${indices.joinToString(",")}"
        )
        if (indices.isEmpty()) {
            log.warn("No IssueModel indices were found in the displayed Code Issues tree")
            return
        }

        val project = e.project ?: run {
            log.warn("Copy action has no project in AnActionEvent")
            return
        }
        val solution = getProtocolSolution(project) ?: run {
            log.warn("Could not resolve Rider protocol Solution")
            return
        }
        val model = solution.inspectionCopyModel
        val resultModelId = inspectionResultModelId() ?: run {
            log.warn("Could not resolve the standard InspectionResultsModel rdId from Code Issues panel")
            return
        }

        subscribe(model)
        val requestId = java.util.UUID.randomUUID().toString()
        pendingRequest.set(requestId)
        waiting.set(true)
        log.info("Sending copy request: resultModelId=$resultModelId, issueCount=${indices.size}")
        try {
            model.start.fire("$resultModelId|$requestId|${indices.joinToString(",")}")
        } catch (t: Throwable) {
            waiting.set(false)
            log.warn("Could not send inspection-copy request", t)
        }
    }

    private fun subscribe(model: Any) {
        if (subscribed) return
        subscribed = true

        val typedModel = model as com.jetbrains.rd.ide.model.InspectionCopyModel
        typedModel.result.advise(Lifetime.Eternal) { report ->
            val payload = responsePayload(report) ?: return@advise
            if (waiting.compareAndSet(true, false)) {
                log.info("Received inspection-copy result: ${payload.length} characters")
                copyToClipboard(payload)
            }
        }
        typedModel.error.advise(Lifetime.Eternal) { error ->
            val payload = responsePayload(error) ?: return@advise
            if (waiting.compareAndSet(true, false)) {
                log.warn("Received inspection-copy error: $payload")
                copyToClipboard(payload)
            }
        }
    }

    private fun responsePayload(response: String?): String? {
        if (response.isNullOrBlank()) return null
        val separator = response.indexOf('|')
        if (separator <= 0) return null
        val requestId = pendingRequest.get() ?: return null
        if (response.substring(0, separator) != requestId) return null
        return response.substring(separator + 1)
    }

    private fun copyToClipboard(text: String) {
        ApplicationManager.getApplication().invokeLater {
            if (!ApplicationManager.getApplication().isDisposed) {
                CopyPasteManager.getInstance().setContents(StringSelection(text))
                log.info("Inspection-copy text placed into CopyPasteManager: ${text.length} characters")
            }
        }
    }

    private fun collectIssueIndices(): List<Int> {
        val selection = tree.selectionPaths
        val roots = Collections.newSetFromMap(IdentityHashMap<TreeNode, Boolean>())
        selection?.forEach { path ->
            (path.lastPathComponent as? TreeNode)?.let { roots.add(it) }
        }
        if (roots.isEmpty()) {
            (tree.model.root as? TreeNode)?.let { roots.add(it) }
        }

        val seen = Collections.newSetFromMap(IdentityHashMap<TreeNode, Boolean>())
        val result = LinkedHashSet<Int>()
        roots.forEach { collect(it, seen, result) }
        return result.toList()
    }

    private fun collect(
        node: TreeNode,
        seen: MutableSet<TreeNode>,
        result: MutableSet<Int>
    ) {
        if (!seen.add(node)) return
        issueIndex(node)?.let { result.add(it) }
        for (i in 0 until node.childCount) {
            (node.getChildAt(i) as? TreeNode)?.let { collect(it, seen, result) }
        }
    }

    private fun issueIndex(node: TreeNode): Int? {
        val values = listOfNotNull(
            invokeNoArg(node, "getGroupingObject"),
            invokeNoArg(node, "getUserObject"),
            invokeNoArg(node, "getValue"),
            invokeNoArg(node, "getElement")
        )
        for (value in values) {
            if (value.javaClass.name.endsWith("IssueModel")) {
                val index = invokeInt(value, "getIndex", -1)
                if (index >= 0) return index
            }
        }
        return null
    }

    private fun getProtocolSolution(project: com.intellij.openapi.project.Project): Solution? {
        return try {
            val host = Class.forName("com.jetbrains.rider.projectView.SolutionHostExtensionsKt")
            val method = host.getMethod("getSolution", com.intellij.openapi.project.Project::class.java)
            method.invoke(null, project) as? Solution
        } catch (_: ReflectiveOperationException) {
            null
        }
    }

    private fun inspectionResultModelId(): Long? {
        var type: Class<*>? = panel.javaClass
        while (type != null) {
            try {
                val field = type.declaredFields.firstOrNull {
                    it.name == "model" && it.type.name.endsWith("InspectionResultsModel")
                }
                if (field != null) {
                    field.isAccessible = true
                    val resultModel = field.get(panel) ?: return null
                    log.info("Found standard inspection result model: ${resultModel.javaClass.name}")
                    return rdId(resultModel)
                }
            } catch (_: ReflectiveOperationException) {
                return null
            } catch (_: SecurityException) {
                return null
            }
            type = type.superclass
        }
        return null
    }

    private fun rdId(model: Any): Long? {
        return try {
            val method = model.javaClass.methods.firstOrNull {
                it.name.startsWith("getRdid") && it.parameterCount == 0
            } ?: return null
            (method.invoke(model) as? Number)?.toLong()
        } catch (_: ReflectiveOperationException) {
            null
        }
    }

    private fun invokeNoArg(value: Any, name: String): Any? {
        var type: Class<*>? = value.javaClass
        while (type != null) {
            try {
                val method = type.declaredMethods.firstOrNull {
                    it.name == name && it.parameterCount == 0
                }
                if (method != null) {
                    method.isAccessible = true
                    return method.invoke(value)
                }
            } catch (_: ReflectiveOperationException) {
                return null
            } catch (_: SecurityException) {
                return null
            }
            type = type.superclass
        }
        return null
    }

    private fun groupingObject(node: TreeNode): Any? {
        return try {
            invokeNoArg(node, "getGroupingObject")
        } catch (_: RuntimeException) {
            null
        }
    }

    private fun invokeInt(value: Any, name: String, fallback: Int): Int {
        return try {
            (invokeNoArg(value, name) as? Number)?.toInt() ?: fallback
        } catch (_: RuntimeException) {
            fallback
        }
    }
}
