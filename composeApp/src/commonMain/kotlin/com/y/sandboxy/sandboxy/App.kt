package com.y.sandboxy.sandboxy

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.y.sandboxy.sandboxy.agent.LlmClientProvider
import com.y.sandboxy.sandboxy.repository.ChatRepository
import com.y.sandboxy.sandboxy.state.ChatState
import com.y.sandboxy.sandboxy.state.ExperimentState
import com.y.sandboxy.sandboxy.state.TestingState
import com.y.sandboxy.sandboxy.theme.SandboxyTheme
import com.y.sandboxy.sandboxy.ui.ChatHeader
import com.y.sandboxy.sandboxy.ui.ChatInput
import com.y.sandboxy.sandboxy.ui.ClearChatDialog
import com.y.sandboxy.sandboxy.ui.ExperimentScreen
import com.y.sandboxy.sandboxy.ui.MessageList
import com.y.sandboxy.sandboxy.ui.MetricsBar
import com.y.sandboxy.sandboxy.ui.SettingsPanel
import com.y.sandboxy.sandboxy.ui.TestingScreen
import kotlinx.coroutines.launch

enum class AppMode { Chat, Testing, Experiments }

@Composable
fun App() {
    SandboxyTheme {
        val chatState = remember { ChatState() }
        val testingState = remember { TestingState() }
        val experimentState = remember { ExperimentState() }
        var appMode by remember { mutableStateOf(AppMode.Chat) }
        val scope = rememberCoroutineScope()
        val snackbarHostState = remember { SnackbarHostState() }

        val apiKey = remember { getEnvVariable("OPENROUTER_API_KEY") }
        val repository = remember(apiKey) {
            apiKey?.let { ChatRepository(LlmClientProvider(it)) }
        }

        LaunchedEffect(apiKey) {
            if (apiKey.isNullOrBlank()) {
                chatState.apiKeyAvailable = false
                snackbarHostState.showSnackbar(
                    message = "OpenRouter API key not configured. Set OPENROUTER_API_KEY in your environment or .env file.",
                    duration = SnackbarDuration.Indefinite,
                )
            }
        }

        // Show undo snackbar when a message is soft-deleted
        LaunchedEffect(chatState.pendingDelete) {
            val pending = chatState.pendingDelete ?: return@LaunchedEffect
            val result = snackbarHostState.showSnackbar(
                message = "Message deleted",
                actionLabel = "Undo",
                duration = SnackbarDuration.Short,
            )
            if (result == SnackbarResult.ActionPerformed) {
                chatState.undoPendingDelete()
            } else {
                chatState.commitPendingDelete()
            }
        }

        // Show error snackbar when errorMessage changes (chat mode)
        LaunchedEffect(chatState.errorMessage) {
            val error = chatState.errorMessage ?: return@LaunchedEffect
            val result = snackbarHostState.showSnackbar(
                message = error,
                actionLabel = "Retry",
                duration = SnackbarDuration.Long,
            )
            if (result == SnackbarResult.ActionPerformed && repository != null) {
                chatState.retryLastMessage(repository, scope)
            }
            chatState.errorMessage = null
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
                when (appMode) {
                    AppMode.Chat -> {
                        Row(modifier = Modifier.fillMaxSize()) {
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                ChatHeader(
                                    selectedModel = chatState.selectedModel,
                                    onModelSelected = { chatState.selectedModel = it },
                                    onSettingsClick = { chatState.showSettings = !chatState.showSettings },
                                    onClearClick = { chatState.showClearDialog = true },
                                    onModeChange = { appMode = it },
                                    currentMode = AppMode.Chat,
                                    modifier = Modifier.widthIn(max = 768.dp),
                                )

                                MessageList(
                                    messages = chatState.messages.toList(),
                                    contextWindowLimit = chatState.params.contextWindowLimit,
                                    onDeleteMessage = { id -> chatState.softDeleteMessage(id) },
                                    isTyping = chatState.isTyping,
                                    modelName = chatState.selectedModel.name,
                                    modifier = Modifier
                                        .weight(1f)
                                        .widthIn(max = 768.dp),
                                )

                                MetricsBar(
                                    metrics = chatState.metrics,
                                    isStreaming = chatState.isStreaming,
                                    modifier = Modifier.widthIn(max = 768.dp),
                                )

                                ChatInput(
                                    text = chatState.inputText,
                                    onTextChange = { chatState.inputText = it },
                                    onSend = {
                                        if (repository != null) {
                                            chatState.sendMessageStreaming(repository, scope)
                                        }
                                    },
                                    onStop = { chatState.cancelStreaming() },
                                    isStreaming = chatState.isStreaming,
                                    enabled = chatState.apiKeyAvailable,
                                    modifier = Modifier.widthIn(max = 768.dp),
                                )
                            }

                            SettingsPanel(
                                visible = chatState.showSettings,
                                params = chatState.params,
                                onParamsChange = { chatState.params = it },
                                onReset = { chatState.resetParams() },
                            )
                        }
                    }

                    AppMode.Testing -> {
                        Column(modifier = Modifier.fillMaxSize()) {
                            ChatHeader(
                                selectedModel = chatState.selectedModel,
                                onModelSelected = {},
                                onSettingsClick = {},
                                onClearClick = {},
                                onModeChange = { appMode = it },
                                currentMode = AppMode.Testing,
                            )

                            TestingScreen(
                                state = testingState,
                                repository = repository,
                                scope = scope,
                                enabled = chatState.apiKeyAvailable,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }

                    AppMode.Experiments -> {
                        Column(modifier = Modifier.fillMaxSize()) {
                            ChatHeader(
                                selectedModel = chatState.selectedModel,
                                onModelSelected = {},
                                onSettingsClick = {},
                                onClearClick = {},
                                onModeChange = { appMode = it },
                                currentMode = AppMode.Experiments,
                            )

                            ExperimentScreen(
                                state = experimentState,
                                repository = repository,
                                scope = scope,
                                enabled = chatState.apiKeyAvailable,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
            }

            if (chatState.showClearDialog) {
                ClearChatDialog(
                    onConfirm = {
                        chatState.clearMessages()
                        chatState.showClearDialog = false
                    },
                    onDismiss = { chatState.showClearDialog = false },
                )
            }
        }
    }
}
