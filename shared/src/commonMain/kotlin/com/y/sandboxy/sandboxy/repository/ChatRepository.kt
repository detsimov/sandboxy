package com.y.sandboxy.sandboxy.repository

import ai.koog.prompt.dsl.prompt
import ai.koog.prompt.executor.clients.openrouter.OpenRouterParams
import ai.koog.prompt.llm.LLMCapability
import ai.koog.prompt.llm.LLMProvider
import ai.koog.prompt.llm.LLModel
import ai.koog.prompt.streaming.StreamFrame
import com.y.sandboxy.sandboxy.agent.LlmClientProvider
import com.y.sandboxy.sandboxy.model.ChatMessage
import com.y.sandboxy.sandboxy.model.LlmModel
import com.y.sandboxy.sandboxy.model.LlmParams
import com.y.sandboxy.sandboxy.model.MessageRole
import com.y.sandboxy.sandboxy.model.ResponseStyle
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow


class ChatRepository(private val clientProvider: LlmClientProvider) {

    fun sendMessageStreaming(
        messages: List<ChatMessage>,
        model: LlmModel,
        params: LlmParams,
    ): Flow<String> = flow {
        val contextMessages = trimToContextWindow(messages, params.contextWindowLimit)

        val effectiveSystemPrompt = buildEffectiveSystemPrompt(params)
        val stopSeqs = params.stopSequences.filter { it.isNotBlank() }.ifEmpty { null }

        val llmParams = OpenRouterParams(
            temperature = params.temperature.toDouble(),
            maxTokens = params.maxTokens,
            topP = params.topP.toDouble(),
            topK = params.topK,
            stop = stopSeqs,
        )

        val chatPrompt = prompt("chat", llmParams) {
            if (effectiveSystemPrompt.isNotBlank()) {
                system(effectiveSystemPrompt)
            }
            contextMessages.forEach { msg ->
                when (msg.role) {
                    MessageRole.User -> user(msg.content)
                    MessageRole.Assistant -> assistant(msg.content)
                }
            }
        }

        val llmModel = LLModel(
            provider = LLMProvider.OpenRouter,
            id = model.id,
            capabilities = listOf(
                LLMCapability.Temperature,
                LLMCapability.Completion,
            )
        )

        val streamFlow: Flow<StreamFrame> = clientProvider.client.executeStreaming(chatPrompt, llmModel)

        // Batch rapid deltas: accumulate text and emit per frame
        var buffer = StringBuilder()
        var lastEmitTime = 0L

        streamFlow.collect { frame ->
            when (frame) {
                is StreamFrame.TextDelta -> {
                    buffer.append(frame.text)
                    val now = currentTimeMillis()
                    if (now - lastEmitTime >= 16 || buffer.length > 50) {
                        emit(buffer.toString())
                        buffer = StringBuilder()
                        lastEmitTime = now
                    }
                }
                is StreamFrame.End -> {
                    if (buffer.isNotEmpty()) {
                        emit(buffer.toString())
                    }
                }
                else -> { /* ignore tool calls, reasoning, etc. */ }
            }
        }
    }

    private fun buildEffectiveSystemPrompt(params: LlmParams): String {
        val base = params.systemPrompt.trim()
        val suffix = params.responseStyle.suffix
        return if (suffix.isEmpty()) base
        else if (base.isEmpty()) suffix
        else "$base\n\n$suffix"
    }

    private fun trimToContextWindow(
        messages: List<ChatMessage>,
        limit: Int,
    ): List<ChatMessage> {
        return if (messages.size > limit) {
            messages.takeLast(limit)
        } else {
            messages
        }
    }
}

private fun currentTimeMillis(): Long = kotlin.time.Clock.System.now().toEpochMilliseconds()
