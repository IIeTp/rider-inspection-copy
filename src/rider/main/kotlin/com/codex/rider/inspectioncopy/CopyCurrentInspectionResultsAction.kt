package com.codex.rider.inspectioncopy

import javax.swing.JComponent
import javax.swing.JTree

/** Copies the displayed Code Issues occurrences without changing their format. */
class CopyCurrentInspectionResultsAction(
    panel: JComponent,
    tree: JTree
) : AbstractCopyInspectionResultsAction(
    panel,
    tree,
    "Copy Inspection Results",
    "Copy the displayed Rider inspection results with file paths and line numbers"
)
