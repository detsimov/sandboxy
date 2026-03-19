package com.y.sandboxy.sandboxy.state

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.y.sandboxy.sandboxy.model.AvailableModels
import com.y.sandboxy.sandboxy.model.ChatMessage
import com.y.sandboxy.sandboxy.model.ExperimentConfig
import com.y.sandboxy.sandboxy.model.ExperimentPreset
import com.y.sandboxy.sandboxy.model.ExperimentResult
import com.y.sandboxy.sandboxy.model.LlmParams
import com.y.sandboxy.sandboxy.model.MessageRole
import com.y.sandboxy.sandboxy.model.Rating
import com.y.sandboxy.sandboxy.model.StreamChunk
import com.y.sandboxy.sandboxy.repository.ChatRepository
import com.y.sandboxy.sandboxy.storage.ExperimentHistoryEntry
import com.y.sandboxy.sandboxy.storage.ExperimentHistoryRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

class ExperimentState {
    val configs = mutableStateListOf<ExperimentConfig>()
    val results = mutableStateListOf<ExperimentResult>()

    var prompt by mutableStateOf("")
    var systemPrompt by mutableStateOf("")
    var showSystemPrompt by mutableStateOf(false)
    var iterations by mutableStateOf(1)
    var isRunning by mutableStateOf(false)
    var showHistory by mutableStateOf(false)
    val historyEntries = mutableStateListOf<ExperimentHistoryEntry>()

    // Diff mode
    var diffMode by mutableStateOf(false)
    var diffSelections = mutableStateListOf<Pair<String, Int>>() // (configId, iterationIndex)

    private val jobs = mutableMapOf<String, Job>()
    private var configCounter = 0

    init {
        addConfig()
        addConfig()
    }

    // --- Config CRUD ---

    fun addConfig() {
        if (configs.size >= MAX_CONFIGS) return
        configCounter++
        val name = "Config ${('A' + configCounter - 1).toChar()}"
        configs.add(
            ExperimentConfig(
                name = name,
                colorIndex = (configCounter - 1) % CONFIG_COLORS.size,
            )
        )
    }

    fun removeConfig(id: String) {
        if (configs.size <= 1) return
        configs.removeAll { it.id == id }
    }

    fun duplicateConfig(id: String) {
        if (configs.size >= MAX_CONFIGS) return
        val source = configs.find { it.id == id } ?: return
        configCounter++
        configs.add(
            source.copy(
                id = generateId(),
                name = "${source.name} (copy)",
                colorIndex = (configCounter - 1) % CONFIG_COLORS.size,
            )
        )
    }

    fun updateConfig(id: String, update: (ExperimentConfig) -> ExperimentConfig) {
        val index = configs.indexOfFirst { it.id == id }
        if (index != -1) {
            configs[index] = update(configs[index])
        }
    }

    fun applyPreset(preset: ExperimentPreset) {
        configs.clear()
        configCounter = 0
        val presetConfigs = preset.createConfigs()
        presetConfigs.forEach { config ->
            configCounter++
            configs.add(config.copy(colorIndex = (configCounter - 1) % CONFIG_COLORS.size))
        }
    }

    // --- Experiment Execution ---

    fun runExperiment(repository: ChatRepository, scope: CoroutineScope) {
        val text = prompt.trim()
        if (text.isEmpty() || isRunning) return

        results.clear()
        isRunning = true

        // Pre-populate result placeholders for all configs x iterations
        configs.forEach { config ->
            repeat(iterations) { iter ->
                results.add(
                    ExperimentResult(
                        configId = config.id,
                        iterationIndex = iter,
                        isStreaming = true,
                    )
                )
            }
        }

        // Launch one coroutine per config (parallel), iterations sequential within
        configs.forEach { config ->
            val job = scope.launch {
                for (iter in 0 until iterations) {
                    runSingleIteration(config, iter, text, repository)
                }
            }
            jobs[config.id] = job
        }

        // Monitor completion + auto-save
        scope.launch {
            jobs.values.forEach { it.join() }
            isRunning = false
            jobs.clear()
            // Auto-save to history
            if (results.any { !it.isStreaming && it.responseText.isNotEmpty() }) {
                ExperimentHistoryRepository.save(
                    prompt = text,
                    systemPrompt = systemPrompt,
                    iterations = iterations,
                    configs = configs.toList(),
                    results = results.toList(),
                )
            }
        }
    }

    private suspend fun runSingleIteration(
        config: ExperimentConfig,
        iterationIndex: Int,
        promptText: String,
        repository: ChatRepository,
    ) {
        val resultIndex = results.indexOfFirst {
            it.configId == config.id && it.iterationIndex == iterationIndex
        }
        if (resultIndex == -1) return

        val effectiveParams = config.params.copy(
            systemPrompt = systemPrompt.ifBlank { config.params.systemPrompt },
        )
        val messages = listOf(ChatMessage(role = MessageRole.User, content = promptText))

        val startTime = currentTimeMillis()
        var inputTokens: Int? = null
        var outputTokens: Int? = null

        try {
            repository.sendMessageStreamingWithUsage(
                messages = messages,
                model = config.model,
                params = effectiveParams,
            ).catch { e ->
                if (e !is CancellationException) {
                    results[resultIndex] = results[resultIndex].copy(
                        isStreaming = false,
                        responseText = "[Error: ${e.message}]",
                    )
                }
            }.collect { chunk ->
                when (chunk) {
                    is StreamChunk.TextDelta -> {
                        val current = results[resultIndex]
                        results[resultIndex] = current.copy(
                            responseText = current.responseText + chunk.text,
                        )
                    }
                    is StreamChunk.Usage -> {
                        inputTokens = chunk.inputTokens
                        outputTokens = chunk.outputTokens
                    }
                }
            }

            val endTime = currentTimeMillis()
            results[resultIndex] = results[resultIndex].copy(
                isStreaming = false,
                latencyMs = endTime - startTime,
                inputTokens = inputTokens,
                outputTokens = outputTokens,
                modelId = config.model.id,
            )
        } catch (e: CancellationException) {
            results[resultIndex] = results[resultIndex].copy(isStreaming = false)
            throw e
        }
    }

    fun cancelExperiment() {
        jobs.values.forEach { it.cancel() }
        jobs.clear()
        // Finalize any streaming results
        results.forEachIndexed { i, r ->
            if (r.isStreaming) {
                results[i] = r.copy(isStreaming = false)
            }
        }
        isRunning = false
    }

    // --- Rating ---

    fun rateResult(configId: String, iterationIndex: Int, rating: Rating?) {
        val index = results.indexOfFirst {
            it.configId == configId && it.iterationIndex == iterationIndex
        }
        if (index != -1) {
            results[index] = results[index].copy(rating = rating)
        }
    }

    // --- Diff Mode ---

    fun enterDiffMode() {
        diffMode = true
        diffSelections.clear()
    }

    fun exitDiffMode() {
        diffMode = false
        diffSelections.clear()
    }

    fun toggleDiffSelection(configId: String, iterationIndex: Int) {
        val pair = configId to iterationIndex
        val existing = diffSelections.indexOfFirst { it.first == pair.first && it.second == pair.second }
        if (existing != -1) {
            diffSelections.removeAt(existing)
        } else if (diffSelections.size < 2) {
            diffSelections.add(pair)
        }
    }

    fun getDiffTexts(): Pair<String, String>? {
        if (diffSelections.size != 2) return null
        val (cid1, iter1) = diffSelections[0]
        val (cid2, iter2) = diffSelections[1]
        val text1 = results.find { it.configId == cid1 && it.iterationIndex == iter1 }?.responseText ?: return null
        val text2 = results.find { it.configId == cid2 && it.iterationIndex == iter2 }?.responseText ?: return null
        return text1 to text2
    }

    // --- History ---

    fun loadHistory() {
        historyEntries.clear()
        historyEntries.addAll(ExperimentHistoryRepository.list())
    }

    fun deleteHistoryEntry(id: String) {
        ExperimentHistoryRepository.delete(id)
        historyEntries.removeAll { it.id == id }
    }

    fun replayFromHistory(id: String) {
        val saved = ExperimentHistoryRepository.load(id) ?: return
        prompt = saved.prompt
        systemPrompt = saved.systemPrompt
        iterations = saved.iterations
        if (saved.systemPrompt.isNotBlank()) showSystemPrompt = true
        showHistory = false
    }

    // --- Helpers ---

    fun resultsForConfig(configId: String): List<ExperimentResult> =
        results.filter { it.configId == configId }.sortedBy { it.iterationIndex }

    val hasResults: Boolean
        get() = results.any { !it.isStreaming && it.responseText.isNotEmpty() }

    companion object {
        const val MAX_CONFIGS = 6
        val CONFIG_COLORS = listOf(
            0xFF6C9BF2, // blue
            0xFFE87F6F, // red/coral
            0xFF7BD389, // green
            0xFFE5A54B, // amber
            0xFFC084EB, // purple
            0xFF5BC5C8, // teal
        )
    }
}

private fun generateId(): String {
    val chars = "abcdefghijklmnopqrstuvwxyz0123456789"
    return (1..8).map { chars[kotlin.random.Random.nextInt(chars.length)] }.joinToString("")
}

private fun currentTimeMillis(): Long = kotlin.time.Clock.System.now().toEpochMilliseconds()
