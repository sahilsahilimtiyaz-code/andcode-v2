package com.yugahashimoto.andcode.core.workspace

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class WorkspaceEngineTest {

    private lateinit var engine: WorkspaceEngine

    @Before
    fun setUp() {
        engine = WorkspaceEngine { cmd, dir, timeout ->
            0 to "ok"
        }
    }

    private fun testWorkspace(
        id: String = "ws1",
        name: String = "My Workspace",
        rootPath: String = "/tmp/workspace",
        tags: List<String> = emptyList(),
    ) = Workspace(id = id, name = name, rootPath = rootPath, tags = tags)

    @Test
    fun `getWorkspaces returns empty initially`() {
        assertTrue(engine.getWorkspaces().isEmpty())
    }

    @Test
    fun `createWorkspace stores and retrieves`() {
        engine.createWorkspace(testWorkspace())
        assertEquals(1, engine.getWorkspaces().size)
        assertEquals("My Workspace", engine.getWorkspaces().first().name)
    }

    @Test
    fun `createWorkspace sets isActive false`() {
        val ws = engine.createWorkspace(testWorkspace())
        assertFalse(ws.isActive)
    }

    @Test
    fun `deleteWorkspace removes workspace`() {
        engine.createWorkspace(testWorkspace(id = "ws1"))
        engine.createWorkspace(testWorkspace(id = "ws2"))
        assertTrue(engine.deleteWorkspace("ws1"))
        assertEquals(1, engine.getWorkspaces().size)
    }

    @Test
    fun `deleteWorkspace returns false for unknown id`() {
        assertFalse(engine.deleteWorkspace("nonexistent"))
    }

    @Test
    fun `openWorkspace sets it active`() {
        engine.createWorkspace(testWorkspace(id = "ws1"))
        engine.createWorkspace(testWorkspace(id = "ws2"))
        val opened = engine.openWorkspace("ws1")
        assertNotNull(opened)
        assertTrue(opened!!.isActive)
        val other = engine.getWorkspaceById("ws2")
        assertFalse(other!!.isActive)
    }

    @Test
    fun `openWorkspace updates lastOpenedAt`() {
        engine.createWorkspace(testWorkspace())
        val opened = engine.openWorkspace("ws1")
        assertNotNull(opened!!.lastOpenedAt)
    }

    @Test
    fun `openWorkspace adds to recent`() {
        engine.createWorkspace(testWorkspace(id = "ws1"))
        engine.createWorkspace(testWorkspace(id = "ws2"))
        engine.openWorkspace("ws1")
        engine.openWorkspace("ws2")
        val recent = engine.getRecentWorkspaces()
        assertEquals(2, recent.size)
        assertEquals("ws2", recent.first().id)
    }

    @Test
    fun `openWorkspace caps recent at MAX_RECENT`() {
        repeat(WorkspaceEngine.MAX_RECENT + 5) { i ->
            engine.createWorkspace(testWorkspace(id = "ws$i"))
            engine.openWorkspace("ws$i")
        }
        assertEquals(WorkspaceEngine.MAX_RECENT, engine.getRecentWorkspaces().size)
    }

    @Test
    fun `setActiveWorkspace delegates to openWorkspace`() {
        engine.createWorkspace(testWorkspace(id = "ws1"))
        val result = engine.setActiveWorkspace("ws1")
        assertNotNull(result)
        assertTrue(result!!.isActive)
    }

    @Test
    fun `addPinnedFile adds file to workspace`() {
        engine.createWorkspace(testWorkspace())
        assertTrue(engine.addPinnedFile("ws1", "src/Main.kt"))
        val ws = engine.getWorkspaceById("ws1")!!
        assertEquals(1, ws.pinnedFiles.size)
        assertEquals("src/Main.kt", ws.pinnedFiles.first())
    }

    @Test
    fun `addPinnedFile returns false for duplicate`() {
        engine.createWorkspace(testWorkspace())
        engine.addPinnedFile("ws1", "src/Main.kt")
        assertFalse(engine.addPinnedFile("ws1", "src/Main.kt"))
    }

    @Test
    fun `removePinnedFile removes file`() {
        engine.createWorkspace(testWorkspace())
        engine.addPinnedFile("ws1", "src/Main.kt")
        assertTrue(engine.removePinnedFile("ws1", "src/Main.kt"))
        assertTrue(engine.getWorkspaceById("ws1")!!.pinnedFiles.isEmpty())
    }

    @Test
    fun `addRecentFile adds to workspace`() {
        engine.createWorkspace(testWorkspace())
        val file = RecentFile(path = "src/Main.kt", name = "Main.kt", lastModifiedAt = System.currentTimeMillis())
        engine.addRecentFile("ws1", file)
        assertEquals(1, engine.getWorkspaceById("ws1")!!.recentFiles.size)
    }

    @Test
    fun `addRecentFile increments openCount for existing file`() {
        engine.createWorkspace(testWorkspace())
        val file = RecentFile(path = "src/Main.kt", name = "Main.kt", lastModifiedAt = System.currentTimeMillis())
        engine.addRecentFile("ws1", file)
        engine.addRecentFile("ws1", file.copy(lastModifiedAt = System.currentTimeMillis() + 1000))
        val recent = engine.getWorkspaceById("ws1")!!.recentFiles
        assertEquals(1, recent.size)
        assertEquals(2, recent.first().openCount)
    }

    @Test
    fun `addRecentFile caps at MAX_RECENT_FILES`() {
        engine.createWorkspace(testWorkspace())
        repeat(WorkspaceEngine.MAX_RECENT_FILES + 5) { i ->
            engine.addRecentFile("ws1", RecentFile(path = "file$i.kt", name = "file$i.kt", lastModifiedAt = i.toLong()))
        }
        assertEquals(WorkspaceEngine.MAX_RECENT_FILES, engine.getWorkspaceById("ws1")!!.recentFiles.size)
    }

    @Test
    fun `createSharedContext creates context`() {
        engine.createWorkspace(testWorkspace(id = "ws1"))
        engine.createWorkspace(testWorkspace(id = "ws2"))
        val ctx = engine.createSharedContext(listOf("ws1", "ws2"), mapOf("key" to "value"))
        assertEquals(2, ctx.workspaceIds.size)
        assertEquals("value", ctx.sharedVariables["key"])
    }

    @Test
    fun `getSharedContexts returns all`() {
        engine.createSharedContext(listOf("ws1"))
        engine.createSharedContext(listOf("ws2"))
        assertEquals(2, engine.getSharedContexts().size)
    }

    @Test
    fun `searchWorkspaces finds by name`() {
        engine.createWorkspace(testWorkspace(id = "ws1", name = "Android Project"))
        engine.createWorkspace(testWorkspace(id = "ws2", name = "iOS Project"))
        val results = engine.searchWorkspaces("android")
        assertEquals(1, results.size)
        assertEquals("ws1", results.first().id)
    }

    @Test
    fun `searchWorkspaces finds by tag`() {
        engine.createWorkspace(testWorkspace(id = "ws1", tags = listOf("kotlin", "android")))
        engine.createWorkspace(testWorkspace(id = "ws2", tags = listOf("swift", "ios")))
        val results = engine.searchWorkspaces("kotlin")
        assertEquals(1, results.size)
    }

    @Test
    fun `filterByTag returns matching`() {
        engine.createWorkspace(testWorkspace(id = "ws1", tags = listOf("android")))
        engine.createWorkspace(testWorkspace(id = "ws2", tags = listOf("ios")))
        engine.createWorkspace(testWorkspace(id = "ws3", tags = listOf("android", "kotlin")))
        assertEquals(2, engine.filterByTag("android").size)
    }

    @Test
    fun `updateWorkspace applies transformation`() {
        engine.createWorkspace(testWorkspace())
        val updated = engine.updateWorkspace("ws1") { it.copy(name = "Renamed") }
        assertEquals("Renamed", updated!!.name)
    }

    @Test
    fun `updateWorkspace returns null for unknown id`() {
        assertNull(engine.updateWorkspace("nonexistent") { it })
    }

    @Test
    fun `deleteWorkspace transfers active status to first remaining`() {
        engine.createWorkspace(testWorkspace(id = "ws1"))
        engine.createWorkspace(testWorkspace(id = "ws2"))
        engine.openWorkspace("ws1")
        assertTrue(engine.getWorkspaceById("ws1")!!.isActive)
        engine.deleteWorkspace("ws1")
        assertTrue(engine.getWorkspaceById("ws2")!!.isActive)
    }

    @Test
    fun `listFiles returns file list`() = runBlocking {
        engine.createWorkspace(testWorkspace())
        val files = engine.listFiles("ws1")
        assertEquals(1, files.size)
        assertEquals("ok", files.first())
    }

    @Test
    fun `listFiles returns empty for unknown workspace`() = runBlocking {
        assertTrue(engine.listFiles("nonexistent").isEmpty())
    }

    @Test
    fun `detectGitStatus updates git fields`() = runBlocking {
        val gitEngine = WorkspaceEngine { cmd, dir, timeout ->
            when {
                cmd.contains("git branch") -> 0 to "main"
                cmd.contains("git status") -> 0 to " M src/Main.kt"
                cmd.contains("git remote") -> 0 to "https://github.com/test/repo.git"
                else -> 0 to ""
            }
        }
        gitEngine.createWorkspace(testWorkspace())
        val updated = gitEngine.detectGitStatus("ws1")
        assertNotNull(updated)
        assertEquals("main", updated!!.gitBranch)
        assertTrue(updated.gitDirty)
        assertEquals("https://github.com/test/repo.git", updated.gitRemoteUrl)
    }
}
