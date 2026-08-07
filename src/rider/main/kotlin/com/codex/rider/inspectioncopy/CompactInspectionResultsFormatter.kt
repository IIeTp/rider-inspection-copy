package com.codex.rider.inspectioncopy

internal object CompactInspectionResultsFormatter {
    private val issuePattern = Regex(
        """^(?<path>.+):(?<startLine>\d+):(?<startColumn>\d+)-""" +
            """(?<endLine>\d+):(?<endColumn>\d+)\s(?<message>.*)$"""
    )

    private data class Issue(
        val path: String,
        val range: String,
        val message: String
    )

    fun format(payload: String): String {
        val lines = payload.lineSequence()
            .map { it.removeSuffix("\r") }
            .filter { it.isNotBlank() }
            .toList()
        if (lines.isEmpty()) return payload

        val issues = lines.map { line ->
            val match = issuePattern.matchEntire(line) ?: return payload
            Issue(
                path = match.value("path"),
                range = "${match.value("startLine")}:${match.value("startColumn")}-" +
                    "${match.value("endLine")}:${match.value("endColumn")}",
                message = match.value("message")
            )
        }

        val directories = LinkedHashMap<String, LinkedHashMap<String, MutableList<Issue>>>()
        for (issue in issues) {
            val files = directories.getOrPut(parentOf(issue.path)) { LinkedHashMap() }
            files.getOrPut(issue.path) { mutableListOf() }.add(issue)
        }

        return buildString {
            directories.forEach { (directory, files) ->
                if (files.size == 1) {
                    val (path, fileIssues) = files.entries.first()
                    append(path).append('\n')
                    appendIssues(fileIssues)
                } else {
                    append(directory).append(separatorFor(files.keys.first())).append('\n')
                    files.forEach { (path, fileIssues) ->
                        append(fileName(path)).append('\n')
                        appendIssues(fileIssues)
                    }
                }
            }
            if (isNotEmpty()) setLength(length - 1)
        }
    }

    private fun StringBuilder.appendIssues(issues: List<Issue>) {
        val byMessage = LinkedHashMap<String, MutableList<String>>()
        issues.forEach { issue ->
            byMessage.getOrPut(issue.message) { mutableListOf() }.add(issue.range)
        }
        byMessage.forEach { (message, ranges) ->
            append(ranges.joinToString(",")).append(' ').append(message).append('\n')
        }
    }

    private fun parentOf(path: String): String {
        val separatorIndex = maxOf(path.lastIndexOf('\\'), path.lastIndexOf('/'))
        return if (separatorIndex >= 0) path.substring(0, separatorIndex) else path
    }

    private fun fileName(path: String): String {
        val separatorIndex = maxOf(path.lastIndexOf('\\'), path.lastIndexOf('/'))
        return if (separatorIndex >= 0) path.substring(separatorIndex + 1) else path
    }

    private fun separatorFor(path: String): Char = if (path.contains('\\')) '\\' else '/'

    private fun MatchResult.value(name: String): String = groups[name]?.value.orEmpty()
}
