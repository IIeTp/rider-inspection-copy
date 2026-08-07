package com.codex.rider.inspectioncopy

import kotlin.test.Test
import kotlin.test.assertEquals

class CompactInspectionResultsFormatterTest {
    @Test
    fun groupsFilesAndCombinesIdenticalMessages() {
        val input = """
            D:\\src\\Downloader\\PieceManager.cs:336:9-336:33 Inconsistent modifiers declaration order
            D:\\src\\Downloader\\PieceManager.cs:338:9-338:33 Inconsistent modifiers declaration order
            D:\\src\\Downloader\\DownloadStateStore.cs:75:9-75:14 Change the exception handling
        """.trimIndent()

        assertEquals(
            """
            D:\\src\\Downloader\\
            PieceManager.cs
            336:9-336:33,338:9-338:33 Inconsistent modifiers declaration order
            DownloadStateStore.cs
            75:9-75:14 Change the exception handling
            """.trimIndent(),
            CompactInspectionResultsFormatter.format(input)
        )
    }

    @Test
    fun keepsFullPathWhenDirectoryContainsOneFile() {
        val input = "C:\\repo\\Program.cs:12:4-12:10 Message contains: a colon"

        assertEquals(
            "C:\\repo\\Program.cs\n12:4-12:10 Message contains: a colon",
            CompactInspectionResultsFormatter.format(input)
        )
    }

    @Test
    fun returnsOriginalPayloadWhenALineCannotBeParsed() {
        val input = "not an inspection result"

        assertEquals(input, CompactInspectionResultsFormatter.format(input))
    }
}
