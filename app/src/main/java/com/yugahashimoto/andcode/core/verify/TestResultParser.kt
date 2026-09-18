package com.yugahashimoto.andcode.core.verify

object TestResultParser {

    private val testCasePattern = Regex("""<testcase\s+name="([^"]+)"\s+classname="([^"]+)"(?:\s+time="([^"]+)")?""")
    private val failurePattern = Regex("""<failure\s+message="([^"]*)"[^>]*>([\s\S]*?)</failure>""")
    private val testSuitePattern = Regex("""<testsuite\s+.*?tests="(\d+)"\s+failures="(\d+)"\s+errors="(\d+)""")

    fun parseXml(xml: String): List<TestResult> {
        val results = mutableListOf<TestResult>()
        val testCases = testCasePattern.findAll(xml).toList()

        for (match in testCases) {
            val name = match.groupValues[1]
            val className = match.groupValues[2]
            val duration = parseDuration(match.groupValues[3])

            val remainingXml = xml.substring(match.range.last)
            val failureMatch = failurePattern.find(remainingXml)

            results.add(
                TestResult(
                    name = name,
                    className = className,
                    passed = failureMatch == null,
                    durationMillis = duration,
                    errorMessage = failureMatch?.groupValues?.get(1),
                    stackTrace = failureMatch?.groupValues?.get(2)?.trim()?.take(500),
                )
            )
        }

        return results
    }

    fun parseTextOutput(output: String): List<TestResult> {
        val results = mutableListOf<TestResult>()
        val passPattern = Regex("""(PASS|ok)\s+(.+)""")
        val failPattern = Regex("""(FAIL)\s+(.+?)(?:\s+\((.+?)\))?""")

        for (line in output.lines()) {
            passPattern.find(line)?.let { match ->
                results.add(
                    TestResult(
                        name = match.groupValues[2].trim(),
                        className = "",
                        passed = true,
                    )
                )
            }
            failPattern.find(line)?.let { match ->
                results.add(
                    TestResult(
                        name = match.groupValues[2].trim(),
                        className = "",
                        passed = false,
                        errorMessage = match.groupValues[3].ifBlank { null },
                    )
                )
            }
        }

        return results
    }

    fun parseSummary(xml: String): Pair<Int, Int> {
        val match = testSuitePattern.find(xml) ?: return Pair(0, 0)
        val tests = match.groupValues[1].toIntOrNull() ?: 0
        val failures = match.groupValues[2].toIntOrNull() ?: 0
        return Pair(tests, failures)
    }

    private fun parseDuration(timeStr: String): Long {
        return (timeStr.toDoubleOrNull() ?: 0.0).times(1000).toLong()
    }
}
