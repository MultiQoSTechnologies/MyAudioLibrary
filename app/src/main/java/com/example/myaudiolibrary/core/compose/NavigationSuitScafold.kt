package com.example.myaudiolibrary.core.compose

import androidx.annotation.FloatRange
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.LocalContentColor
import androidx.compose.material.MaterialTheme
import androidx.compose.material.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

private const val TAG = "Scaffold2"

private const val LAYOUT_ID_PROGRESS_BAR = "_layout_id_progress_bar"

@Deprecated("This is no use from now on.")

@Composable
fun NavigationSuiteScaffold(
    vertical: Boolean,
    content: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    hideNavigationBar: Boolean = false,
    background: Color = MaterialTheme.colors.background,
    contentColor: Color = contentColorFor(backgroundColor = background),
    channel: Channel = remember(::Channel),
    @FloatRange(0.0, 1.0) progress: Float = Float.NaN,
    navBar: @Composable () -> Unit,
) {
    val realContent =
        @Composable {
            CompositionLocalProvider(
                value = LocalContentColor provides contentColor,
                content = content
            )
            SnackbarProvider(channel)
            when {
                progress == -1f -> LinearProgressIndicator(
                    modifier = Modifier.layoutId(LAYOUT_ID_PROGRESS_BAR)
                )
                !progress.isNaN() -> LinearProgressIndicator(
                    progress = progress,
                    modifier = Modifier.layoutId(LAYOUT_ID_PROGRESS_BAR)
                )
                else -> Unit
            }
            when {
                hideNavigationBar -> Unit
                else -> navBar()
            }
        }
    when (vertical) {
        true -> Vertical(content = realContent, modifier = modifier.background(background).fillMaxSize())
        else -> Horizontal(content = realContent, modifier = modifier.background(background).fillMaxSize())
    }
}

@Composable
private inline fun Vertical(
    content: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    var orgNavBarHeightPx by remember { mutableFloatStateOf(Float.NaN) }
    val orgAnmNavBarHeight by animateFloatAsState(targetValue = if (orgNavBarHeightPx.isNaN()) 0f else orgNavBarHeightPx, tween(500))
    var navBarHeight by remember { mutableFloatStateOf(0f) }

    Layout(
        content = content,
        modifier = modifier
            .fillMaxSize(),
    ) { measurables, constraints ->
        val width = constraints.maxWidth
        val height = constraints.maxHeight
        val navBarOffsetY = if (orgAnmNavBarHeight == 0f) 0f else orgAnmNavBarHeight - navBarHeight
        var h = height - navBarOffsetY.roundToInt()
        val placeableContent = measurables[0].measure(
            constraints.copy(minHeight = h, maxHeight = h)
        )
        val channelPlaceable = measurables[1].measure(
            constraints.copy(minWidth = 0, minHeight = 0)
        )
        val measurable = measurables.getOrNull(2)
        val placeable = measurable?.measure(constraints.copy(minHeight = 0))
        val placeableProgress =
            if (measurable?.layoutId == LAYOUT_ID_PROGRESS_BAR) placeable else null
        val placeableNavBar =
            if (measurable?.layoutId != LAYOUT_ID_PROGRESS_BAR) placeable else null

        orgNavBarHeightPx = placeableNavBar?.height?.toFloat() ?: Float.NaN
        layout(width, height) {
            var x: Int = 0
            var y: Int = 0
            placeableContent.placeRelative(0, 0)
            x = width / 2 - channelPlaceable.width / 2   // centre
            y = (height - channelPlaceable.height - navBarOffsetY).roundToInt()
            channelPlaceable.placeRelative(x, y)
            x = width / 2 - (placeableNavBar?.width ?: 0) / 2
            y = (height - navBarOffsetY).roundToInt()
            placeableNavBar?.placeRelative(x, y)
            x = width / 2 - (placeableProgress?.width ?: 0) / 2
            y = (height - (placeableProgress?.height ?: 0) - navBarOffsetY).roundToInt()
            placeableProgress?.placeRelative(x, y)
        }
    }
}

@Composable
private inline fun Horizontal(
    content: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
    Layout(
        content = content,
        modifier = modifier,
        measurePolicy = { measurables, constraints ->
            val width = constraints.maxWidth
            val height = constraints.maxHeight
            val measurable = when {
                measurables.size == 4 -> measurables[2]
                measurables.size == 3 && measurables[2].layoutId == LAYOUT_ID_PROGRESS_BAR -> measurables[2]
                else -> null
            }
            val placeableNavRail = measurables.getOrNull(if (measurable == null) 2 else 3)
                ?.measure(constraints.copy(minWidth = 0))
            var w = width - (placeableNavRail?.width ?: 0)
            val modified = constraints.copy(minWidth = w, maxWidth = w, minHeight = 0)
            val placeableProgressBar = measurable?.measure(modified)
            val placeableContent = measurables[0].measure(modified)
            w = (placeableContent.width * 0.8f).roundToInt()
            val placeableChannel = measurables[1].measure(modified.copy(minWidth = 0, minHeight = 0, maxWidth = w))
            layout(width, height) {
                var x: Int = placeableNavRail?.width ?: 0
                var y: Int = 0
                placeableContent.placeRelative(x, y)
                x = (placeableNavRail?.width
                    ?: 0) + (placeableContent.width / 2) - placeableChannel.width / 2   // centre
                y = (height - placeableChannel.height - 40.dp.toPx().roundToInt())
                placeableChannel.placeRelative(x, y)
                x = 0
                y = 0
                placeableNavRail?.placeRelative(x, y)
                x = placeableContent.width / 2 - (placeableProgressBar?.width ?: 0) / 2
                y = height - (placeableProgressBar?.height ?: 0)
                placeableProgressBar?.placeRelative(x, y)
            }
        }
    )
}