package com.y.sandboxy.sandboxy

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.y.sandboxy.sandboxy.agent.LlmClientProvider
import com.y.sandboxy.sandboxy.repository.ChatRepository
import com.y.sandboxy.sandboxy.state.ChatState
import com.y.sandboxy.sandboxy.theme.SandboxyTheme
import com.y.sandboxy.sandboxy.ui.ChatHeader
import com.y.sandboxy.sandboxy.ui.ChatInput
import com.y.sandboxy.sandboxy.ui.ClearChatDialog
import com.y.sandboxy.sandboxy.ui.MessageList
import com.y.sandboxy.sandboxy.ui.SettingsPanel
import kotlinx.coroutines.launch

@Composable
fun App() {
    SandboxyTheme {
        val state = remember { ChatState() }
        val scope = rememberCoroutineScope()
        val listState = rememberLazyListState()
        val snackbarHostState = remember { SnackbarHostState() }

        val apiKey = remember { getEnvVariable("OPENROUTER_API_KEY") }
        val repository = remember(apiKey) {
            apiKey?.let { ChatRepository(LlmClientProvider(it)) }
        }

        LaunchedEffect(apiKey) {
            if (apiKey.isNullOrBlank()) {
                state.apiKeyAvailable = false
                snackbarHostState.showSnackbar(
                    message = "OpenRouter API key not configured. Set OPENROUTER_API_KEY in your environment or .env file.",
                    duration = SnackbarDuration.Indefinite,
                )
            }
        }

        // Show error snackbar when errorMessage changes
        LaunchedEffect(state.errorMessage) {
            val error = state.errorMessage ?: return@LaunchedEffect
            val result = snackbarHostState.showSnackbar(
                message = error,
                actionLabel = "Retry",
                duration = SnackbarDuration.Long,
            )
            if (result == SnackbarResult.ActionPerformed && repository != null) {
                state.retryLastMessage(repository, scope)
            }
            state.errorMessage = null
        }

        Scaffold(
            snackbarHost = {
                SnackbarHost(snackbarHostState) { data ->
                    Snackbar(
                        snackbarData = data,
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                        actionColor = MaterialTheme.colorScheme.primary,
                        shape = MaterialTheme.shapes.medium,
                    )
                }
            },
            containerColor = MaterialTheme.colorScheme.background,
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.TopCenter,
            ) {
                Row(modifier = Modifier.fillMaxSize()) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        ChatHeader(
                            selectedModel = state.selectedModel,
                            onModelSelected = { state.selectedModel = it },
                            onSettingsClick = { state.showSettings = !state.showSettings },
                            onClearClick = { state.showClearDialog = true },
                            modifier = Modifier.widthIn(max = 768.dp),
                        )

                        MessageList(
                            messages = state.messages.toList(),
                            contextWindowLimit = state.params.contextWindowLimit,
                            listState = listState,
                            onDeleteMessage = { id -> state.deleteMessage(id) },
                            isTyping = state.isTyping,
                            modifier = Modifier
                                .weight(1f)
                                .widthIn(max = 768.dp),
                        )

                        ChatInput(
                            text = state.inputText,
                            onTextChange = { state.inputText = it },
                            onSend = {
                                if (repository != null) {
                                    state.sendMessageStreaming(repository, scope)
                                }
                            },
                            onStop = { state.cancelStreaming() },
                            isStreaming = state.isStreaming,
                            enabled = state.apiKeyAvailable,
                            modifier = Modifier.widthIn(max = 768.dp),
                        )
                    }

                    SettingsPanel(
                        visible = state.showSettings,
                        params = state.params,
                        onParamsChange = { state.params = it },
                        onReset = { state.resetParams() },
                    )
                }
            }

            if (state.showClearDialog) {
                ClearChatDialog(
                    onConfirm = {
                        state.clearMessages()
                        state.showClearDialog = false
                    },
                    onDismiss = { state.showClearDialog = false },
                )
            }
        }
    }
}
