package com.y.sandboxy.sandboxy.storage

import com.y.sandboxy.sandboxy.export.ExperimentExporter
import com.y.sandboxy.sandboxy.model.AvailableModels
import com.y.sandboxy.sandboxy.model.ExperimentConfig
import com.y.sandboxy.sandboxy.model.ExperimentResult
import com.y.sandboxy.sandboxy.model.LlmModel
import com.y.sandboxy.sandboxy.model.LlmParams
import com.y.sandboxy.sandboxy.model.Rating

data class ExperimentHistoryEntry(
    val id: String,
    val timestamp: Long,
    val promptPreview: String,
    val configCount: Int,
    val iterationCount: Int,
)

data class SavedExperiment(
    val id: String,
    val timestamp: Long,
    val prompt: String,
    val systemPrompt: String,
    val iterations: Int,
    val configs: List<ExperimentConfig>,
    val results: List<ExperimentResult>,
)

object ExperimentHistoryRepository {

    fun save(
        prompt: String,
        systemPrompt: String,
        iterations: Int,
        configs: List<ExperimentConfig>,
        results: List<ExperimentResult>,
    ): String {
        val id = kotlin.time.Clock.System.now().toEpochMilliseconds().toString()
        val json = ExperimentExporter.toJson(prompt, systemPrompt, iterations, configs, results)
        // Wrap with metadata header for quick listing
        val wrapped = buildString {
            appendLine("---")
            appendLine("id=$id")
            appendLine("timestamp=$id")
            appendLine("promptPreview=${prompt.take(100).replace("\n", " ")}")
            appendLine("configCount=${configs.size}")
            appendLine("iterationCount=$iterations")
            appendLine("---")
            append(json)
        }
        ExperimentStorage.save(id, wrapped)
        return id
    }

    fun list(): List<ExperimentHistoryEntry> {
        return ExperimentStorage.listIds().mapNotNull { id ->
            val raw = ExperimentStorage.load(id) ?: return@mapNotNull null
            parseHeader(id, raw)
        }
    }

    fun load(id: String): SavedExperiment? {
        val raw = ExperimentStorage.load(id) ?: return null
        return parseExperiment(id, raw)
    }

    fun delete(id: String) {
        ExperimentStorage.delete(id)
    }

    private fun parseHeader(id: String, raw: String): ExperimentHistoryEntry? {
        if (!raw.startsWith("---")) return null
        val headerEnd = raw.indexOf("---", 3)
        if (headerEnd == -1) return null
        val header = raw.substring(3, headerEnd).trim()
        val fields = header.lines().associate {
            val (k, v) = it.split("=", limit = 2)
            k.trim() to v.trim()
        }
        return ExperimentHistoryEntry(
            id = fields["id"] ?: id,
            timestamp = fields["timestamp"]?.toLongOrNull() ?: 0L,
            promptPreview = fields["promptPreview"] ?: "",
            configCount = fields["configCount"]?.toIntOrNull() ?: 0,
            iterationCount = fields["iterationCount"]?.toIntOrNull() ?: 0,
        )
    }

    private fun parseExperiment(id: String, raw: String): SavedExperiment? {
        // Skip header, parse the JSON body (simple key extraction)
        val jsonStart = raw.indexOf("{")
        if (jsonStart == -1) return null
        val json = raw.substring(jsonStart)

        // Simple JSON parsing for our known structure
        val prompt = extractJsonString(json, "prompt") ?: return null
        val systemPrompt = extractJsonString(json, "systemPrompt") ?: ""
        val iterations = extractJsonInt(json, "iterations") ?: 1
        val header = parseHeader(id, raw)

        return SavedExperiment(
            id = id,
            timestamp = header?.timestamp ?: 0L,
            prompt = prompt,
            systemPrompt = systemPrompt,
            iterations = iterations,
            configs = emptyList(), // Full parsing deferred — configs come from presets/manual
            results = emptyList(), // Full parsing deferred
        )
    }

    private fun extractJsonString(json: String, key: String): String? {
        val pattern = "\"$key\"\\s*:\\s*\""
        val regex = Regex(pattern)
        val match = regex.find(json) ?: return null
        val start = match.range.last + 1
        val sb = StringBuilder()
        var i = start
        while (i < json.length) {
            val c = json[i]
            if (c == '\\' && i + 1 < json.length) {
                when (json[i + 1]) {
                    '"' -> { sb.append('"'); i += 2 }
                    '\\' -> { sb.append('\\'); i += 2 }
                    'n' -> { sb.append('\n'); i += 2 }
                    'r' -> { sb.append('\r'); i += 2 }
                    't' -> { sb.append('\t'); i += 2 }
                    else -> { sb.append(c); i++ }
                }
            } else if (c == '"') {
                break
            } else {
                sb.append(c)
                i++
            }
        }
        return sb.toString()
    }

    private fun extractJsonInt(json: String, key: String): Int? {
        val pattern = "\"$key\"\\s*:\\s*(\\d+)"
        return Regex(pattern).find(json)?.groupValues?.get(1)?.toIntOrNull()
    }
}
