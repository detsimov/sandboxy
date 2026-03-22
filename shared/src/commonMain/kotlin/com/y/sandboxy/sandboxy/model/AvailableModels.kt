package com.y.sandboxy.sandboxy.model

data class LlmModel(
    val id: String,
    val name: String,
)

object AvailableModels {
    val all = listOf(
        LlmModel("minimax/minimax-m2.5", "Minimax-m2.5"),
        LlmModel("deepseek/deepseek-v3.2", "Deepseek-v3.2"),
        LlmModel("openai/gpt-5.4", "GPT-5.4"),
        LlmModel("moonshotai/kimi-k2.5", "Kimi-k2.5"),
        LlmModel("stepfun/step-3.5-flash", "Step-3.5-flash"),
        LlmModel("nvidia/nemotron-3-super-120b-a12b", "Nvidia-nemotron-3-super")
    )

    val default = all.first()
}
