package com.yugahashimoto.andcode.core.terminal

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class TerminalAutomationEngineTest {

    private lateinit var engine: TerminalAutomationEngine
    private val commandLog = mutableListOf<Triple<String, String?, Int>>()

    @Before
    fun setUp() {
        commandLog.clear()
        engine = TerminalAutomationEngine { cmd, dir, timeout ->
            commandLog.add(Triple(cmd, dir, timeout))
            0 to "ok"
        }
    }

    @Test
    fun `getScripts returns empty initially`() {
        assertTrue(engine.getScripts().isEmpty())
    }

    @Test
    fun `saveScript stores and retrieves script`() {
        val script = AutomationScript(
            id = "s1",
            name = "Test Script",
            steps = listOf(ScriptStep(id = "1", command = "echo hello")),
        )
        engine.saveScript(script)
        assertEquals(1, engine.getScripts().size)
        assertEquals("Test Script", engine.getScripts().first().name)
    }

    @Test
    fun `saveScript replaces existing with same id`() {
        engine.saveScript(AutomationScript(id = "s1", name = "V1", steps = emptyList()))
        engine.saveScript(AutomationScript(id = "s1", name = "V2", steps = emptyList()))
        assertEquals(1, engine.getScripts().size)
        assertEquals("V2", engine.getScripts().first().name)
    }

    @Test
    fun `deleteScript removes by id`() {
        engine.saveScript(AutomationScript(id = "s1", name = "S1", steps = emptyList()))
        engine.saveScript(AutomationScript(id = "s2", name = "S2", steps = emptyList()))
        engine.deleteScript("s1")
        assertEquals(1, engine.getScripts().size)
        assertEquals("s2", engine.getScripts().first().id)
    }

    @Test
    fun `getTemplates returns default templates`() {
        val templates = engine.getTemplates()
        assertTrue(templates.isNotEmpty())
        assertTrue(templates.any { it.id == "git_status" })
        assertTrue(templates.any { it.id == "gradle_build" })
        assertTrue(templates.any { it.id == "gradle_test" })
    }

    @Test
    fun `getTemplateById returns correct template`() {
        val template = engine.getTemplateById("git_status")
        assertNotNull(template)
        assertEquals("Git Status", template!!.name)
        assertEquals(TemplateCategory.GIT, template.category)
    }

    @Test
    fun `getTemplateById returns null for unknown id`() {
        assertNull(engine.getTemplateById("nonexistent"))
    }

    @Test
    fun `getOrCreateSession creates new session`() {
        val session = engine.getOrCreateSession("s1", "My Session", "/tmp")
        assertEquals("s1", session.id)
        assertEquals("My Session", session.name)
        assertEquals("/tmp", session.workingDirectory)
    }

    @Test
    fun `getOrCreateSession returns existing session`() {
        val s1 = engine.getOrCreateSession("s1", "V1", "/tmp")
        val s2 = engine.getOrCreateSession("s1", "V2", "/other")
        assertEquals(s1.id, s2.id)
        assertEquals("V1", s2.name)
    }

    @Test
    fun `runScript executes all steps`() = runBlocking {
        val script = AutomationScript(
            id = "s1",
            name = "Multi Step",
            steps = listOf(
                ScriptStep(id = "1", command = "echo first"),
                ScriptStep(id = "2", command = "echo second"),
            ),
        )
        val result = engine.runScript(script, "/tmp")
        assertEquals(ScriptRunStatus.SUCCESS, result.status)
        assertEquals(2, result.stepResults.size)
        assertEquals(2, commandLog.size)
    }

    @Test
    fun `runScript stops on failure when continueOnError is false`() = runBlocking {
        val failingEngine = TerminalAutomationEngine { cmd, _, _ ->
            if (cmd.contains("fail")) 1 to "error" else 0 to "ok"
        }
        val script = AutomationScript(
            id = "s1",
            name = "Fail Script",
            steps = listOf(
                ScriptStep(id = "1", command = "echo ok"),
                ScriptStep(id = "2", command = "fail"),
                ScriptStep(id = "3", command = "echo never"),
            ),
        )
        val result = failingEngine.runScript(script, "/tmp")
        assertEquals(ScriptRunStatus.FAILED, result.status)
        assertEquals(2, result.stepResults.size)
    }

    @Test
    fun `runScript continues on failure when continueOnError is true`() = runBlocking {
        val failingEngine = TerminalAutomationEngine { cmd, _, _ ->
            if (cmd.contains("fail")) 1 to "error" else 0 to "ok"
        }
        val script = AutomationScript(
            id = "s1",
            name = "Continue Script",
            steps = listOf(
                ScriptStep(id = "1", command = "fail", continueOnError = true),
                ScriptStep(id = "2", command = "echo ok"),
            ),
        )
        val result = failingEngine.runScript(script, "/tmp")
        assertEquals(ScriptRunStatus.SUCCESS, result.status)
        assertEquals(2, result.stepResults.size)
    }

    @Test
    fun `runScript resolves variables in commands`() = runBlocking {
        val script = AutomationScript(
            id = "s1",
            name = "Var Script",
            variables = mapOf("name" to "world"),
            steps = listOf(ScriptStep(id = "1", command = "echo hello \${name}")),
        )
        engine.runScript(script, "/tmp")
        assertEquals("echo hello world", commandLog.first().first)
    }

    @Test
    fun `runScript resolves variables in workingDir`() = runBlocking {
        val script = AutomationScript(
            id = "s1",
            name = "Dir Script",
            variables = mapOf("project" to "myapp"),
            steps = listOf(
                ScriptStep(id = "1", command = "ls", workingDir = "/workspace/\${project}"),
            ),
        )
        engine.runScript(script, "/tmp")
        assertEquals("/workspace/myapp", commandLog.first().second)
    }

    @Test
    fun `runScript captures output to variable`() = runBlocking {
        val script = AutomationScript(
            id = "s1",
            name = "Capture Script",
            steps = listOf(
                ScriptStep(id = "1", command = "git rev-parse HEAD", captureOutput = "commit_hash"),
            ),
        )
        val result = engine.runScript(script, "/tmp")
        assertEquals("ok", result.capturedOutputs["commit_hash"])
    }

    @Test
    fun `runScript skips step when OutputContains condition is not met`() = runBlocking {
        val script = AutomationScript(
            id = "s1",
            name = "Cond Script",
            steps = listOf(
                ScriptStep(id = "1", command = "echo hello", captureOutput = "out1"),
                ScriptStep(
                    id = "2",
                    command = "echo skipped",
                    condition = StepCondition.OutputContains("MAGIC_MARKER"),
                ),
            ),
        )
        val result = engine.runScript(script, "/tmp")
        assertEquals(2, result.stepResults.size)
        assertTrue(result.stepResults[1].skipped)
    }

    @Test
    fun `runScript skips step when OutputNotContains condition is met`() = runBlocking {
        val script = AutomationScript(
            id = "s1",
            name = "Cond Script",
            steps = listOf(
                ScriptStep(id = "1", command = "echo ok", captureOutput = "out1"),
                ScriptStep(
                    id = "2",
                    command = "echo should not run",
                    condition = StepCondition.OutputNotContains("ok"),
                ),
            ),
        )
        val result = engine.runScript(script, "/tmp")
        assertTrue(result.stepResults[1].skipped)
    }

    @Test
    fun `runScript handles exception in step`() = runBlocking {
        val throwingEngine = TerminalAutomationEngine { _, _, _ ->
            throw RuntimeException("command exploded")
        }
        val script = AutomationScript(
            id = "s1",
            name = "Throw Script",
            steps = listOf(ScriptStep(id = "1", command = "bad")),
        )
        val result = throwingEngine.runScript(script, "/tmp")
        assertEquals(ScriptRunStatus.FAILED, result.status)
        assertEquals("command exploded", result.stepResults.first().error)
    }

    @Test
    fun `runScript increments runCount on script`() = runBlocking {
        val script = AutomationScript(
            id = "s1",
            name = "Counted",
            steps = listOf(ScriptStep(id = "1", command = "echo hi")),
        )
        engine.saveScript(script)
        engine.runScript(script, "/tmp")
        engine.runScript(script, "/tmp")
        assertEquals(2, engine.getScripts().first().runCount)
        assertNotNull(engine.getScripts().first().lastRunAt)
    }

    @Test
    fun `runScript calls onStepComplete callback`() = runBlocking {
        val completedSteps = mutableListOf<Int>()
        val script = AutomationScript(
            id = "s1",
            name = "Callback",
            steps = listOf(
                ScriptStep(id = "1", command = "echo a"),
                ScriptStep(id = "2", command = "echo b"),
            ),
        )
        engine.runScript(script, "/tmp") { index, _ -> completedSteps.add(index) }
        assertEquals(listOf(0, 1), completedSteps)
    }
}
