package com.y.sandboxy.sandboxy.model

data class ExperimentConfig(
    val id: String = generateId(),
    val name: String,
    val colorIndex: Int = 0,
    val model: LlmModel = AvailableModels.default,
    val params: LlmParams = LlmParams(),
)

enum class Rating { Positive, Negative }

data class ExperimentResult(
    val configId: String,
    val iterationIndex: Int,
    val responseText: String = "",
    val isStreaming: Boolean = false,
    val latencyMs: Long? = null,
    val inputTokens: Int? = null,
    val outputTokens: Int? = null,
    val modelId: String? = null,
    val rating: Rating? = null,
)

data class Experiment(
    val id: String = generateId(),
    val timestamp: Long = 0L,
    val prompt: String,
    val systemPrompt: String = "",
    val iterations: Int = 1,
    val configs: List<ExperimentConfig>,
    val results: List<ExperimentResult> = emptyList(),
)

sealed class ExperimentPreset(
    val name: String,
    val description: String,
) {
    abstract fun createConfigs(): List<ExperimentConfig>

    data object TemperatureSweep : ExperimentPreset(
        name = "Temperature Sweep",
        description = "5 configs with temperature: 0.0, 0.3, 0.7, 1.0, 1.5",
    ) {
        override fun createConfigs(): List<ExperimentConfig> {
            val temps = listOf(0.0f, 0.3f, 0.7f, 1.0f, 1.5f)
            return temps.mapIndexed { i, temp ->
                ExperimentConfig(
                    name = "Temp ${formatTemp(temp)}",
                    colorIndex = i,
                    params = LlmParams(temperature = temp),
                )
            }
        }
    }

    data object ModelComparison : ExperimentPreset(
        name = "Model Comparison",
        description = "Compare up to 3 different models with identical params",
    ) {
        override fun createConfigs(): List<ExperimentConfig> {
            return AvailableModels.all.take(3).mapIndexed { i, model ->
                ExperimentConfig(
                    name = model.name,
                    colorIndex = i,
                    model = model,
                )
            }
        }
    }

    data object CreativityVsPrecision : ExperimentPreset(
        name = "Creativity vs Precision",
        description = "temp=0/top_p=0.1 vs temp=1.5/top_p=0.95",
    ) {
        override fun createConfigs(): List<ExperimentConfig> = listOf(
            ExperimentConfig(
                name = "Precision",
                colorIndex = 0,
                params = LlmParams(temperature = 0f, topP = 0.1f),
            ),
            ExperimentConfig(
                name = "Creative",
                colorIndex = 1,
                params = LlmParams(temperature = 1.5f, topP = 0.95f),
            ),
        )
    }

    data object TopPSweep : ExperimentPreset(
        name = "Top-P Sweep",
        description = "4 configs with top_p: 0.1, 0.5, 0.9, 1.0",
    ) {
        override fun createConfigs(): List<ExperimentConfig> {
            val topPs = listOf(0.1f, 0.5f, 0.9f, 1.0f)
            return topPs.mapIndexed { i, tp ->
                ExperimentConfig(
                    name = "Top-P ${formatTemp(tp)}",
                    colorIndex = i,
                    params = LlmParams(topP = tp),
                )
            }
        }
    }

    companion object {
        val all: List<ExperimentPreset> = listOf(
            TemperatureSweep, ModelComparison, CreativityVsPrecision, TopPSweep,
        )
    }
}

private fun formatTemp(v: Float): String {
    val s = v.toString()
    return if ('.' in s) s else "$s.0"
}

private fun generateId(): String {
    val chars = "abcdefghijklmnopqrstuvwxyz0123456789"
    return (1..8).map { chars[kotlin.random.Random.nextInt(chars.length)] }.joinToString("")
}
