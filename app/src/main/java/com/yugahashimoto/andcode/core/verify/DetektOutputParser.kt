package com.yugahashimoto.andcode.core.verify

object DetektOutputParser {

    private val sarifMessagePattern = Regex("""\{"ruleId":"([^"]+)","message":"([^"]+)"[^}]*"locations":\[\{"physicalLocation":\{"artifactLocation":\{"uri":"([^"]+)"\},"region":\{"startLine":(\d+),"startColumn":(\d+)""")

    fun parseSarif(output: String): List<LintViolation> {
        val violations = mutableListOf<LintViolation>()
        for (match in sarifMessagePattern.findAll(output)) {
            violations.add(
                LintViolation(
                    rule = match.groupValues[1],
                    message = match.groupValues[2],
                    file = match.groupValues[3],
                    line = match.groupValues[4].toIntOrNull() ?: 0,
                    column = match.groupValues[5].toIntOrNull() ?: 0,
                    severity = VerifySeverity.WARNING,
                )
            )
        }
        return violations
    }

    private val htmlRowPattern = Regex("""<tr[^>]*>.*?<td[^>]*>(.*?)</td>.*?<td[^>]*>(.*?)</td>.*?<td[^>]*>(.*?)</td>.*?<td[^>]*>(.*?)</td>.*?<td[^>]*>(.*?)</td>.*?</tr>""", setOf(RegexOption.DOT_MATCHES_ALL))

    fun parseHtml(output: String): List<LintViolation> {
        val violations = mutableListOf<LintViolation>()
        for (match in htmlRowPattern.findAll(output)) {
            val values = match.groupValues.drop(1).map { it.trim() }
            if (values.size >= 5) {
                violations.add(
                    LintViolation(
                        file = values[0],
                        line = values[1].toIntOrNull() ?: 0,
                        rule = values[2],
                        message = values[3],
                        severity = when (values[4].lowercase()) {
                            "error" -> VerifySeverity.ERROR
                            "warning" -> VerifySeverity.WARNING
                            else -> VerifySeverity.INFO
                        },
                    )
                )
            }
        }
        return violations
    }

    fun parseText(output: String): List<LintViolation> {
        val violations = mutableListOf<LintViolation>()
        val linePattern = Regex("""(.+?):(\d+):(\d+): (\w+) - (.+) \[(.+)]""")
        for (line in output.lines()) {
            linePattern.find(line)?.let { match ->
                violations.add(
                    LintViolation(
                        file = match.groupValues[1],
                        line = match.groupValues[2].toIntOrNull() ?: 0,
                        column = match.groupValues[3].toIntOrNull() ?: 0,
                        severity = when (match.groupValues[4].lowercase()) {
                            "error" -> VerifySeverity.ERROR
                            "warning" -> VerifySeverity.WARNING
                            else -> VerifySeverity.INFO
                        },
                        message = match.groupValues[5],
                        rule = match.groupValues[6],
                    )
                )
            }
        }
        return violations
    }
}
