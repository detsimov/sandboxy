package com.y.sandboxy.sandboxy.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.y.sandboxy.sandboxy.model.UsageMetrics
import kotlinx.coroutines.delay

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MetricsBar(
    metrics: UsageMetrics,
    isStreaming: Boolean,
    modifier: Modifier = Modifier,
) {
    val hasAnyMetrics = metrics.inputTokens != null || metrics.outputTokens != null || metrics.totalTimeMs != null

    // Live timer during streaming
    var elapsedMs by remember { mutableStateOf(0L) }
    LaunchedEffect(isStreaming) {
        if (isStreaming) {
            val start = kotlin.time.Clock.System.now().toEpochMilliseconds()
            while (true) {
                elapsedMs = kotlin.time.Clock.System.now().toEpochMilliseconds() - start
                delay(100)
            }
        } else {
            elapsedMs = 0
        }
    }

    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = 0.dp,
        modifier = modifier,
    ) {
        if (isStreaming) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                MetricChip("Time", "${elapsedMs / 1000}.${(elapsedMs % 1000) / 100}s")
            }
        } else if (hasAnyMetrics) {
            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                MetricChip("In", metrics.inputTokens?.toString() ?: "N/A")
                MetricChip("Out", metrics.outputTokens?.toString() ?: "N/A")
                MetricChip("Total", metrics.totalTokens?.toString() ?: "N/A")
                MetricChip("TTFT", metrics.timeToFirstTokenMs?.let { "${it}ms" } ?: "N/A")
                MetricChip("Time", metrics.totalTimeMs?.let { formatDuration(it) } ?: "N/A")
                MetricChip(
                    "Speed",
                    metrics.tokensPerSecond?.let { "${formatOneDecimal(it)} t/s" } ?: "N/A",
                )
            }
        }
    }
}

@Composable
private fun MetricChip(label: String, value: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            value,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

internal fun formatOneDecimal(value: Double): String {
    val rounded = kotlin.math.round(value * 10) / 10
    return rounded.toString()
}

internal fun formatDuration(ms: Long): String {
    return if (ms < 1000) "${ms}ms"
    else "${ms / 1000}.${(ms % 1000) / 100}s"
}
