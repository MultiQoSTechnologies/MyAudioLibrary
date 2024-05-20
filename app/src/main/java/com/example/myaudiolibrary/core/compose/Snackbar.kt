package com.example.myaudiolibrary.core.compose

import androidx.annotation.StringRes
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.ContentAlpha
import androidx.compose.material.Icon
import androidx.compose.material.LocalContentColor
import androidx.compose.material.Surface
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.AccessibilityManager
import androidx.compose.ui.platform.LocalAccessibilityManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.dismiss
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.content.res.ResourcesCompat
import com.example.myaudiolibrary.ui.theme.SignalWhite
import com.example.myaudiolibrary.ui.theme.TrafficBlack
import com.example.myaudiolibrary.ui.theme.fontMedium
import com.example.myaudiolibrary.ui.theme.primary
import com.primex.core.Text
import com.primex.core.composableOrNull
import com.primex.core.get
import com.primex.core.value
import com.primex.material2.IconButton
import com.primex.material2.Label
import com.primex.material2.ListTile
import com.primex.material2.TextButton
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.coroutines.resume

@Stable
class Channel {

    enum class Duration {
        Short,
        Long,
        Indefinite
    }

    interface Data {

        val accent: Color
        val leading: Any?

        val title: Text?

        val message: Text
        val action: Text?
        val duration: Duration
        fun action()
        fun dismiss()
    }

    enum class Result {
        Dismissed,
        ActionPerformed,
    }

    private val mutex = Mutex()

    var current by mutableStateOf<Data?>(null)
        private set
    suspend fun show(
        message: Text,
        title: Text? = null,
        action: Text? = null,
        leading: Any? = null,
        accent: Color = Color.Unspecified,
        duration: Duration = Duration.Short
    ): Result = mutex.withLock {
        try {
            return suspendCancellableCoroutine { continuation ->
                current = Message(message, title, accent, leading, action, duration, continuation)
            }
        } finally {
            current = null
        }
    }

    suspend fun show(
        message: CharSequence,
        title: CharSequence? = null,
        action: CharSequence? = null,
        leading: Any? = null,
        accent: Color = Color.Unspecified,
        duration: Duration = Duration.Short
    ) = show(
        Text(message),
        title?.let { Text(it) },
        action = action?.let { Text(it) },
        leading = leading,
        accent = accent,
        duration = duration
    )

    suspend fun show(
        @StringRes message: Int,
        @StringRes title: Int = ResourcesCompat.ID_NULL,
        @StringRes action: Int = ResourcesCompat.ID_NULL,
        leading: Any? = null,
        accent: Color = Color.Unspecified,
        duration: Duration = Duration.Short
    ) = show(
        title = if (title == ResourcesCompat.ID_NULL) null else Text(title),
        message = Text(message),
        action = if (action == ResourcesCompat.ID_NULL) null else Text(action),
        leading = leading,
        accent = accent,
        duration = duration
    )
}

@Stable
private class Message(
    override val message: Text,
    override val title: Text? = null,
    override val accent: Color = Color.Unspecified,
    override val leading: Any? = null,
    override val action: Text? = null,
    override val duration: Channel.Duration = Channel.Duration.Indefinite,
    private val continuation: CancellableContinuation<Channel.Result>,
) : Channel.Data {

    override fun action() {
        if (continuation.isActive) continuation.resume(Channel.Result.ActionPerformed)
    }

    override fun dismiss() {
        if (continuation.isActive) continuation.resume(Channel.Result.Dismissed)
    }
}

private fun Channel.Duration.toMillis(
    hasAction: Boolean,
    accessibilityManager: AccessibilityManager?
): Long {
    val original = when (this) {
        Channel.Duration.Short -> 4000L
        Channel.Duration.Long -> 10000L
        Channel.Duration.Indefinite -> Long.MAX_VALUE
    }
    if (accessibilityManager == null) {
        return original
    }
    return accessibilityManager.calculateRecommendedTimeoutMillis(
        original, containsIcons = true, containsText = true, containsControls = hasAction
    )
}

@Composable
private fun FadeInFadeOutWithScale(
    current: Channel.Data?,
    modifier: Modifier = Modifier,
    content: @Composable (Channel.Data) -> Unit
) {
    val state = remember { FadeInFadeOutState<Channel.Data?>() }
    if (current != state.current) {
        state.current = current
        val keys = state.items.map { it.key }.toMutableList()
        if (!keys.contains(current)) {
            keys.add(current)
        }
        keys.filterNotNull().mapTo(state.items) { key ->
            FadeInFadeOutAnimationItem(key) { children ->
                val isVisible = key == current
                val duration = if (isVisible) ToastFadeInMillis else ToastFadeOutMillis
                val delay = ToastFadeOutMillis + ToastInBetweenDelayMillis
                val animationDelay = if (isVisible && keys.filterNotNull().size != 1) delay else 0
                val opacity = animatedOpacity(animation = tween(
                    easing = LinearEasing,
                    delayMillis = animationDelay,
                    durationMillis = duration
                ), visible = isVisible, onAnimationFinish = {
                    if (key != state.current) {
                        state.items.removeAll { it.key == key }
                        state.scope?.invalidate()
                    }
                })
                val scale = animatedScale(
                    animation = tween(
                        easing = FastOutSlowInEasing,
                        delayMillis = animationDelay,
                        durationMillis = duration
                    ), visible = isVisible
                )
                Box(
                    Modifier
                        .graphicsLayer(
                            scaleX = scale.value, scaleY = scale.value, alpha = opacity.value
                        )
                        .semantics {
                            liveRegion = LiveRegionMode.Polite
                            dismiss { key.dismiss(); true }
                        }) {
                    children()
                }
            }
        }
    }

    Box(modifier) {
        state.scope = currentRecomposeScope
        state.items.forEach { (item, opacity) ->
            key(item) {
                opacity {
                    content(item!!)
                }
            }
        }
    }
}

private class FadeInFadeOutState<T> {
    var current: Any? = Any()
    var items = mutableListOf<FadeInFadeOutAnimationItem<T>>()
    var scope: RecomposeScope? = null
}

private data class FadeInFadeOutAnimationItem<T>(
    val key: T,
    val transition: FadeInFadeOutTransition
)

private typealias FadeInFadeOutTransition = @Composable (content: @Composable () -> Unit) -> Unit

@Composable
private fun animatedOpacity(
    animation: AnimationSpec<Float>,
    visible: Boolean,
    onAnimationFinish: () -> Unit = {}
): State<Float> {
    val alpha = remember { Animatable(if (!visible) 1f else 0f) }
    LaunchedEffect(visible) {
        alpha.animateTo(
            if (visible) 1f else 0f, animationSpec = animation
        )
        onAnimationFinish()
    }
    return alpha.asState()
}

@Composable
private fun animatedScale(
    animation: AnimationSpec<Float>,
    visible: Boolean
): State<Float> {
    val scale = remember { Animatable(if (!visible) 1f else 0.8f) }
    LaunchedEffect(visible) {
        scale.animateTo(
            if (visible) 1f else 0.8f, animationSpec = animation
        )
    }
    return scale.asState()
}

private const val ToastFadeInMillis = 150
private const val ToastFadeOutMillis = 75
private const val ToastInBetweenDelayMillis = 0

private inline fun Indicatior(color: Color) = Modifier.drawBehind {
    drawRect(color = color, size = size.copy(width = 4.dp.toPx()))
}

@Composable
private fun Snackbar2(
    data: Channel.Data,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(4.dp),
    backgroundColor: Color = TrafficBlack,
    contentColor: Color = SignalWhite,
    actionColor: Color = primary,
    elevation: Dp = 6.dp,
) {
    Surface(
        modifier = modifier
            .padding(12.dp)
            .sizeIn(minHeight = 56.dp),
        shape = shape,
        elevation = elevation,
        color = backgroundColor,
        contentColor = contentColor,
    ) {
        ListTile(
            modifier = Indicatior(actionColor), centreVertically = true,
            leading = composableOrNull(data.leading != null) {
                val icon = data.leading
                Icon(
                    painter = when (icon) {
                        is ImageVector -> rememberVectorPainter(image = icon)
                        is Int -> painterResource(id = icon)
                        else -> error("$icon is neither resource nor ImageVector.")
                    }, contentDescription = null, tint = actionColor
                )
            },
            text = {
                Label(
                    text = data.message.value,
                    color = LocalContentColor.current,
                    style = fontMedium,
                    maxLines = 5,
                )
            },
            overlineText = composableOrNull(data.title != null) {
                Label(
                    text = data.title!!.get,
                    color = LocalContentColor.current.copy(ContentAlpha.high),
                    style = fontMedium,
                    fontWeight = FontWeight.SemiBold
                )
            },
            trailing = {
                if (data.action != null)
                    TextButton(
                        label = data.action!!.get,
                        onClick = { data.action() },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = actionColor
                        )
                    )
                else
                    IconButton(
                        onClick = { data.dismiss() },
                        imageVector = Icons.Outlined.Close,
                        contentDescription = null
                    )
            }
        )
    }
}

@Composable
fun SnackbarProvider(
    state: Channel,
    modifier: Modifier = Modifier,
    message: @Composable (Channel.Data) -> Unit = { Snackbar2(it) }
) {
    val currentSnackbarData = state.current
    val accessibilityManager = LocalAccessibilityManager.current
    LaunchedEffect(currentSnackbarData) {
        if (currentSnackbarData != null) {
            val duration = currentSnackbarData.duration.toMillis(
                currentSnackbarData.action != null, accessibilityManager
            )
            delay(duration)
            currentSnackbarData.dismiss()
        }
    }
    FadeInFadeOutWithScale(
        current = state.current, modifier = modifier, content = message
    )
}