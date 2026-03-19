package com.y.sandboxy.sandboxy.storage

import kotlinx.browser.window

actual object ExperimentStorage {
    private const val PREFIX = "sandboxy_exp_"
    private const val INDEX_KEY = "sandboxy_exp_index"

    actual fun save(id: String, json: String) {
        window.localStorage.setItem("$PREFIX$id", json)
        val ids = loadIndex().toMutableSet()
        ids.add(id)
        window.localStorage.setItem(INDEX_KEY, ids.joinToString(","))
    }

    actual fun load(id: String): String? {
        return window.localStorage.getItem("$PREFIX$id")
    }

    actual fun delete(id: String) {
        window.localStorage.removeItem("$PREFIX$id")
        val ids = loadIndex().toMutableSet()
        ids.remove(id)
        window.localStorage.setItem(INDEX_KEY, ids.joinToString(","))
    }

    actual fun listIds(): List<String> {
        return loadIndex().sortedDescending()
    }

    private fun loadIndex(): List<String> {
        val raw = window.localStorage.getItem(INDEX_KEY) ?: return emptyList()
        return raw.split(",").filter { it.isNotBlank() }
    }
}
