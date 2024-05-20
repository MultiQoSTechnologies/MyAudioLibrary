package com.example.myaudiolibrary.core.compose

import android.app.Activity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.toComposeRect
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.window.layout.WindowMetricsCalculator

private const val TAG = "WindowSize"

enum class Range {
    Compact,
    Medium,
    Large,
    xLarge
}

private fun fromWidth(width: Dp): Range {
    require(width >= 0.dp) { "Width must not be negative" }
    return when {
        width <= 500.dp -> Range.Compact
        width <= 700.dp -> Range.Medium
        width <= 900.dp -> Range.Large
        else -> Range.xLarge
    }
}

private fun fromHeight(height: Dp): Range {
    require(height >= 0.dp) { "Height must not be negative" }
    return when {
        height <= 500.dp -> Range.Compact
        height <= 700.dp -> Range.Medium
        height <= 900.dp -> Range.Large
        else -> Range.xLarge
    }
}

@Immutable
@JvmInline
value class WindowSize(val value: DpSize) {
    val widthRange: Range get() = fromWidth(value.width)
    val heightRange: Range get() = fromHeight(value.height)
    operator fun component1() = widthRange
    operator fun component2() = heightRange
    override fun toString() = "WindowSize($widthRange, $heightRange)"
}

@Composable
@ReadOnlyComposable
fun calculateWindowSizeClass(activity: Activity): WindowSize {
    LocalConfiguration.current
    val density = LocalDensity.current
    val metrics = WindowMetricsCalculator.getOrCreate().computeCurrentWindowMetrics(activity)
    val size = with(density) { metrics.bounds.toComposeRect().size.toDpSize() }
    return WindowSize(size)
}

val LocalWindowSize = compositionLocalOf<WindowSize> {
    error("No Window size defined.")
}