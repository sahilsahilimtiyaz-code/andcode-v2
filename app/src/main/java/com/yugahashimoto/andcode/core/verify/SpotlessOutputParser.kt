package com.yugahashimoto.andcode.core.verify

object SpotlessOutputParser {

    private val needsFormattingPattern = Regex("""TextField\((.+?)\)""")

    fun parseCheckOutput(output: String): List<String> {
        val files = mutableListOf<String>()
        for (match in needsFormattingPattern.findAll(output)) {
            files.add(match.groupValues[1].trim())
        }
        if (files.isEmpty()) {
            for (line in output.lines()) {
                if (line.contains("not formatted") || line.contains("would change")) {
                    val filePath = line.substringBefore(":").substringAfterLast(" ").trim()
                    if (filePath.isNotBlank() && filePath.contains("/")) {
                        files.add(filePath)
                    }
                }
            }
        }
        return files.distinct()
    }

    fun parseApplyOutput(output: String): Int {
        var count = 0
        for (line in output.lines()) {
            if (line.contains("Fixed") || line.contains("adjusted") || line.contains("cleaned")) {
                count++
            }
        }
        return count
    }

    fun isAlreadyFormatted(output: String): Boolean {
        return output.contains("Everything up-to-date") ||
            output.contains("all files are clean") ||
            output.contains("No formatting changes needed") ||
            output.lines().none { it.contains("would change") || it.contains("not formatted") }
    }
}
