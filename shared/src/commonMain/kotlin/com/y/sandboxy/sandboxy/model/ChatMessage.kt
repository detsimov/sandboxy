package com.y.sandboxy.sandboxy.model

import kotlin.random.Random
import kotlin.time.Clock
import kotlin.time.Instant

enum class MessageRole {
    User,
    Assistant,
}

data class ChatMessage(
    val id: String = generateId(),
    val role: MessageRole,
    val content: String,
    val isStreaming: Boolean = false,
    val timestamp: Instant = Clock.System.now(),
)

private fun generateId(): String {
    val chars = "abcdefghijklmnopqrstuvwxyz0123456789"
    return (1..12).map { chars[Random.nextInt(chars.length)] }.joinToString("")
}
