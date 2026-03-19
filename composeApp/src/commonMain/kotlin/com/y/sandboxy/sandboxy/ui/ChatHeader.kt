package com.y.sandboxy.sandboxy.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.y.sandboxy.sandboxy.AppMode
import com.y.sandboxy.sandboxy.model.AvailableModels
import com.y.sandboxy.sandboxy.model.LlmModel

@Composable
fun ChatHeader(
    selectedModel: LlmModel,
    onModelSelected: (LlmModel) -> Unit,
    onSettingsClick: () -> Unit,
    onClearClick: () -> Unit,
    onModeChange: (AppMode) -> Unit,
    currentMode: AppMode,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 2.dp,
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                // Mode toggle
                ModeToggle(
                    currentMode = currentMode,
                    onModeChange = onModeChange,
                )

                if (currentMode == AppMode.Chat) {
                    ModelDropdown(
                        selectedModel = selectedModel,
                        onModelSelected = onModelSelected,
                    )
                }
            }
            if (currentMode == AppMode.Chat) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    IconButton(onClick = onSettingsClick) {
                        Text("⚙", style = MaterialTheme.typography.titleMedium)
                    }
                    IconButton(onClick = onClearClick) {
                        Text("🗑", style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
        }
    }
}

@Composable
private fun ModeToggle(
    currentMode: AppMode,
    onModeChange: (AppMode) -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        shape = MaterialTheme.shapes.small,
    ) {
        Row(modifier = Modifier.padding(2.dp)) {
            AppMode.entries.forEach { mode ->
                ModeTab(
                    label = mode.name,
                    selected = currentMode == mode,
                    onClick = { if (currentMode != mode) onModeChange(mode) },
                )
            }
        }
    }
}

@Composable
private fun ModeTab(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHighest,
        shape = MaterialTheme.shapes.extraSmall,
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ModelDropdown(
    selectedModel: LlmModel,
    onModelSelected: (LlmModel) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        TextButton(onClick = { expanded = true }) {
            Text(
                text = selectedModel.name,
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = " ▾",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.titleMedium,
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            AvailableModels.all.forEach { model ->
                DropdownMenuItem(
                    text = {
                        Text(
                            model.name,
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    },
                    onClick = {
                        onModelSelected(model)
                        expanded = false
                    },
                )
            }
        }
    }
}
