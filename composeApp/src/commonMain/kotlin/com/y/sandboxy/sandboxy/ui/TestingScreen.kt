package com.y.sandboxy.sandboxy.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.y.sandboxy.sandboxy.model.AvailableModels
import com.y.sandboxy.sandboxy.model.LlmModel
import com.y.sandboxy.sandboxy.model.LlmParams
import com.y.sandboxy.sandboxy.model.MessageRole
import com.y.sandboxy.sandboxy.model.TestingSession
import com.y.sandboxy.sandboxy.repository.ChatRepository
import com.y.sandboxy.sandboxy.state.TestingState
import kotlinx.coroutines.CoroutineScope

@Composable
fun TestingScreen(
    state: TestingState,
    repository: ChatRepository?,
    scope: CoroutineScope,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        // Session columns area
        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
        ) {
            state.sessions.forEachIndexed { index, session ->
                if (index > 0) {
                    VerticalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant,
                        modifier = Modifier.fillMaxHeight(),
                    )
                }
                SessionColumn(
                    session = session,
                    canRemove = state.sessions.size > 2,
                    onRemove = { state.removeSession(session.id) },
                    onConfigChange = { params, model ->
                        state.updateSessionConfig(session.id, params, model)
                    },
                    onCancel = { state.cancelSession(session.id) },
                    onRetry = {
                        if (repository != null) {
                            state.retrySession(session.id, repository, scope)
                        }
                    },
                    onClear = { state.clearSession(session.id) },
                    modifier = Modifier
                        .width(400.dp)
                        .fillMaxHeight(),
                )
            }

            // Add session button column
            Box(
                modifier = Modifier
                    .width(56.dp)
                    .fillMaxHeight(),
                contentAlignment = Alignment.TopCenter,
            ) {
                IconButton(
                    onClick = { state.addSession() },
                    modifier = Modifier.padding(top = 12.dp),
                ) {
                    Text("+", style = MaterialTheme.typography.titleLarge)
                }
            }
        }

        // Shared input bar
        TestingInputBar(
            text = state.inputText,
            onTextChange = { state.inputText = it },
            onSend = {
                if (repository != null) {
                    state.sendToAllSessions(repository, scope)
                }
            },
            onStopAll = { state.cancelAllSessions() },
            isAnyStreaming = state.isAnyStreaming,
            enabled = enabled,
        )
    }
}

@Composable
private fun SessionColumn(
    session: TestingSession,
    canRemove: Boolean,
    onRemove: () -> Unit,
    onConfigChange: (LlmParams, LlmModel) -> Unit,
    onCancel: () -> Unit,
    onRetry: () -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showSettings by remember { mutableStateOf(false) }

    Column(modifier = modifier) {
        // Header
        SessionHeader(
            session = session,
            canRemove = canRemove,
            onRemove = onRemove,
            onSettingsToggle = { showSettings = !showSettings },
            onClear = onClear,
            showSettings = showSettings,
        )

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

        if (showSettings) {
            // Inline settings
            SessionSettingsPanel(
                session = session,
                onConfigChange = onConfigChange,
                onClose = { showSettings = false },
                modifier = Modifier.weight(1f),
            )
        } else {
            // Messages area
            Box(modifier = Modifier.weight(1f)) {
                val listState = rememberLazyListState()

                val isNearBottom by remember {
                    derivedStateOf {
                        val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()
                        lastVisible == null || lastVisible.index >= listState.layoutInfo.totalItemsCount - 2
                    }
                }

                LaunchedEffect(session.messages.size, session.messages.lastOrNull()?.content) {
                    if (isNearBottom && session.messages.isNotEmpty()) {
                        listState.animateScrollToItem(session.messages.lastIndex)
                    }
                }

                if (session.messages.isEmpty() && !session.isTyping) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            "Send a message to start",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        )
                    }
                } else {
                    val clipboardManager = LocalClipboardManager.current

                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        itemsIndexed(session.messages, key = { _, msg -> msg.id }) { _, message ->
                            MessageBubble(
                                message = message,
                                onDelete = {},
                                showDeleteAction = false,
                                roleLabel = if (message.role == MessageRole.User) "You" else session.model.name,
                                onCopy = { clipboardManager.setText(AnnotatedString(message.content)) },
                            )
                        }

                        if (session.isTyping) {
                            item(key = "typing_${session.id}") {
                                TypingIndicator(modifier = Modifier.padding(vertical = 4.dp))
                            }
                        }
                    }
                }

                // Error overlay
                if (session.errorMessage != null) {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.9f),
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(12.dp),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(
                                session.errorMessage ?: "",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.weight(1f),
                            )
                            IconButton(
                                onClick = onRetry,
                                modifier = Modifier.size(32.dp),
                            ) {
                                Text(
                                    "↻",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                )
                            }
                        }
                    }
                }

                // Per-session cancel
                if (session.isStreaming) {
                    FilledIconButton(
                        onClick = onCancel,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(8.dp)
                            .size(32.dp),
                        shape = CircleShape,
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                        ),
                    ) {
                        Text("◼", fontSize = 10.sp, color = MaterialTheme.colorScheme.onError)
                    }
                }
            }

            // Metrics bar
            MetricsBar(
                metrics = session.metrics,
                isStreaming = session.isStreaming,
            )
        }
    }
}

@Composable
private fun SessionHeader(
    session: TestingSession,
    canRemove: Boolean,
    onRemove: () -> Unit,
    onSettingsToggle: () -> Unit,
    onClear: () -> Unit,
    showSettings: Boolean,
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 1.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = session.label,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                IconButton(
                    onClick = onSettingsToggle,
                    modifier = Modifier.size(28.dp),
                ) {
                    Text(
                        if (showSettings) "✕" else "⚙",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                IconButton(
                    onClick = onClear,
                    modifier = Modifier.size(28.dp),
                ) {
                    Text("🗑", style = MaterialTheme.typography.bodySmall)
                }
                if (canRemove) {
                    IconButton(
                        onClick = onRemove,
                        modifier = Modifier.size(28.dp),
                    ) {
                        Text("✕", style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error)
                    }
                }
            }
            // Config summary
            Text(
                text = "${session.model.name} · ${session.params.responseStyle.displayName}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}


@Composable
private fun SessionSettingsPanel(
    session: TestingSession,
    onConfigChange: (LlmParams, LlmModel) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var params by remember(session.id) { mutableStateOf(session.params) }
    var model by remember(session.id) { mutableStateOf(session.model) }

    // Push changes up on every edit
    LaunchedEffect(params, model) {
        onConfigChange(params, model)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(12.dp),
    ) {
        // Model selection
        Text(
            "Model",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.height(8.dp))
        AvailableModels.all.forEach { m ->
            Surface(
                onClick = { model = m },
                color = if (m == model) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    Color.Transparent
                },
                shape = MaterialTheme.shapes.small,
            ) {
                Text(
                    m.name,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (m == model) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                )
            }
        }

        Spacer(Modifier.height(12.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Spacer(Modifier.height(12.dp))

        // Use the full settings content inline — reusing the same param structure
        SettingsPanelContent(
            params = params,
            onParamsChange = { params = it },
            onReset = { params = LlmParams() },
        )
    }
}

@Composable
private fun TestingInputBar(
    text: String,
    onTextChange: (String) -> Unit,
    onSend: () -> Unit,
    onStopAll: () -> Unit,
    isAnyStreaming: Boolean,
    enabled: Boolean,
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 3.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextField(
                value = text,
                onValueChange = onTextChange,
                modifier = Modifier.weight(1f),
                enabled = enabled && !isAnyStreaming,
                placeholder = {
                    Text(
                        text = when {
                            !enabled -> "API key not configured"
                            isAnyStreaming -> "Waiting for responses..."
                            else -> "Type a message to send to all sessions..."
                        },
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
                shape = MaterialTheme.shapes.extraLarge,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.5f),
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent,
                ),
                singleLine = true,
            )

            FilledIconButton(
                onClick = { if (isAnyStreaming) onStopAll() else onSend() },
                enabled = if (isAnyStreaming) true else (enabled && text.isNotBlank()),
                modifier = Modifier
                    .padding(start = 8.dp)
                    .size(44.dp),
                shape = CircleShape,
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = if (isAnyStreaming) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.primary
                    },
                    disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                ),
            ) {
                Text(
                    text = if (isAnyStreaming) "◼" else "↑",
                    fontSize = if (isAnyStreaming) 14.sp else 20.sp,
                    color = if (isAnyStreaming) {
                        MaterialTheme.colorScheme.onError
                    } else {
                        MaterialTheme.colorScheme.onPrimary
                    },
                )
            }
        }
    }
}
