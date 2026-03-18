package com.y.sandboxy.sandboxy.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.y.sandboxy.sandboxy.model.ChatMessage
import com.y.sandboxy.sandboxy.model.MessageRole
import kotlinx.coroutines.launch

@Composable
fun MessageList(
    messages: List<ChatMessage>,
    contextWindowLimit: Int,
    onDeleteMessage: (String) -> Unit,
    isTyping: Boolean,
    modifier: Modifier = Modifier,
    modelName: String = "Assistant",
) {
    val scope = rememberCoroutineScope()
    val clipboardManager = LocalClipboardManager.current
    val scrollState = rememberScrollState()

    if (messages.isEmpty() && !isTyping) {
        EmptyStateView(modifier = modifier)
        return
    }

    val contextDividerIndex = if (messages.size > contextWindowLimit) {
        messages.size - contextWindowLimit
    } else {
        -1
    }

    // Smart auto-scroll: only scroll if user is near the bottom
    val isNearBottom by remember {
        derivedStateOf {
            val maxScroll = scrollState.maxValue
            maxScroll == 0 || scrollState.value >= maxScroll - 200
        }
    }

    // Show FAB when scrolled up significantly
    val showScrollFab by remember {
        derivedStateOf {
            val maxScroll = scrollState.maxValue
            maxScroll > 0 && scrollState.value < maxScroll - 500
        }
    }

    LaunchedEffect(messages.size, messages.lastOrNull()?.content) {
        if (isNearBottom && messages.isNotEmpty()) {
            scrollState.animateScrollTo(scrollState.maxValue)
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        SelectionContainer(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                messages.forEachIndexed { index, message ->
                    if (index == contextDividerIndex) {
                        ContextDivider()
                    }
                    AnimatedVisibility(
                        visible = true,
                        enter = fadeIn(
                            animationSpec = tween(150),
                        ) + slideInVertically(
                            initialOffsetY = { 24 },
                            animationSpec = tween(150),
                        ),
                    ) {
                        MessageBubble(
                            message = message,
                            onDelete = { onDeleteMessage(message.id) },
                            roleLabel = if (message.role == MessageRole.User) "You" else modelName,
                            onCopy = { clipboardManager.setText(AnnotatedString(message.content)) },
                        )
                    }
                }
            }
        }

        ScrollToBottomFab(
            visible = showScrollFab,
            onClick = {
                scope.launch {
                    scrollState.animateScrollTo(scrollState.maxValue)
                }
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
        )
    }
}

@Composable
private fun ContextDivider() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        HorizontalDivider(
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.outlineVariant,
        )
        Text(
            text = "messages above are not in context",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 8.dp),
        )
        HorizontalDivider(
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.outlineVariant,
        )
    }
}
