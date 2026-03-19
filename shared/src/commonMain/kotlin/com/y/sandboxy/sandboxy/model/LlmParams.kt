package com.y.sandboxy.sandboxy.model

data class LlmParams(
    val temperature: Float = DEFAULT_TEMPERATURE,
    val topK: Int = DEFAULT_TOP_K,
    val topP: Float = DEFAULT_TOP_P,
    val maxTokens: Int = DEFAULT_MAX_TOKENS,
    val contextWindowLimit: Int = DEFAULT_CONTEXT_WINDOW,
    val systemPrompt: String = DEFAULT_SYSTEM_PROMPT,
    val responseStyle: ResponseStyle = ResponseStyle.Default,
    val stopSequences: List<String> = emptyList(),
    val frequencyPenalty: Float = DEFAULT_FREQUENCY_PENALTY,
    val presencePenalty: Float = DEFAULT_PRESENCE_PENALTY,
    val seed: Int? = null,
) {
    companion object {
        const val DEFAULT_TEMPERATURE = 0.7f
        const val DEFAULT_TOP_K = 40
        const val DEFAULT_TOP_P = 0.9f
        const val DEFAULT_MAX_TOKENS = 4096
        const val DEFAULT_CONTEXT_WINDOW = 20
        const val DEFAULT_SYSTEM_PROMPT = "You are a helpful AI assistant."
        const val DEFAULT_FREQUENCY_PENALTY = 0f
        const val DEFAULT_PRESENCE_PENALTY = 0f
    }
}
