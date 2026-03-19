package com.y.sandboxy.sandboxy.storage

expect object ExperimentStorage {
    fun save(id: String, json: String)
    fun load(id: String): String?
    fun delete(id: String)
    fun listIds(): List<String>
}
