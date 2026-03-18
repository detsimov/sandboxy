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
import com.y.sandboxy.sandboxy.model.TestingSession
import com.y.sandboxy.sandboxy.model.UsageMetrics
import com.y.sandboxy.sandboxy.repository.ChatRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

class TestingState {
    val sessions = mutableStateListOf<TestingSession>()
    var inputText by mutableStateOf("")
    var isAnyStreaming by mutableStateOf(false)

    private val streamingJobs = mutableMapOf<String, Job>()
    private var sessionCounter = 0

    init {
        addSession()
        addSession()
    }

    fun addSession() {
        sessionCounter++
        sessions.add(
            TestingSession(
                label = "Session $sessionCounter",
                params = LlmParams(),
                model = AvailableModels.default,
            )
        )
    }

    fun removeSession(id: String) {
        if (sessions.size <= 2) return
        cancelSession(id)
        sessions.removeAll { it.id == id }
        updateAnyStreaming()
    }

    fun updateSessionConfig(id: String, params: LlmParams, model: LlmModel) {
        val index = sessions.indexOfFirst { it.id == id }
        if (index != -1) {
            sessions[index] = sessions[index].copy(params = params, model = model)
        }
    }

    fun sendToAllSessions(repository: ChatRepository, scope: CoroutineScope) {
        val text = inputText.trim()
        if (text.isEmpty() || isAnyStreaming) return
        inputText = ""

        sessions.forEachIndexed { index, session ->
            val userMessage = ChatMessage(role = MessageRole.User, content = text)
            val updatedMessages = session.messages + userMessage
            sessions[index] = session.copy(messages = updatedMessages)
            launchSessionStreaming(index, sessions[index], repository, scope)
        }
    }

    private fun launchSessionStreaming(
        index: Int,
        session: TestingSession,
        repository: ChatRepository,
        scope: CoroutineScope,
    ) {
        val assistantMessage = ChatMessage(
            role = MessageRole.Assistant,
            content = "",
            isStreaming = true,
        )
        sessions[index] = session.copy(
            messages = session.messages + assistantMessage,
            isStreaming = true,
            isTyping = true,
            errorMessage = null,
            metrics = UsageMetrics(),
        )
        updateAnyStreaming()

        val startTime = currentTimeMillis()
        var firstTokenTime: Long? = null

        val job = scope.launch {
            var receivedFirstToken = false
            repository.sendMessageStreamingWithUsage(
                messages = sessions[safeIndex(session.id)].messages
                    .filter { it.id != assistantMessage.id },
                model = session.model,
                params = session.params,
            ).catch { e ->
                if (e !is CancellationException) {
                    val idx = safeIndex(session.id)
                    if (idx != -1) {
                        // Remove the placeholder assistant message
                        val filtered = sessions[idx].messages.filter { it.id != assistantMessage.id }
                        sessions[idx] = sessions[idx].copy(
                            messages = filtered,
                            isStreaming = false,
                            isTyping = false,
                            errorMessage = e.message ?: "Unknown error",
                        )
                        updateAnyStreaming()
                    }
                }
            }.collect { chunk ->
                val idx = safeIndex(session.id)
                if (idx == -1) return@collect

                when (chunk) {
                    is StreamChunk.TextDelta -> {
                        if (!receivedFirstToken) {
                            receivedFirstToken = true
                            firstTokenTime = currentTimeMillis()
                            sessions[idx] = sessions[idx].copy(isTyping = false)
                        }
                        appendToMessage(idx, assistantMessage.id, chunk.text)
                    }
                    is StreamChunk.Usage -> {
                        val endTime = currentTimeMillis()
                        sessions[idx] = sessions[idx].copy(
                            metrics = UsageMetrics(
                                inputTokens = chunk.inputTokens,
                                outputTokens = chunk.outputTokens,
                                totalTokens = chunk.totalTokens,
                                timeToFirstTokenMs = firstTokenTime?.let { it - startTime },
                                totalTimeMs = endTime - startTime,
                            )
                        )
                    }
                }
            }

            val idx = safeIndex(session.id)
            if (idx != -1) {
                finalizeMessage(idx, assistantMessage.id)
                sessions[idx] = sessions[idx].copy(isStreaming = false, isTyping = false)
                updateAnyStreaming()
            }
        }
        streamingJobs[session.id] = job
    }

    fun cancelSession(id: String) {
        streamingJobs[id]?.cancel()
        streamingJobs.remove(id)
        val idx = safeIndex(id)
        if (idx != -1) {
            val s = sessions[idx]
            // Finalize any streaming message
            val finalizedMessages = s.messages.map {
                if (it.isStreaming) it.copy(isStreaming = false) else it
            }
            sessions[idx] = s.copy(
                messages = finalizedMessages,
                isStreaming = false,
                isTyping = false,
            )
        }
        updateAnyStreaming()
    }

    fun cancelAllSessions() {
        sessions.forEachIndexed { index, session ->
            streamingJobs[session.id]?.cancel()
            val finalizedMessages = session.messages.map {
                if (it.isStreaming) it.copy(isStreaming = false) else it
            }
            sessions[index] = session.copy(
                messages = finalizedMessages,
                isStreaming = false,
                isTyping = false,
            )
        }
        streamingJobs.clear()
        updateAnyStreaming()
    }

    fun retrySession(id: String, repository: ChatRepository, scope: CoroutineScope) {
        val idx = safeIndex(id)
        if (idx == -1) return
        val session = sessions[idx]

        // Find the last user message
        val lastUserMsg = session.messages.lastOrNull { it.role == MessageRole.User } ?: return

        sessions[idx] = session.copy(errorMessage = null)
        launchSessionStreaming(idx, sessions[idx], repository, scope)
    }

    fun clearSession(id: String) {
        val idx = safeIndex(id)
        if (idx != -1) {
            cancelSession(id)
            sessions[idx] = sessions[idx].copy(
                messages = emptyList(),
                metrics = UsageMetrics(),
                errorMessage = null,
            )
        }
    }

    private fun safeIndex(sessionId: String): Int =
        sessions.indexOfFirst { it.id == sessionId }

    private fun appendToMessage(sessionIndex: Int, messageId: String, text: String) {
        val session = sessions[sessionIndex]
        val updatedMessages = session.messages.map {
            if (it.id == messageId) it.copy(content = it.content + text) else it
        }
        sessions[sessionIndex] = session.copy(messages = updatedMessages)
    }

    private fun finalizeMessage(sessionIndex: Int, messageId: String) {
        val session = sessions[sessionIndex]
        val updatedMessages = session.messages.map {
            if (it.id == messageId) it.copy(isStreaming = false) else it
        }
        sessions[sessionIndex] = session.copy(messages = updatedMessages)
    }

    private fun updateAnyStreaming() {
        isAnyStreaming = sessions.any { it.isStreaming }
    }
}

private fun currentTimeMillis(): Long = kotlin.time.Clock.System.now().toEpochMilliseconds()
