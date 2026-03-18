package com.y.sandboxy.sandboxy.model

sealed class StreamChunk {
    data class TextDelta(val text: String) : StreamChunk()
    data class Usage(
        val inputTokens: Int?,
        val outputTokens: Int?,
        val totalTokens: Int?,
    ) : StreamChunk()
}
