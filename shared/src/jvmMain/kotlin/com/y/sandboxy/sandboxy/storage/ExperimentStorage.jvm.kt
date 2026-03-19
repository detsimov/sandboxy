package com.y.sandboxy.sandboxy.storage

import java.io.File

actual object ExperimentStorage {
    private val dir: File by lazy {
        val home = System.getProperty("user.home")
        File(home, ".sandboxy/experiments").also { it.mkdirs() }
    }

    actual fun save(id: String, json: String) {
        File(dir, "$id.json").writeText(json)
    }

    actual fun load(id: String): String? {
        val file = File(dir, "$id.json")
        return if (file.exists()) file.readText() else null
    }

    actual fun delete(id: String) {
        File(dir, "$id.json").delete()
    }

    actual fun listIds(): List<String> {
        return dir.listFiles()
            ?.filter { it.extension == "json" }
            ?.map { it.nameWithoutExtension }
            ?.sortedDescending()
            ?: emptyList()
    }
}
