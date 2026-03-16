package com.y.sandboxy.sandboxy.agent

import ai.koog.prompt.executor.clients.openrouter.OpenRouterLLMClient

class LlmClientProvider(apiKey: String) {
    val client: OpenRouterLLMClient = OpenRouterLLMClient(apiKey)
}
