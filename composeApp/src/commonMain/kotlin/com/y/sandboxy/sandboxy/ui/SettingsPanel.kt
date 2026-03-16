package com.y.sandboxy.sandboxy.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.y.sandboxy.sandboxy.model.LlmParams
import kotlin.math.pow
import kotlin.math.round
import kotlin.math.roundToInt

@Composable
fun SettingsPanel(
    visible: Boolean,
    params: LlmParams,
    onParamsChange: (LlmParams) -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = visible,
        enter = slideInHorizontally(
            initialOffsetX = { it },
            animationSpec = tween(300),
        ),
        exit = slideOutHorizontally(
            targetOffsetX = { it },
            animationSpec = tween(250),
        ),
        modifier = modifier,
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 2.dp,
            modifier = Modifier
                .width(320.dp)
                .fillMaxHeight(),
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                Text(
                    text = "Settings",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.height(24.dp))

                // Section: Model Parameters
                Text(
                    text = "Model Parameters",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.height(12.dp))

                ParamSlider(
                    label = "Temperature",
                    value = params.temperature,
                    range = 0f..2f,
                    steps = 19,
                    format = { formatFloat(it, 1) },
                    onValueChange = { onParamsChange(params.copy(temperature = it)) },
                )

                ParamSlider(
                    label = "Top-K",
                    value = params.topK.toFloat(),
                    range = 0f..100f,
                    steps = 99,
                    format = { it.roundToInt().toString() },
                    onValueChange = { onParamsChange(params.copy(topK = it.roundToInt())) },
                )

                ParamSlider(
                    label = "Top-P",
                    value = params.topP,
                    range = 0f..1f,
                    steps = 19,
                    format = { formatFloat(it, 2) },
                    onValueChange = { onParamsChange(params.copy(topP = it)) },
                )

                Spacer(Modifier.height(16.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(Modifier.height(16.dp))

                // Section: Generation Limits
                Text(
                    text = "Generation Limits",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.height(12.dp))

                ParamSlider(
                    label = "Max Tokens",
                    value = params.maxTokens.toFloat(),
                    range = 256f..16384f,
                    steps = 62,
                    format = { it.roundToInt().toString() },
                    onValueChange = { onParamsChange(params.copy(maxTokens = it.roundToInt())) },
                )

                ParamSlider(
                    label = "Context Window",
                    value = params.contextWindowLimit.toFloat(),
                    range = 2f..50f,
                    steps = 47,
                    format = { "${it.roundToInt()} messages" },
                    onValueChange = { onParamsChange(params.copy(contextWindowLimit = it.roundToInt())) },
                )

                Spacer(Modifier.height(24.dp))

                OutlinedButton(
                    onClick = onReset,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Reset to Defaults")
                }
            }
        }
    }
}

@Composable
private fun ParamSlider(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    steps: Int,
    format: (Float) -> String,
    onValueChange: (Float) -> Unit,
) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerHighest,
                shape = MaterialTheme.shapes.extraSmall,
            ) {
                Text(
                    text = format(value),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                )
            }
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = range,
            steps = steps,
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary,
                inactiveTrackColor = MaterialTheme.colorScheme.outlineVariant,
            ),
        )
    }
}

private fun formatFloat(value: Float, decimals: Int): String {
    val factor = 10f.pow(decimals)
    val rounded = round(value * factor) / factor
    val str = rounded.toString()
    val dotIndex = str.indexOf('.')
    if (dotIndex == -1) return str + "." + "0".repeat(decimals)
    val currentDecimals = str.length - dotIndex - 1
    return if (currentDecimals >= decimals) str.take(dotIndex + decimals + 1)
    else str + "0".repeat(decimals - currentDecimals)
}
