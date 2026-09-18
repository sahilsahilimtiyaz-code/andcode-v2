package com.yugahashimoto.andcode.core.index

object SymbolExtractor {

    private val patterns = mapOf(
        SymbolKind.CLASS to listOf(
            Regex("""(?:data\s+|sealed\s+|abstract\s+|open\s+|inner\s+|enum\s+|value\s+)?class\s+(\w+)"""),
            Regex("""(?:data\s+)?object\s+(\w+)"""),
        ),
        SymbolKind.INTERFACE to listOf(
            Regex("""interface\s+(\w+)"""),
        ),
        SymbolKind.FUNCTION to listOf(
            Regex("""fun\s+(?:<[^>]+>\s+)?(\w+)\s*\("""),
            Regex("""fun\s+(?:<[^>]+>\s+)?(\w+)\s*=\s"""),
        ),
        SymbolKind.PROPERTY to listOf(
            Regex("""(?:val|var)\s+(?:\w+\s*:\s*\S+\s*=\s*|)(\w+)"""),
        ),
        SymbolKind.ENUM_ENTRY to listOf(
            Regex("""^\s{4,}(\w+)\s*[({,]""", RegexOption.MULTILINE),
        ),
        SymbolKind.ANNOTATION to listOf(
            Regex("""@(\w+)"""),
        ),
        SymbolKind.IMPORT to listOf(
            Regex("""import\s+([\w.]+)"""),
        ),
        SymbolKind.PACKAGE to listOf(
            Regex("""package\s+([\w.]+)"""),
        ),
    )

    fun extract(filePath: String, content: String): List<SymbolEntry> {
        val entries = mutableListOf<SymbolEntry>()
        val lines = content.lines()

        for ((kind, regexes) in patterns) {
            for (regex in regexes) {
                for (match in regex.findAll(content)) {
                    val name = match.groupValues[1]
                    if (name.length < 2 || name in IGNORED_NAMES) continue

                    val lineIndex = content.substring(0, match.range.first).count { it == '\n' }
                    val lineStart = content.lastIndexOf('\n', match.range.first).let {
                        if (it < 0) 0 else it + 1
                    }
                    val column = match.range.first - lineStart
                    val snippet = lines.getOrNull(lineIndex)?.trim()?.take(120) ?: ""

                    entries.add(
                        SymbolEntry(
                            name = name,
                            kind = kind,
                            filePath = filePath,
                            line = lineIndex + 1,
                            column = column,
                            snippet = snippet,
                        )
                    )
                }
            }
        }

        return entries.distinctBy { "${it.filePath}:${it.line}:${it.column}" }
    }

    fun extractReferences(filePath: String, content: String): Map<String, List<Int>> {
        val refs = mutableMapOf<String, MutableList<Int>>()
        val lines = content.lines()

        for ((lineNum, line) in lines.withIndex()) {
            for (match in WORD_PATTERN.findAll(line)) {
                val word = match.value
                if (word.length < 2 || word in IGNORED_NAMES) continue
                refs.getOrPut(word) { mutableListOf() }.add(lineNum + 1)
            }
        }

        return refs
    }

    private val WORD_PATTERN = Regex("""\b([A-Za-z_]\w*)\b""")
    private val IGNORED_NAMES = setOf(
        "val", "var", "fun", "class", "object", "interface", "enum", "sealed", "abstract",
        "open", "data", "inner", "override", "private", "public", "internal", "protected",
        "suspend", "import", "package", "return", "if", "else", "when", "for", "while",
        "true", "false", "null", "this", "super", "it", "by", "lazy", "init", "companion",
        "get", "set", "field", "value", "this", "type", "where", "out", "in", "as", "is",
    )
}
