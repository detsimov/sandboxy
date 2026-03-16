package com.y.sandboxy.sandboxy.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.unit.dp
import com.y.sandboxy.sandboxy.model.ChatMessage
import kotlinx.coroutines.launch

@Composable
fun MessageList(
    messages: List<ChatMessage>,
    contextWindowLimit: Int,
    listState: LazyListState,
    onDeleteMessage: (String) -> Unit,
    isTyping: Boolean,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()

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
            val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()
            lastVisible == null || lastVisible.index >= listState.layoutInfo.totalItemsCount - 2
        }
    }

    // Show FAB when scrolled up significantly
    val showScrollFab by remember {
        derivedStateOf {
            val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()
            val totalItems = listState.layoutInfo.totalItemsCount
            lastVisible != null && totalItems > 3 && lastVisible.index < totalItems - 3
        }
    }

    LaunchedEffect(messages.size, messages.lastOrNull()?.content) {
        if (isNearBottom && messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.lastIndex)
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            itemsIndexed(messages, key = { _, msg -> msg.id }) { index, message ->
                if (index == contextDividerIndex) {
                    ContextDivider()
                }
                AnimatedVisibility(
                    visible = true,
                    enter = fadeIn(initialAlpha = 0f) + slideInVertically(initialOffsetY = { it / 4 }),
                ) {
                    MessageBubble(
                        message = message,
                        onDelete = { onDeleteMessage(message.id) },
                    )
                }
            }

            if (isTyping) {
                item(key = "typing_indicator") {
                    TypingIndicator(modifier = Modifier.padding(vertical = 4.dp))
                }
            }
        }

        ScrollToBottomFab(
            visible = showScrollFab,
            onClick = {
                scope.launch {
                    if (messages.isNotEmpty()) {
                        listState.animateScrollToItem(messages.lastIndex)
                    }
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
