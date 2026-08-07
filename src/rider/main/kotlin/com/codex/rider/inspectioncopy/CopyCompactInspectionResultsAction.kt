package com.codex.rider.inspectioncopy

import javax.swing.JComponent
import javax.swing.JTree

/** Copies the displayed Code Issues occurrences in a path-grouped compact format. */
class CopyCompactInspectionResultsAction(
    panel: JComponent,
    tree: JTree
) : AbstractCopyInspectionResultsAction(
    panel,
    tree,
    "Copy Inspection Results (Compact)",
    "Copy the displayed Rider inspection results in a compact path-grouped format"
) {
    override fun formatPayload(payload: String): String = CompactInspectionResultsFormatter.format(payload)
}
