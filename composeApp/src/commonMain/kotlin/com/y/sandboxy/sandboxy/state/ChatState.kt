package com.y.sandboxy.sandboxy.state

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.y.sandboxy.sandboxy.model.AvailableModels
import com.y.sandboxy.sandboxy.model.ChatMessage
import com.y.sandboxy.sandboxy.model.LlmModel
import com.y.sandboxy.sandboxy.model.LlmParams
import com.y.sandboxy.sandboxy.model.MessageRole
import com.y.sandboxy.sandboxy.model.StreamChunk
import com.y.sandboxy.sandboxy.model.UsageMetrics
import com.y.sandboxy.sandboxy.repository.ChatRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

data class PendingDelete(
    val message: ChatMessage,
    val originalIndex: Int,
)

class ChatState {
    val messages = mutableStateListOf<ChatMessage>()
    var selectedModel by mutableStateOf(AvailableModels.default)
    var params by mutableStateOf(LlmParams())
    var isStreaming by mutableStateOf(false)
    var isTyping by mutableStateOf(false)
    var inputText by mutableStateOf("")
    var showSettings by mutableStateOf(false)
    var showClearDialog by mutableStateOf(false)
    var apiKeyAvailable by mutableStateOf(true)

    var errorMessage by mutableStateOf<String?>(null)
    var lastUserMessageText by mutableStateOf<String?>(null)
    var metrics by mutableStateOf(UsageMetrics())

    private var streamingJob: Job? = null
    private var currentStreamingMessageId: String? = null

    fun sendMessageStreaming(
        repository: ChatRepository,
        scope: CoroutineScope,
    ) {
        val text = inputText.trim()
        if (text.isEmpty() || isStreaming) return

        inputText = ""
        lastUserMessageText = text

        val userMessage = ChatMessage(role = MessageRole.User, content = text)
        addMessage(userMessage)

        val assistantMessage = ChatMessage(
            role = MessageRole.Assistant,
            content = "",
            isStreaming = true,
        )
        addMessage(assistantMessage)
        currentStreamingMessageId = assistantMessage.id
        isStreaming = true
        isTyping = true
        metrics = UsageMetrics()

        val startTime = currentTimeMillis()
        var firstTokenTime: Long? = null

        streamingJob = scope.launch {
            var receivedFirstToken = false
            repository.sendMessageStreamingWithUsage(
                messages = messages.filter { it.id != assistantMessage.id }.toList(),
                model = selectedModel,
                params = params,
            ).catch { e ->
                if (e !is CancellationException) {
                    deleteMessage(assistantMessage.id)
                    isStreaming = false
                    isTyping = false
                    errorMessage = e.message ?: "Unknown error occurred"
                }
            }.collect { chunk ->
                when (chunk) {
                    is StreamChunk.TextDelta -> {
                        if (!receivedFirstToken) {
                            receivedFirstToken = true
                            firstTokenTime = currentTimeMillis()
                            isTyping = false
                        }
                        appendToStreamingMessage(assistantMessage.id, chunk.text)
                    }
                    is StreamChunk.Usage -> {
                        val endTime = currentTimeMillis()
                        metrics = UsageMetrics(
                            inputTokens = chunk.inputTokens,
                            outputTokens = chunk.outputTokens,
                            totalTokens = chunk.totalTokens,
                            timeToFirstTokenMs = firstTokenTime?.let { it - startTime },
                            totalTimeMs = endTime - startTime,
                        )
                    }
                }
            }

            finalizeStreamingMessage(assistantMessage.id)
            isStreaming = false
            isTyping = false
            currentStreamingMessageId = null
        }
    }

    fun cancelStreaming() {
        streamingJob?.cancel()
        streamingJob = null
        currentStreamingMessageId?.let { id ->
            finalizeStreamingMessage(id)
        }
        isStreaming = false
        isTyping = false
        currentStreamingMessageId = null
    }

    fun retryLastMessage(repository: ChatRepository, scope: CoroutineScope) {
        val text = lastUserMessageText ?: return
        inputText = text
        errorMessage = null
        sendMessageStreaming(repository, scope)
    }

    fun addMessage(message: ChatMessage) {
        messages.add(message)
    }

    private fun appendToStreamingMessage(id: String, chunk: String) {
        val index = messages.indexOfFirst { it.id == id }
        if (index != -1) {
            messages[index] = messages[index].copy(content = messages[index].content + chunk)
        }
    }

    private fun finalizeStreamingMessage(id: String) {
        val index = messages.indexOfFirst { it.id == id }
        if (index != -1) {
            messages[index] = messages[index].copy(isStreaming = false)
        }
    }

    // Soft-delete: stores removed message for undo
    var pendingDelete by mutableStateOf<PendingDelete?>(null)
        private set

    fun softDeleteMessage(id: String) {
        // Commit any existing pending delete first
        commitPendingDelete()

        val index = messages.indexOfFirst { it.id == id }
        if (index == -1) return
        val message = messages[index]
        messages.removeAt(index)
        pendingDelete = PendingDelete(message, index)
    }

    fun undoPendingDelete() {
        val pending = pendingDelete ?: return
        val insertIndex = pending.originalIndex.coerceAtMost(messages.size)
        messages.add(insertIndex, pending.message)
        pendingDelete = null
    }

    fun commitPendingDelete() {
        pendingDelete = null
    }

    fun deleteMessage(id: String) {
        messages.removeAll { it.id == id }
    }

    fun clearMessages() {
        messages.clear()
    }

    fun resetParams() {
        params = LlmParams()
    }
}

private fun currentTimeMillis(): Long = kotlin.time.Clock.System.now().toEpochMilliseconds()
