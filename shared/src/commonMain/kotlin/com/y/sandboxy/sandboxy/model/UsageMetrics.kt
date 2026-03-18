package com.y.sandboxy.sandboxy.model

data class UsageMetrics(
    val inputTokens: Int? = null,
    val outputTokens: Int? = null,
    val totalTokens: Int? = null,
    val timeToFirstTokenMs: Long? = null,
    val totalTimeMs: Long? = null,
) {
    val tokensPerSecond: Double?
        get() {
            val output = outputTokens ?: return null
            val time = totalTimeMs ?: return null
            if (time <= 0) return null
            return output * 1000.0 / time
        }
}
