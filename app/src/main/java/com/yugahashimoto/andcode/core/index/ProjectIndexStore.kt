package com.yugahashimoto.andcode.core.index

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

class ProjectIndexStore(
    private val directory: File,
    private val json: Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    },
) {
    fun load(rootPath: String): ProjectIndex? {
        return runCatching {
            json.decodeFromString<ProjectIndex>(file(rootPath).readText())
        }.getOrNull()
    }

    fun save(index: ProjectIndex) {
        runCatching {
            directory.mkdirs()
            file(index.rootPath).writeText(json.encodeToString(index))
        }
    }

    fun clear(rootPath: String) {
        file(rootPath).delete()
    }

    private fun file(rootPath: String): File {
        val safeName = rootPath.replace(Regex("[^A-Za-z0-9._-]"), "_").take(200)
        return File(directory, "index-$safeName.json")
    }
}
