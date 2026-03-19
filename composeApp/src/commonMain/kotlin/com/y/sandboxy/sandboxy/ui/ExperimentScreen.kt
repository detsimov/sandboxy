package com.y.sandboxy.sandboxy.ui

import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.y.sandboxy.sandboxy.model.AvailableModels
import com.y.sandboxy.sandboxy.model.ExperimentConfig
import com.y.sandboxy.sandboxy.model.ExperimentPreset
import com.y.sandboxy.sandboxy.model.ExperimentResult
import com.y.sandboxy.sandboxy.model.Rating
import com.y.sandboxy.sandboxy.diff.DiffType
import com.y.sandboxy.sandboxy.diff.WordDiff
import com.y.sandboxy.sandboxy.export.ExperimentExporter
import com.y.sandboxy.sandboxy.repository.ChatRepository
import com.y.sandboxy.sandboxy.state.ExperimentState
import kotlinx.coroutines.CoroutineScope

@Composable
fun ExperimentScreen(
    state: ExperimentState,
    repository: ChatRepository?,
    scope: CoroutineScope,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        // Sticky top: Prompt Editor + Configs
        PromptEditorZone(
            state = state,
            onRun = {
                if (repository != null) {
                    state.runExperiment(repository, scope)
                }
            },
            onStop = { state.cancelExperiment() },
            enabled = enabled,
        )

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

        // History panel (replaces configs + results when shown)
        if (state.showHistory) {
            HistoryPanel(
                entries = state.historyEntries,
                onReplay = { state.replayFromHistory(it) },
                onDelete = { state.deleteHistoryEntry(it) },
                modifier = Modifier.weight(1f),
            )
            return@Column
        }

        // Configs row
        ConfigurationsRow(state = state)

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

        // Export bar
        if (state.hasResults && !state.isRunning) {
            ExportBar(state = state)
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        }

        // Diff view when 2 selections made
        if (state.diffMode && state.diffSelections.size == 2) {
            val texts = state.getDiffTexts()
            if (texts != null) {
                DiffView(
                    textA = texts.first,
                    textB = texts.second,
                    onClose = { state.exitDiffMode() },
                    modifier = Modifier.weight(1f),
                )
                return@Column
            }
        }

        // Diff mode selection hint
        if (state.diffMode && state.diffSelections.size < 2) {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    "Select ${2 - state.diffSelections.size} response(s) to compare",
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        }

        // Results area
        if (state.results.isNotEmpty()) {
            ResultsArea(
                state = state,
                modifier = Modifier.weight(1f),
            )
        } else {
            Box(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "Configure and run an experiment to see results",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                )
            }
        }
    }
}

// ==================== Prompt Editor Zone ====================

@Composable
private fun PromptEditorZone(
    state: ExperimentState,
    onRun: () -> Unit,
    onStop: () -> Unit,
    enabled: Boolean,
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 1.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            // Prompt field
            OutlinedTextField(
                value = state.prompt,
                onValueChange = { state.prompt = it },
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.isRunning,
                placeholder = { Text("Enter your prompt...") },
                minLines = 3,
                maxLines = 6,
                textStyle = MaterialTheme.typography.bodyMedium,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                ),
            )

            // System prompt (collapsible)
            if (state.showSystemPrompt) {
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = state.systemPrompt,
                    onValueChange = { state.systemPrompt = it },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !state.isRunning,
                    placeholder = { Text("System prompt...") },
                    minLines = 2,
                    maxLines = 4,
                    textStyle = MaterialTheme.typography.bodySmall,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                    ),
                )
            }

            Spacer(Modifier.height(8.dp))

            // Controls row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    // System prompt toggle
                    TextButton(onClick = { state.showSystemPrompt = !state.showSystemPrompt }) {
                        Text(
                            if (state.showSystemPrompt) "- System Prompt" else "+ System Prompt",
                            style = MaterialTheme.typography.labelMedium,
                        )
                    }

                    // Iterations
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "Iterations:",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.width(4.dp))
                        OutlinedTextField(
                            value = state.iterations.toString(),
                            onValueChange = { text ->
                                val n = text.filter { it.isDigit() }.toIntOrNull() ?: 1
                                state.iterations = n.coerceIn(1, 50)
                            },
                            modifier = Modifier.width(64.dp),
                            enabled = !state.isRunning,
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodySmall,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                            ),
                        )
                    }

                    // Preset selector
                    PresetSelector(
                        onPresetSelected = { state.applyPreset(it) },
                        enabled = !state.isRunning,
                    )

                    // History toggle
                    TextButton(onClick = {
                        state.showHistory = !state.showHistory
                        if (state.showHistory) state.loadHistory()
                    }) {
                        Text(
                            if (state.showHistory) "Hide History" else "History",
                            style = MaterialTheme.typography.labelMedium,
                        )
                    }
                }

                // Cost estimate + Run/Stop button
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (state.prompt.isNotBlank()) {
                        val estimatedCalls = state.configs.size * state.iterations
                        Text(
                            "~$estimatedCalls calls",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    if (state.isRunning) {
                        FilledIconButton(
                            onClick = onStop,
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.error,
                            ),
                        ) {
                            Text("Stop", color = MaterialTheme.colorScheme.onError, style = MaterialTheme.typography.labelMedium)
                        }
                    } else {
                        FilledIconButton(
                            onClick = onRun,
                            enabled = enabled && state.prompt.isNotBlank(),
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                            ),
                        ) {
                            Text("Run", color = MaterialTheme.colorScheme.onPrimary, style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PresetSelector(
    onPresetSelected: (ExperimentPreset) -> Unit,
    enabled: Boolean,
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        TextButton(
            onClick = { expanded = true },
            enabled = enabled,
        ) {
            Text("Presets", style = MaterialTheme.typography.labelMedium)
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            ExperimentPreset.all.forEach { preset ->
                DropdownMenuItem(
                    text = {
                        Column {
                            Text(preset.name, style = MaterialTheme.typography.bodyMedium)
                            Text(
                                preset.description,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    },
                    onClick = {
                        onPresetSelected(preset)
                        expanded = false
                    },
                )
            }
        }
    }
}

// ==================== Configurations Row ====================

@Composable
private fun ConfigurationsRow(state: ExperimentState) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(vertical = 8.dp, horizontal = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        state.configs.forEach { config ->
            ConfigCard(
                config = config,
                canDelete = state.configs.size > 1,
                canDuplicate = state.configs.size < ExperimentState.MAX_CONFIGS,
                onUpdate = { updated -> state.updateConfig(config.id) { updated } },
                onDelete = { state.removeConfig(config.id) },
                onDuplicate = { state.duplicateConfig(config.id) },
                enabled = !state.isRunning,
            )
        }

        // Add button
        if (state.configs.size < ExperimentState.MAX_CONFIGS) {
            Surface(
                onClick = { state.addConfig() },
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.width(48.dp).height(200.dp),
                enabled = !state.isRunning,
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Text("+", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

@Composable
private fun ConfigCard(
    config: ExperimentConfig,
    canDelete: Boolean,
    canDuplicate: Boolean,
    onUpdate: (ExperimentConfig) -> Unit,
    onDelete: () -> Unit,
    onDuplicate: () -> Unit,
    enabled: Boolean,
) {
    val accentColor = Color(ExperimentState.CONFIG_COLORS[config.colorIndex % ExperimentState.CONFIG_COLORS.size])

    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.width(240.dp),
    ) {
        Column {
            // Color bar + name
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .background(accentColor),
            )

            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                // Name + actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedTextField(
                        value = config.name,
                        onValueChange = { onUpdate(config.copy(name = it)) },
                        modifier = Modifier.weight(1f),
                        enabled = enabled,
                        singleLine = true,
                        textStyle = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = accentColor,
                            unfocusedBorderColor = Color.Transparent,
                        ),
                    )
                    if (canDuplicate) {
                        IconButton(onClick = onDuplicate, modifier = Modifier.size(24.dp), enabled = enabled) {
                            Text("⧉", fontSize = 12.sp)
                        }
                    }
                    if (canDelete) {
                        IconButton(onClick = onDelete, modifier = Modifier.size(24.dp), enabled = enabled) {
                            Text("✕", fontSize = 12.sp, color = MaterialTheme.colorScheme.error)
                        }
                    }
                }

                // Model selector
                var modelExpanded by remember { mutableStateOf(false) }
                Box {
                    Surface(
                        onClick = { if (enabled) modelExpanded = true },
                        color = MaterialTheme.colorScheme.surfaceContainerHighest,
                        shape = MaterialTheme.shapes.small,
                    ) {
                        Text(
                            config.model.name,
                            modifier = Modifier.fillMaxWidth().padding(8.dp),
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    DropdownMenu(expanded = modelExpanded, onDismissRequest = { modelExpanded = false }) {
                        AvailableModels.all.forEach { model ->
                            DropdownMenuItem(
                                text = { Text(model.name, style = MaterialTheme.typography.bodySmall) },
                                onClick = {
                                    onUpdate(config.copy(model = model))
                                    modelExpanded = false
                                },
                            )
                        }
                    }
                }

                // Temperature
                CompactSlider(
                    label = "Temp",
                    value = config.params.temperature,
                    range = 0f..2f,
                    format = { formatDecimal(it, 2) },
                    onValueChange = { onUpdate(config.copy(params = config.params.copy(temperature = it))) },
                    enabled = enabled,
                )

                // Top-P
                CompactSlider(
                    label = "Top-P",
                    value = config.params.topP,
                    range = 0f..1f,
                    format = { formatDecimal(it, 2) },
                    onValueChange = { onUpdate(config.copy(params = config.params.copy(topP = it))) },
                    enabled = enabled,
                )

                // Max Tokens
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Max Tokens", style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(1f))
                    OutlinedTextField(
                        value = config.params.maxTokens.toString(),
                        onValueChange = { text ->
                            val n = text.filter { it.isDigit() }.toIntOrNull() ?: 1024
                            onUpdate(config.copy(params = config.params.copy(maxTokens = n.coerceIn(1, 32768))))
                        },
                        modifier = Modifier.width(80.dp),
                        enabled = enabled,
                        singleLine = true,
                        textStyle = MaterialTheme.typography.labelSmall,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                        ),
                    )
                }
            }
        }
    }
}

@Composable
private fun CompactSlider(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    format: (Float) -> String,
    onValueChange: (Float) -> Unit,
    enabled: Boolean,
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(label, style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(1f))
            Text(
                format(value),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = range,
            enabled = enabled,
            modifier = Modifier.height(24.dp),
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary,
                inactiveTrackColor = MaterialTheme.colorScheme.outlineVariant,
            ),
        )
    }
}

// ==================== Results Area ====================

@Composable
private fun ResultsArea(
    state: ExperimentState,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
    ) {
        state.configs.forEachIndexed { index, config ->
            if (index > 0) {
                VerticalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant,
                    modifier = Modifier.fillMaxHeight(),
                )
            }
            ResultColumn(
                config = config,
                results = state.resultsForConfig(config.id),
                iterations = state.iterations,
                onRate = { iter, rating -> state.rateResult(config.id, iter, rating) },
                diffMode = state.diffMode,
                diffSelections = state.diffSelections,
                onDiffSelect = { iter -> state.toggleDiffSelection(config.id, iter) },
                modifier = Modifier
                    .then(
                        if (state.configs.size <= 3) {
                            Modifier.weight(1f)
                        } else {
                            Modifier.width(360.dp)
                        }
                    )
                    .fillMaxHeight(),
            )
        }
    }
}

@Composable
private fun ResultColumn(
    config: ExperimentConfig,
    results: List<ExperimentResult>,
    iterations: Int,
    onRate: (Int, Rating?) -> Unit,
    diffMode: Boolean = false,
    diffSelections: List<Pair<String, Int>> = emptyList(),
    onDiffSelect: (Int) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val accentColor = Color(ExperimentState.CONFIG_COLORS[config.colorIndex % ExperimentState.CONFIG_COLORS.size])
    var selectedIteration by remember(results.size) {
        mutableIntStateOf((results.lastOrNull { !it.isStreaming }?.iterationIndex ?: 0))
    }
    val clipboardManager = LocalClipboardManager.current

    Column(modifier = modifier) {
        // Column header
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 1.dp,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(accentColor, CircleShape),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        config.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Text(
                    "${config.model.name} · temp=${config.params.temperature}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                // Rating summary
                val positiveCount = results.count { it.rating == Rating.Positive }
                val negativeCount = results.count { it.rating == Rating.Negative }
                if (positiveCount > 0 || negativeCount > 0) {
                    Text(
                        "+$positiveCount / -$negativeCount",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }

                // Aggregate metrics when iterations > 1
                if (iterations > 1 && results.any { !it.isStreaming }) {
                    Spacer(Modifier.height(4.dp))
                    val completed = results.filter { !it.isStreaming && it.responseText.isNotEmpty() }
                    if (completed.isNotEmpty()) {
                        val avgLatency = completed.mapNotNull { it.latencyMs }.average()
                        val avgLen = completed.map { it.responseText.length }.average()
                        val uniqueCount = completed.map { it.responseText.trim() }.toSet().size
                        Text(
                            "Avg: ${avgLatency.toLong()}ms · ${avgLen.toInt()} chars · $uniqueCount unique/${completed.size}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

        // Iteration tabs
        if (iterations > 1) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                repeat(iterations) { iter ->
                    val isSelected = selectedIteration == iter
                    Surface(
                        onClick = { selectedIteration = iter },
                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surfaceContainerHighest,
                        shape = MaterialTheme.shapes.extraSmall,
                    ) {
                        Text(
                            "Run ${iter + 1}",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        }

        // Response content
        val currentResult = results.find { it.iterationIndex == selectedIteration }

        if (currentResult != null) {
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(12.dp),
            ) {
                item {
                    if (currentResult.isStreaming && currentResult.responseText.isEmpty()) {
                        // Loading state
                        TypingIndicator(modifier = Modifier.padding(vertical = 8.dp))
                    } else {
                        // Response text with Markdown
                        MarkdownText(
                            text = currentResult.responseText,
                            modifier = Modifier.fillMaxWidth(),
                        )

                        if (currentResult.isStreaming) {
                            TypingIndicator(modifier = Modifier.padding(vertical = 4.dp))
                        }
                    }
                }

                // Metadata
                if (!currentResult.isStreaming && currentResult.responseText.isNotEmpty()) {
                    item {
                        Spacer(Modifier.height(12.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        Spacer(Modifier.height(8.dp))

                        // Metadata row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            currentResult.latencyMs?.let {
                                MetadataChip("${it}ms")
                            }
                            currentResult.inputTokens?.let {
                                MetadataChip("in: $it")
                            }
                            currentResult.outputTokens?.let {
                                MetadataChip("out: $it")
                            }
                            currentResult.modelId?.let {
                                MetadataChip(it.substringAfterLast("/"))
                            }
                        }

                        Spacer(Modifier.height(8.dp))

                        // Actions: copy + diff select + rating
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            // Copy
                            TextButton(
                                onClick = {
                                    clipboardManager.setText(AnnotatedString(currentResult.responseText))
                                },
                            ) {
                                Text("Copy", style = MaterialTheme.typography.labelSmall)
                            }

                            // Diff select
                            if (diffMode) {
                                val isSelected = diffSelections.any {
                                    it.first == config.id && it.second == currentResult.iterationIndex
                                }
                                TextButton(
                                    onClick = { onDiffSelect(currentResult.iterationIndex) },
                                ) {
                                    Text(
                                        if (isSelected) "Selected" else "Select",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }

                            Spacer(Modifier.weight(1f))

                            // Rating buttons
                            val currentRating = currentResult.rating
                            IconButton(
                                onClick = {
                                    onRate(
                                        currentResult.iterationIndex,
                                        if (currentRating == Rating.Positive) null else Rating.Positive,
                                    )
                                },
                                modifier = Modifier.size(32.dp),
                            ) {
                                Text(
                                    "👍",
                                    fontSize = 14.sp,
                                    modifier = Modifier.then(
                                        if (currentRating == Rating.Positive) {
                                            Modifier.background(
                                                MaterialTheme.colorScheme.primaryContainer,
                                                CircleShape,
                                            )
                                        } else Modifier
                                    ),
                                )
                            }
                            IconButton(
                                onClick = {
                                    onRate(
                                        currentResult.iterationIndex,
                                        if (currentRating == Rating.Negative) null else Rating.Negative,
                                    )
                                },
                                modifier = Modifier.size(32.dp),
                            ) {
                                Text(
                                    "👎",
                                    fontSize = 14.sp,
                                    modifier = Modifier.then(
                                        if (currentRating == Rating.Negative) {
                                            Modifier.background(
                                                MaterialTheme.colorScheme.errorContainer,
                                                CircleShape,
                                            )
                                        } else Modifier
                                    ),
                                )
                            }
                        }
                    }
                }
            }
        } else {
            Box(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "Waiting for results...",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                )
            }
        }
    }
}

@Composable
private fun DiffView(
    textA: String,
    textB: String,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val segments = remember(textA, textB) { WordDiff.diff(textA, textB) }

    Column(modifier = modifier) {
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 1.dp,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "Diff Comparison",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = onClose) {
                    Text("Close", style = MaterialTheme.typography.labelMedium)
                }
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

        val onSurfaceColor = MaterialTheme.colorScheme.onSurface

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
        ) {
            item {
                val annotated = androidx.compose.ui.text.buildAnnotatedString {
                    segments.forEach { segment ->
                        val color = when (segment.type) {
                            DiffType.Added -> Color(0xFF4CAF50)
                            DiffType.Removed -> Color(0xFFE57373)
                            DiffType.Unchanged -> onSurfaceColor
                        }
                        val bg = when (segment.type) {
                            DiffType.Added -> Color(0xFF4CAF50).copy(alpha = 0.15f)
                            DiffType.Removed -> Color(0xFFE57373).copy(alpha = 0.15f)
                            DiffType.Unchanged -> Color.Transparent
                        }
                        pushStyle(
                            androidx.compose.ui.text.SpanStyle(
                                color = color,
                                background = bg,
                            )
                        )
                        append(segment.text)
                        pop()
                        append(" ")
                    }
                }
                Text(
                    text = annotated,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

@Composable
private fun HistoryPanel(
    entries: List<com.y.sandboxy.sandboxy.storage.ExperimentHistoryEntry>,
    onReplay: (String) -> Unit,
    onDelete: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (entries.isEmpty()) {
        Box(
            modifier = modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                "No experiments saved yet",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            )
        }
    } else {
        LazyColumn(
            modifier = modifier.fillMaxWidth(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(entries.size) { index ->
                val entry = entries[index]
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    shape = MaterialTheme.shapes.medium,
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                entry.promptPreview,
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                "${entry.configCount} configs · ${entry.iterationCount} iterations",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        TextButton(onClick = { onReplay(entry.id) }) {
                            Text("Replay", style = MaterialTheme.typography.labelSmall)
                        }
                        IconButton(
                            onClick = { onDelete(entry.id) },
                            modifier = Modifier.size(32.dp),
                        ) {
                            Text("✕", fontSize = 12.sp, color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ExportBar(state: ExperimentState) {
    val clipboardManager = LocalClipboardManager.current
    var exportExpanded by remember { mutableStateOf(false) }

    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 1.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.End,
        ) {
            // Compare button
            if (!state.diffMode) {
                OutlinedButton(onClick = { state.enterDiffMode() }) {
                    Text("Compare", style = MaterialTheme.typography.labelMedium)
                }
                Spacer(Modifier.width(8.dp))
            } else {
                OutlinedButton(onClick = { state.exitDiffMode() }) {
                    Text("Exit Compare", style = MaterialTheme.typography.labelMedium)
                }
                Spacer(Modifier.width(8.dp))
            }

            Box {
                OutlinedButton(onClick = { exportExpanded = true }) {
                    Text("Export", style = MaterialTheme.typography.labelMedium)
                }
                DropdownMenu(expanded = exportExpanded, onDismissRequest = { exportExpanded = false }) {
                    DropdownMenuItem(
                        text = { Text("Copy as JSON") },
                        onClick = {
                            val json = ExperimentExporter.toJson(
                                state.prompt, state.systemPrompt, state.iterations,
                                state.configs.toList(), state.results.toList(),
                            )
                            clipboardManager.setText(AnnotatedString(json))
                            exportExpanded = false
                        },
                    )
                    DropdownMenuItem(
                        text = { Text("Copy as CSV") },
                        onClick = {
                            val csv = ExperimentExporter.toCsv(
                                state.configs.toList(), state.results.toList(),
                            )
                            clipboardManager.setText(AnnotatedString(csv))
                            exportExpanded = false
                        },
                    )
                    DropdownMenuItem(
                        text = { Text("Copy as Markdown") },
                        onClick = {
                            val md = ExperimentExporter.toMarkdown(
                                state.prompt, state.systemPrompt, state.iterations,
                                state.configs.toList(), state.results.toList(),
                            )
                            clipboardManager.setText(AnnotatedString(md))
                            exportExpanded = false
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun MetadataChip(text: String) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        shape = MaterialTheme.shapes.extraSmall,
    ) {
        Text(
            text,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun formatDecimal(value: Float, decimals: Int): String {
    val factor = 10f.let { base -> (1..decimals).fold(1f) { acc, _ -> acc * base } }
    val rounded = kotlin.math.round(value * factor) / factor
    val str = rounded.toString()
    val dotIndex = str.indexOf('.')
    if (dotIndex == -1) return str + "." + "0".repeat(decimals)
    val currentDecimals = str.length - dotIndex - 1
    return if (currentDecimals >= decimals) str.take(dotIndex + decimals + 1)
    else str + "0".repeat(decimals - currentDecimals)
}

// Uses MarkdownText from MarkdownText.kt in same package
