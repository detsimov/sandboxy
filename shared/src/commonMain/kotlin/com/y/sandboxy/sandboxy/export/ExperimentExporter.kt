package com.y.sandboxy.sandboxy.export

import com.y.sandboxy.sandboxy.model.ExperimentConfig
import com.y.sandboxy.sandboxy.model.ExperimentResult

object ExperimentExporter {

    fun toJson(
        prompt: String,
        systemPrompt: String,
        iterations: Int,
        configs: List<ExperimentConfig>,
        results: List<ExperimentResult>,
    ): String = buildString {
        appendLine("{")
        appendLine("""  "prompt": ${escapeJson(prompt)},""")
        appendLine("""  "systemPrompt": ${escapeJson(systemPrompt)},""")
        appendLine("""  "iterations": $iterations,""")
        appendLine("""  "configs": [""")
        configs.forEachIndexed { i, c ->
            appendLine("    {")
            appendLine("""      "name": ${escapeJson(c.name)},""")
            appendLine("""      "model": ${escapeJson(c.model.id)},""")
            appendLine("""      "temperature": ${c.params.temperature},""")
            appendLine("""      "topP": ${c.params.topP},""")
            appendLine("""      "topK": ${c.params.topK},""")
            appendLine("""      "maxTokens": ${c.params.maxTokens},""")
            appendLine("""      "frequencyPenalty": ${c.params.frequencyPenalty},""")
            appendLine("""      "presencePenalty": ${c.params.presencePenalty}""")
            append("    }")
            if (i < configs.lastIndex) appendLine(",") else appendLine()
        }
        appendLine("  ],")
        appendLine("""  "results": [""")
        val configMap = configs.associateBy { it.id }
        results.forEachIndexed { i, r ->
            val configName = configMap[r.configId]?.name ?: r.configId
            appendLine("    {")
            appendLine("""      "configName": ${escapeJson(configName)},""")
            appendLine("""      "iteration": ${r.iterationIndex + 1},""")
            appendLine("""      "responseText": ${escapeJson(r.responseText)},""")
            appendLine("""      "latencyMs": ${r.latencyMs},""")
            appendLine("""      "inputTokens": ${r.inputTokens},""")
            appendLine("""      "outputTokens": ${r.outputTokens},""")
            appendLine("""      "modelId": ${r.modelId?.let { escapeJson(it) } ?: "null"},""")
            appendLine("""      "rating": ${r.rating?.let { escapeJson(it.name) } ?: "null"}""")
            append("    }")
            if (i < results.lastIndex) appendLine(",") else appendLine()
        }
        appendLine("  ]")
        appendLine("}")
    }

    fun toCsv(
        configs: List<ExperimentConfig>,
        results: List<ExperimentResult>,
    ): String = buildString {
        appendLine("config_name,model,temperature,top_p,run_number,response_text,latency_ms,tokens_in,tokens_out")
        val configMap = configs.associateBy { it.id }
        results.forEach { r ->
            val config = configMap[r.configId]
            val name = csvEscape(config?.name ?: r.configId)
            val model = csvEscape(config?.model?.id ?: "")
            val temp = config?.params?.temperature ?: 0f
            val topP = config?.params?.topP ?: 0f
            val run = r.iterationIndex + 1
            val text = csvEscape(r.responseText)
            val latency = r.latencyMs ?: ""
            val tokensIn = r.inputTokens ?: ""
            val tokensOut = r.outputTokens ?: ""
            appendLine("$name,$model,$temp,$topP,$run,$text,$latency,$tokensIn,$tokensOut")
        }
    }

    fun toMarkdown(
        prompt: String,
        systemPrompt: String,
        iterations: Int,
        configs: List<ExperimentConfig>,
        results: List<ExperimentResult>,
    ): String = buildString {
        appendLine("# Experiment Report")
        appendLine()
        appendLine("**Prompt:** $prompt")
        if (systemPrompt.isNotBlank()) appendLine("**System Prompt:** $systemPrompt")
        appendLine("**Iterations:** $iterations")
        appendLine("**Configurations:** ${configs.size}")
        appendLine()

        val configMap = configs.associateBy { it.id }
        configs.forEach { config ->
            appendLine("## ${config.name}")
            appendLine()
            appendLine("- Model: ${config.model.name} (${config.model.id})")
            appendLine("- Temperature: ${config.params.temperature}")
            appendLine("- Top-P: ${config.params.topP}")
            appendLine("- Max Tokens: ${config.params.maxTokens}")
            appendLine()

            val configResults = results.filter { it.configId == config.id }
            configResults.forEach { r ->
                if (iterations > 1) appendLine("### Run ${r.iterationIndex + 1}")
                appendLine()
                appendLine(r.responseText)
                appendLine()
                val meta = listOfNotNull(
                    r.latencyMs?.let { "${it}ms" },
                    r.inputTokens?.let { "in: $it tokens" },
                    r.outputTokens?.let { "out: $it tokens" },
                    r.rating?.let { "Rating: ${it.name}" },
                )
                if (meta.isNotEmpty()) appendLine("*${meta.joinToString(" | ")}*")
                appendLine()
            }
        }
    }

    private fun escapeJson(s: String): String {
        val escaped = s
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
        return "\"$escaped\""
    }

    private fun csvEscape(s: String): String {
        val needsQuotes = s.contains(',') || s.contains('"') || s.contains('\n')
        return if (needsQuotes) "\"${s.replace("\"", "\"\"")}\"" else s
    }
}
