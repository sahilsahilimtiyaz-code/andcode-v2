package com.yugahashimoto.andcode.core.index

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SymbolExtractorTest {

    @Test
    fun `extracts class declarations`() {
        val symbols = SymbolExtractor.extract("Foo.kt", "class FooBar")
        assertTrue(symbols.any { it.name == "FooBar" && it.kind == SymbolKind.CLASS })
    }

    @Test
    fun `extracts data classes`() {
        val symbols = SymbolExtractor.extract("Foo.kt", "data class User(val name: String)")
        assertTrue(symbols.any { it.name == "User" && it.kind == SymbolKind.CLASS })
    }

    @Test
    fun `extracts object declarations`() {
        val symbols = SymbolExtractor.extract("Foo.kt", "object MySingleton")
        assertTrue(symbols.any { it.name == "MySingleton" && it.kind == SymbolKind.CLASS })
    }

    @Test
    fun `extracts interfaces`() {
        val symbols = SymbolExtractor.extract("Foo.kt", "interface Repository")
        assertTrue(symbols.any { it.name == "Repository" && it.kind == SymbolKind.INTERFACE })
    }

    @Test
    fun `extracts functions`() {
        val symbols = SymbolExtractor.extract("Foo.kt", "fun calculateTotal(items: List<Int>): Int")
        assertTrue(symbols.any { it.name == "calculateTotal" && it.kind == SymbolKind.FUNCTION })
    }

    @Test
    fun `extracts properties`() {
        val symbols = SymbolExtractor.extract("Foo.kt", "val itemCount: Int = 0")
        assertTrue(symbols.any { it.name == "itemCount" && it.kind == SymbolKind.PROPERTY })
    }

    @Test
    fun `extracts imports`() {
        val symbols = SymbolExtractor.extract("Foo.kt", "import com.example.MyClass")
        assertTrue(symbols.any { it.name == "com.example.MyClass" && it.kind == SymbolKind.IMPORT })
    }

    @Test
    fun `extracts package declarations`() {
        val symbols = SymbolExtractor.extract("Foo.kt", "package com.example.app")
        assertTrue(symbols.any { it.name == "com.example.app" && it.kind == SymbolKind.PACKAGE })
    }

    @Test
    fun `skips single character names`() {
        val symbols = SymbolExtractor.extract("Foo.kt", "val x = 1")
        assertFalse(symbols.any { it.name == "x" })
    }

    @Test
    fun `skips keyword names`() {
        val symbols = SymbolExtractor.extract("Foo.kt", "val class = 1")
        assertFalse(symbols.any { it.name == "class" })
    }

    @Test
    fun `extracts annotations`() {
        val symbols = SymbolExtractor.extract("Foo.kt", "@Serializable\ndata class Config(val x: Int)")
        assertTrue(symbols.any { it.name == "Serializable" && it.kind == SymbolKind.ANNOTATION })
    }

    @Test
    fun `extracts multiple symbols from file`() {
        val code = """
            package com.example
            
            import com.other.Lib
            
            class UserService {
                fun getUser(id: String): User = TODO()
                fun saveUser(user: User) = TODO()
            }
            
            data class User(val id: String, val name: String)
        """.trimIndent()
        val symbols = SymbolExtractor.extract("UserService.kt", code)
        assertTrue(symbols.size >= 4)
        assertTrue(symbols.any { it.kind == SymbolKind.PACKAGE })
        assertTrue(symbols.any { it.kind == SymbolKind.IMPORT })
        assertTrue(symbols.any { it.name == "UserService" && it.kind == SymbolKind.CLASS })
        assertTrue(symbols.any { it.kind == SymbolKind.FUNCTION })
    }

    @Test
    fun `extractReferences finds all words`() {
        val refs = SymbolExtractor.extractReferences("Foo.kt", "val x = foo(bar)")
        assertTrue(refs.containsKey("val"))
        assertTrue(refs.containsKey("foo"))
        assertTrue(refs.containsKey("bar"))
    }

    @Test
    fun `line numbers are correct`() {
        val code = """
            class A
            class B
            class C
        """.trimIndent()
        val symbols = SymbolExtractor.extract("Foo.kt", code)
        val classes = symbols.filter { it.kind == SymbolKind.CLASS }.sortedBy { it.line }
        assertEquals(3, classes.size)
        assertEquals(1, classes[0].line)
        assertEquals(2, classes[1].line)
        assertEquals(3, classes[2].line)
    }
}
