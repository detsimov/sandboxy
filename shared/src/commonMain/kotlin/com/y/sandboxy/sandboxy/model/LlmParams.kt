package com.y.sandboxy.sandboxy.model

data class LlmParams(
    val temperature: Float = DEFAULT_TEMPERATURE,
    val topK: Int = DEFAULT_TOP_K,
    val topP: Float = DEFAULT_TOP_P,
    val maxTokens: Int = DEFAULT_MAX_TOKENS,
    val contextWindowLimit: Int = DEFAULT_CONTEXT_WINDOW,
) {
    companion object {
        const val DEFAULT_TEMPERATURE = 0.7f
        const val DEFAULT_TOP_K = 40
        const val DEFAULT_TOP_P = 0.9f
        const val DEFAULT_MAX_TOKENS = 4096
        const val DEFAULT_CONTEXT_WINDOW = 20
    }
}
