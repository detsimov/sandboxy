package com.y.sandboxy.sandboxy.model

data class TestingSession(
    val id: String = generateSessionId(),
    val label: String,
    val params: LlmParams = LlmParams(),
    val model: LlmModel = AvailableModels.default,
    val messages: List<ChatMessage> = emptyList(),
    val isStreaming: Boolean = false,
    val isTyping: Boolean = false,
    val errorMessage: String? = null,
    val metrics: UsageMetrics = UsageMetrics(),
)

private fun generateSessionId(): String {
    val chars = "abcdefghijklmnopqrstuvwxyz0123456789"
    return (1..8).map { chars[kotlin.random.Random.nextInt(chars.length)] }.joinToString("")
}
