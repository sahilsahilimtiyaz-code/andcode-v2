package com.yugahashimoto.andcode.core.verify

class VerifyPipeline(
    private val stepExecutor: suspend (VerifyStep, String) -> String,
    private val steps: List<VerifyStep> = VerifyStep.entries,
    private val timeoutMillis: Long = 300_000L,
) {
    suspend fun run(workspaceDir: String): VerifyReport {
        val results = mutableListOf<StepResult>()

        for (step in steps) {
            val startTime = System.currentTimeMillis()
            try {
                val output = stepExecutor(step, workspaceDir)
                val duration = System.currentTimeMillis() - startTime
                val stepResult = parseStepResult(step, output, duration)
                results.add(stepResult)

                if (!stepResult.success && step == VerifyStep.BUILD) {
                    break
                }
            } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
                results.add(
                    StepResult(
                        step = step,
                        success = false,
                        error = "Step timed out after ${timeoutMillis}ms",
                        durationMillis = System.currentTimeMillis() - startTime,
                    )
                )
                break
            } catch (e: Exception) {
                results.add(
                    StepResult(
                        step = step,
                        success = false,
                        error = e.message,
                        durationMillis = System.currentTimeMillis() - startTime,
                    )
                )
            }
        }

        return VerifyReport(results = results)
    }

    private fun parseStepResult(step: VerifyStep, output: String, durationMillis: Long): StepResult {
        return when (step) {
            VerifyStep.LINT -> {
                val violations = DetektOutputParser.parseText(output)
                StepResult(
                    step = step,
                    success = violations.none { it.severity == VerifySeverity.ERROR },
                    output = output,
                    durationMillis = durationMillis,
                    violations = violations,
                )
            }
            VerifyStep.FORMAT -> {
                val files = SpotlessOutputParser.parseCheckOutput(output)
                StepResult(
                    step = step,
                    success = SpotlessOutputParser.isAlreadyFormatted(output),
                    output = output,
                    durationMillis = durationMillis,
                    violations = files.map { file ->
                        LintViolation(
                            file = file,
                            line = 0,
                            rule = "spotless",
                            message = "File needs formatting",
                            severity = VerifySeverity.WARNING,
                        )
                    },
                )
            }
            VerifyStep.TEST -> {
                val testResults = TestResultParser.parseXml(output) +
                    TestResultParser.parseTextOutput(output)
                StepResult(
                    step = step,
                    success = testResults.all { it.passed },
                    output = output,
                    durationMillis = durationMillis,
                    testResults = testResults,
                )
            }
            VerifyStep.BUILD -> {
                val success = output.contains("BUILD SUCCESSFUL") && !output.contains("BUILD FAILED")
                StepResult(
                    step = step,
                    success = success,
                    output = output,
                    durationMillis = durationMillis,
                )
            }
        }
    }
}
