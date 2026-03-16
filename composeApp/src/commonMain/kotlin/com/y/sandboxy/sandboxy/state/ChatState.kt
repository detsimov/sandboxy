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
import com.y.sandboxy.sandboxy.repository.ChatRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

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

        streamingJob = scope.launch {
            var receivedFirstToken = false
            repository.sendMessageStreaming(
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
                if (!receivedFirstToken) {
                    receivedFirstToken = true
                    isTyping = false
                }
                appendToStreamingMessage(assistantMessage.id, chunk)
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
