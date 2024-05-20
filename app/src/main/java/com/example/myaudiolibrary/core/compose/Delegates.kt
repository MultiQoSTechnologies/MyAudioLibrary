@file:Suppress("NOTHING_TO_INLINE")

package com.example.myaudiolibrary.core.compose

import android.graphics.Typeface
import androidx.annotation.DrawableRes
import androidx.annotation.RawRes
import androidx.compose.animation.core.AnimationConstants
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.graphics.ExperimentalAnimationGraphicsApi
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.size
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.airbnb.lottie.AsyncUpdates
import com.airbnb.lottie.RenderMode
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieDynamicProperties
import com.airbnb.lottie.compose.rememberLottieComposition
import com.example.myaudiolibrary.R
import com.example.myaudiolibrary.core.LongDurationMills
import com.primex.material2.Label
import com.primex.material2.Placeholder
import kotlin.math.roundToLong
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieClipSpec

@Composable
inline fun Artwork(
    data: Any?,
    modifier: Modifier = Modifier,
    fallback: Painter? = painterResource(id = R.drawable.ic_default_music_icon),
    contentScale: ContentScale = ContentScale.Crop,
    alignment: Alignment = Alignment.Center,
    fadeMills: Int = AnimationConstants.DefaultDurationMillis,
    transformers: List<coil.transform.Transformation>? = null,
) {
    val context = LocalContext.current
    val request = remember(data) {
        ImageRequest
            .Builder(context).apply {
                data(data)
                if (transformers != null)
                    transformations(transformers)
                crossfade(fadeMills)
            }
            .build()
    }
    AsyncImage(
        model = request,
        contentDescription = null,
        error = fallback,
        modifier = modifier,
        contentScale = contentScale,
        alignment = alignment,
    )
}

@Composable
@Deprecated("The reason for deprication of this is that it doesnt morph for all window sizes.")
inline fun Placeholder(
    title: String,
    modifier: Modifier = Modifier,
    vertical: Boolean = true,
    @RawRes iconResId: Int,
    message: String? = null,
    noinline action: @Composable (() -> Unit)? = null
) {
    Placeholder(
        modifier = modifier,
        vertical = vertical,
        message = { if (message != null) Text(text = message) },
        title = { Label(text = title.ifEmpty { " " }, maxLines = 2) },

        icon = {
            LottieAnimation(
                id = iconResId, iterations = Int.MAX_VALUE
            )
        },
        action = action,
    )
}

@Composable
inline fun LottieAnimation(
    @RawRes id: Int,
    modifier: Modifier = Modifier,
    atEnd: Boolean = false,
    scale: Float = 1f,
    progressRange: ClosedFloatingPointRange<Float> = 0f..1f,
    duration: Int = -1,
    easing: Easing = FastOutSlowInEasing
) {
    val composition by rememberLottieComposition(spec = LottieCompositionSpec.RawRes(id))
    val duration2 = composition?.duration?.roundToLong() ?: AnimationConstants.LongDurationMills
    val progress by animateFloatAsState(
        targetValue = if (atEnd) progressRange.start else progressRange.endInclusive,
        label = "Lottie $id",
        animationSpec = tween(if (duration == -1) duration2.toInt() else duration, easing = easing)
    )
    LottieAnimation(
        composition = composition,
        progress = { progress },
        modifier = Modifier
            .size(24.dp)
            .scale(scale)
            .then(modifier),
    )
}

@Composable
inline fun LottieAnimation1(
    @RawRes id: Int,
    modifier: Modifier = Modifier,
    atEnd: Boolean = false,
    progressRange: ClosedFloatingPointRange<Float> = 0f..1f,
    duration: Int = -1,
    easing: Easing = FastOutSlowInEasing
) {
    val composition by rememberLottieComposition(spec = LottieCompositionSpec.RawRes(id))
    val duration2 = composition?.duration?.roundToLong() ?: AnimationConstants.LongDurationMills
    val progress by animateFloatAsState(
        targetValue = if (atEnd) progressRange.start else progressRange.endInclusive,
        label = "Lottie $id",
        animationSpec = tween(if (duration == -1) duration2.toInt() else duration, easing = easing)
    )
    LottieAnimation(
        composition = composition,
        progress = { progress },
        modifier = Modifier
            .size(40.dp)
            .then(modifier),
    )
}

@Composable
inline fun LottieAnimation(
    @RawRes id: Int,
    modifier: Modifier = Modifier,
    scale: Float = 1f,
    isPlaying: Boolean = true,
    restartOnPlay: Boolean = true,
    clipSpec: LottieClipSpec? = null,
    speed: Float = 1f,
    iterations: Int = 1,
    outlineMasksAndMattes: Boolean = false,
    applyOpacityToLayers: Boolean = false,
    enableMergePaths: Boolean = false,
    renderMode: RenderMode = RenderMode.AUTOMATIC,
    reverseOnRepeat: Boolean = false,
    maintainOriginalImageBounds: Boolean = false,
    dynamicProperties: LottieDynamicProperties? = null,
    alignment: Alignment = Alignment.Center,
    contentScale: ContentScale = ContentScale.Fit,
    clipToCompositionBounds: Boolean = true,
    fontMap: Map<String, Typeface>? = null,
    asyncUpdates: AsyncUpdates = AsyncUpdates.AUTOMATIC
) {
    val composition by rememberLottieComposition(
        spec = LottieCompositionSpec.RawRes(id)
    )
    LottieAnimation(
        composition,
        Modifier
            .size(24.dp)
            .scale(scale)
            .then(modifier),
        isPlaying,
        restartOnPlay,
        clipSpec,
        speed,
        iterations,
        outlineMasksAndMattes,
        applyOpacityToLayers,
        enableMergePaths,
        renderMode,
        reverseOnRepeat,
        maintainOriginalImageBounds,
        dynamicProperties,
        alignment,
        contentScale,
        clipToCompositionBounds,
        fontMap = fontMap,
        asyncUpdates = asyncUpdates
    )
}

@Composable
inline fun LottieAnimButton(
    @RawRes id: Int,
    noinline onClick: () -> Unit,
    modifier: Modifier = Modifier,
    atEnd: Boolean = false,
    scale: Float = 1f,
    progressRange: ClosedFloatingPointRange<Float> = 0f..1f,
    duration: Int = -1,
    enabled: Boolean = true,
    easing: Easing = FastOutSlowInEasing,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
) {
    IconButton(
        onClick = onClick,
        modifier,
        enabled,
        interactionSource,
        content = {
            LottieAnimation(
                id = id,
                atEnd = atEnd,
                scale = scale,
                easing = easing,
                progressRange = progressRange,
                duration = duration
            )
        }
    )
}

@Composable
inline fun LottieAnimButton1(
    @RawRes id: Int,
    noinline onClick: () -> Unit,
    modifier: Modifier = Modifier,
    atEnd: Boolean = false,
    progressRange: ClosedFloatingPointRange<Float> = 0f..1f,
    duration: Int = -1,
    enabled: Boolean = true,
    easing: Easing = FastOutSlowInEasing,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
) {
    IconButton(
        onClick = onClick,
        modifier,
        enabled,
        interactionSource,
        content = {
            LottieAnimation1(
                id = id,
                atEnd = atEnd,
                easing = easing,
                progressRange = progressRange,
                duration = duration
            )
        }
    )
}

@OptIn(ExperimentalAnimationGraphicsApi::class)
@Composable
inline fun AnimatedIconButton(
    @DrawableRes id: Int,
    noinline onClick: () -> Unit,
    modifier: Modifier = Modifier,
    atEnd: Boolean = false,
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    tint: Color = Color.Unspecified
) {
    IconButton(onClick = onClick, modifier = modifier, enabled, interactionSource) {
        Icon(
            painter = rememberAnimatedVectorResource(id = id, atEnd = atEnd),
            modifier = Modifier.size(30.dp),
            contentDescription = null,
            tint = tint
        )
    }
}
