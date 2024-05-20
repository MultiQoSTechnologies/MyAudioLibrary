package com.example.myaudiolibrary.console

import android.app.Activity
import android.net.Uri
import android.text.format.DateUtils.formatElapsedTime
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.annotation.IntDef
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.LinearEasing
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.SliderDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.KeyboardDoubleArrowLeft
import androidx.compose.material.icons.outlined.KeyboardDoubleArrowRight
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.media3.common.Player
import com.example.myaudiolibrary.R
import com.example.myaudiolibrary.core.Anim
import com.example.myaudiolibrary.core.MediumDurationMills
import com.example.myaudiolibrary.core.compose.AnimatedIconButton
import com.example.myaudiolibrary.core.compose.Artwork
import com.example.myaudiolibrary.core.compose.LocalNavController
import com.example.myaudiolibrary.core.compose.LocalSystemFacade
import com.example.myaudiolibrary.core.compose.LocalWindowSize
import com.example.myaudiolibrary.core.compose.LottieAnimButton1
import com.example.myaudiolibrary.core.compose.LottieAnimation
import com.example.myaudiolibrary.core.compose.Range
import com.example.myaudiolibrary.core.compose.WindowSize
import com.example.myaudiolibrary.core.compose.marque
import com.example.myaudiolibrary.core.compose.preference
import com.example.myaudiolibrary.core.playback.artworkUri
import com.example.myaudiolibrary.core.playback.subtitle
import com.example.myaudiolibrary.core.playback.title
import com.example.myaudiolibrary.settings.Settings
import com.example.myaudiolibrary.soundeffects.AudioFx
import com.example.myaudiolibrary.ui.theme.blackColor
import com.example.myaudiolibrary.ui.theme.fontMedium
import com.example.myaudiolibrary.ui.theme.fontSemiBold
import com.example.myaudiolibrary.ui.theme.lightPink
import com.example.myaudiolibrary.ui.theme.lightPurple
import com.example.myaudiolibrary.ui.theme.lightWhitePink
import com.example.myaudiolibrary.ui.theme.lightWhitePurple
import com.example.myaudiolibrary.ui.theme.whiteColor
import com.primex.material2.IconButton
import com.primex.material2.Label
import com.primex.material2.OutlinedButton2
import com.primex.material2.Text
import ir.mahozad.multiplatform.wavyslider.material.WavySlider

private const val TAG = "ConsoleView"

private const val SEEKBAR_STYLE_SIMPLE = 0

private const val SEEKBAR_STYLE_WAVY = 1

@IntDef(SEEKBAR_STYLE_SIMPLE, SEEKBAR_STYLE_WAVY)
@Target(AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.SOURCE)
@MustBeDocumented
private annotation class Seekbar

private const val PLAY_BUTTON_STYLE_SIMPLE = 0

private const val PLAY_BUTTON_STYLE_NEUMORPHIC = 1

@IntDef(PLAY_BUTTON_STYLE_SIMPLE, PLAY_BUTTON_STYLE_NEUMORPHIC)
@Target(AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.SOURCE)
@MustBeDocumented
private annotation class PlayButton

private const val REQUEST_SHOW_PLAYING_QUEUE = 1

private const val REQUEST_SHOW_PROPERTIES = 2

private const val REQUEST_HANDLE_BACK_PRESS = 3


private const val REQUEST_REQUIRES_LIGHT_SYSTEM_BARS = 4

private const val REQUEST_REQUIRES_DARK_SYSTEM_BARS = 5

private const val REQUEST_TOOGLE_LOCK = 6

private const val REQUEST_TOGGLE_VISIBILITY = 7

private const val REQUEST_TOGGLE_ROTATION_LOCK = 8

@IntDef(
    REQUEST_HANDLE_BACK_PRESS,
    REQUEST_SHOW_PLAYING_QUEUE,
    REQUEST_SHOW_PROPERTIES,
    REQUEST_REQUIRES_LIGHT_SYSTEM_BARS,
    REQUEST_REQUIRES_DARK_SYSTEM_BARS,
    REQUEST_TOOGLE_LOCK,
    REQUEST_TOGGLE_VISIBILITY,
    REQUEST_TOGGLE_ROTATION_LOCK
)
@Target(AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.PROPERTY, AnnotationTarget.TYPE)
@Retention(AnnotationRetention.SOURCE)
@MustBeDocumented
private annotation class Request

private val WindowInsets.asDpRect: DpRect
    @Composable
    @ReadOnlyComposable
    get() {
        val ld =
            LocalLayoutDirection.current
        val density = LocalDensity.current
        with(density) {
            return DpRect(
                left = getLeft(density, ld).toDp(),
                right = getRight(this, ld).toDp(),
                top = getTop(this).toDp(),
                bottom = getBottom(this).toDp()
            )
        }
    }

fun DpSize.contains(other: DpSize): Boolean {
    return this.width >= other.width && this.height >= other.height
}

private inline val Console.title: String?
    get() = current?.title?.toString()

private inline val Console.subtitle: String?
    get() = current?.subtitle?.toString()

private inline val Console.artworkUri: Uri?
    get() = current?.artworkUri

private fun WindowInsetsControllerCompat.immersiveMode(enable: Boolean) =
    if (enable) hide(WindowInsetsCompat.Type.systemBars()) else show(WindowInsetsCompat.Type.systemBars())

private fun Console.ensureAlwaysVisible(enabled: Boolean) {
    visibility = when {
        visibility == Console.VISIBILITY_LOCKED -> return
        !isVideo -> return
        visibility == Console.VISIBILITY_ALWAYS && !enabled -> Console.VISIBILITY_VISIBLE
        else -> Console.VISIBILITY_ALWAYS
    }
}

@Composable
@NonRestartableComposable
private fun PlayButton(
    onClick: () -> Unit,
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    @PlayButton style: Int = PLAY_BUTTON_STYLE_NEUMORPHIC,
) {
    when (style) {
        PLAY_BUTTON_STYLE_SIMPLE ->
            IconButton(
                painter = painterResource(id = if (isPlaying) R.drawable.media3_notification_pause else R.drawable.media3_notification_play),
                modifier = modifier.scale(1.5f),
                onClick = onClick
            )

        PLAY_BUTTON_STYLE_NEUMORPHIC -> Box(
            modifier = Modifier
                .size(70.dp)
                .clip(shape = CircleShape)
                .background(
                    whiteColor
                )
                .border(
                    width = 3.dp,
                    color = lightWhitePurple,
                    shape = CircleShape
                )
                .clickable(
                    onClick = {
                        onClick()
                    },
                ),
            contentAlignment = Alignment.Center
        ) {
            LottieAnimation(
                id = R.raw.lt_play_pause,
                atEnd = !isPlaying,
                scale = 1.5f,
                progressRange = 0.0f..0.29f,
                duration = Anim.MediumDurationMills,
                easing = LinearEasing
            )
        }
        // handle others
        else -> TODO("$style Not Implemented Yet!")
    }
}

private val NoOpPointerInput = Modifier.pointerInput(Unit) {}

@Composable
private fun TimeBar(
    state: Console,
    modifier: Modifier = Modifier,
    @Seekbar style: Int = SEEKBAR_STYLE_WAVY,
) = Row(
    verticalAlignment = Alignment.CenterVertically,
    modifier = modifier.then(NoOpPointerInput)
) {
    val value = if (state.isSeekable) state.progress else Float.NaN
    when (value.isNaN()) {
        true -> LinearProgressIndicator(
            modifier = modifier,
            color = whiteColor,
            strokeCap = StrokeCap.Round,
        )

        else -> WavySlider(
            value = value,
            onValueChange = {
                state.progress = it
            },
            modifier = modifier,
            waveLength = if (style == SEEKBAR_STYLE_SIMPLE) 0.dp else 20.dp,
            waveHeight = if (style == SEEKBAR_STYLE_SIMPLE) 0.dp else 7.dp,
            incremental = true,
            colors = SliderDefaults.colors(activeTrackColor = whiteColor, thumbColor = whiteColor)
        )
    }
}

@Composable
private inline fun ControlsBottomBar(
    state: Console,
    modifier: Modifier = Modifier,
    @PlayButton style: Int = PLAY_BUTTON_STYLE_NEUMORPHIC,
) = Row(
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween,
    modifier = modifier
        .then(NoOpPointerInput)
        .fillMaxWidth()
) {
    val shuffle = state.shuffle

    LottieAnimButton1(
        id = R.raw.lt_shuffle_on_off,
        onClick = { state.toggleShuffle() },
        atEnd = !shuffle,
        progressRange = 0f..0.8f,
    )

    var enabled = !state.isFirst
    val onColor = whiteColor
    Box(
        modifier = Modifier
            .size(45.dp)
            .clip(shape = CircleShape)
            .border(width = 2.dp, color = lightWhitePurple, shape = CircleShape)
            .background(lightPurple), contentAlignment = Alignment.Center
    ) {
        IconButton(
            onClick = { state.skipToPrev() },
            painter = rememberVectorPainter(image = Icons.Outlined.KeyboardDoubleArrowLeft),
            contentDescription = null,
            enabled = enabled,
            tint = onColor.copy(if (enabled) 1.00f else 0.74f),
            modifier = Modifier.size(30.dp)
        )
    }

    // play_button
    PlayButton(
        onClick = { state.togglePlay() },
        isPlaying = state.isPlaying,
        modifier = Modifier
            .padding(horizontal = 8.dp)
            .size(60.dp),
        style = style
    )

    // Skip to Next
    enabled = !state.isLast
    Box(
        modifier = Modifier
            .size(45.dp)
            .clip(shape = CircleShape)
            .border(width = 2.dp, color = lightWhitePurple, shape = CircleShape)
            .background(lightPurple), contentAlignment = Alignment.Center
    ) {
        IconButton(
            onClick = { state.skipToNext() },
            painter = rememberVectorPainter(image = Icons.Outlined.KeyboardDoubleArrowRight),
            contentDescription = null,
            enabled = enabled,
            tint = onColor.copy(if (enabled) 1.00f else 0.74f),
            modifier = Modifier.size(30.dp)
        )
    }

    // CycleRepeatMode
    val mode = state.repeatMode
    AnimatedIconButton(
        id = R.drawable.avd_repeat_more_one_all,
        onClick = { state.cycleRepeatMode() },
        atEnd = mode == Player.REPEAT_MODE_ALL,
        tint = onColor.copy(if (mode == Player.REPEAT_MODE_OFF) 0.38f else 1.00f)
    )
}


@Composable
private fun OtherOptions(
    state: Console,
    modifier: Modifier = Modifier,
) = Row(
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween,
    modifier = modifier.then(NoOpPointerInput)
) {
    var expanded by remember { mutableIntStateOf(0) }
    val controller = LocalNavController.current
    val useBuiltIn by preference(key = Settings.USE_IN_BUILT_AUDIO_FX)
    val facade = LocalSystemFacade.current
    // Speed Controller.
    PlaybackSpeed(
        expanded = expanded == 2,
        value = state.playbackSpeed,
        onValueChange = {
            if (it != -1f)
                state.playbackSpeed = it
            state.ensureAlwaysVisible(false)
            expanded = 0;
        }
    )

    // Sleep Timer.
    SleepTimer(
        expanded = expanded == 3,
        onValueChange = {
            if (it != -2L)
                state.sleepAfterMills = it
            expanded = 0
            state.ensureAlwaysVisible(false)
        }
    )

    // Speed Controller.
    IconButton(
        onClick = { expanded = 2; state.ensureAlwaysVisible(true) },
        painter = rememberVectorPainter(image = Icons.Outlined.Speed),
        tint = whiteColor,
        modifier = Modifier.size(50.dp)
    )

    // SleepAfter.
    IconButton(
        onClick = { expanded = 3; state.ensureAlwaysVisible(true) },
        content = {
            val mills = state.sleepAfterMills
            Crossfade(targetState = mills != -1L, label = "SleepAfter CrossFade") { show ->
                when (show) {
                    true -> Label(
                        text = formatElapsedTime(mills / 1000L),
                        style = fontMedium.copy(fontSize = 15.sp, color = Color.Red),
                    )

                    else -> Icon(
                        imageVector = Icons.Outlined.Timer,
                        contentDescription = null,
                        tint = whiteColor
                    )
                }
            }
        },
    )

    // Equalizer
    IconButton(
        imageVector = Icons.Outlined.Tune,
        onClick = {
            if (useBuiltIn)
                controller.navigate(AudioFx.route)
            else
                facade.launchEqualizer(state.audioSessionId)
            expanded = 0
        },
        tint = whiteColor
    )
}


@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun MainContent(
    state: Console,
    constraints: Constraints,
    onRequest: (request: @Request Int) -> Boolean,
    modifier: Modifier = Modifier,
) = ConstraintLayout(
    constraintSet = constraints.value,
    modifier = modifier,
    animateChanges = false
) {
    val gradientGreenRed = Brush.horizontalGradient(0f to lightPurple, 1000f to lightPink)
    val navController = LocalNavController.current
    var removePlayerView by remember { mutableStateOf(false) }
    val onNavigateBack: () -> Unit = onNavigateBack@{
        if (onRequest(REQUEST_HANDLE_BACK_PRESS))
            return@onNavigateBack

        removePlayerView = true
        navController.navigateUp()
    }
    BackHandler(onBack = onNavigateBack)

    Column(
        modifier = Modifier
            .layoutId(Constraints.ID_BACKGROUND)
            .fillMaxSize()
            .background(brush = gradientGreenRed)
            .padding(start = 16.dp, end = 16.dp),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier
                .padding(top = 25.dp)
                .fillMaxWidth()
                .padding(top = 25.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Close Button
            OutlinedButton2(
                onClick = onNavigateBack,
                modifier = Modifier
                    .size(45.dp)
                    .layoutId(Constraints.ID_CLOSE_BTN),
                shape = CircleShape,
                colors = ButtonDefaults.textButtonColors(
                    backgroundColor = lightWhitePink.copy(alpha = 0.3f)
                ),
                contentPadding = PaddingValues(vertical = 10.dp),
                content = {
                    Image(
                        painter = painterResource(id = R.drawable.ic_back_black),
                        contentDescription = "",
                        colorFilter = ColorFilter.tint(blackColor),
                        modifier = Modifier
                            .size(55.dp)
                    )
                },
            )
            Text(
                style = fontSemiBold.copy(color = whiteColor, fontSize = 25.sp),
                text = "Now Playing"
            )
            Box(
                modifier = Modifier
                    .size(45.dp)
            ) {}
        }

        // Artwork
        Box(
            modifier = Modifier
                .size(280.dp)
                .border(width = 2.dp, color = lightWhitePurple, shape = CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Artwork(
                data = state.artworkUri,
                modifier = Modifier
                    .size(250.dp)
                    .layoutId(Constraints.ID_ARTWORK)
                    .clip(shape = CircleShape)
            )
        }

        Column(
            modifier = Modifier
                .padding(top = 20.dp)
                .fillMaxWidth()
                .height(70.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = state.title ?: stringResource(id = R.string.unknown),
                style = fontSemiBold.copy(color = whiteColor, fontSize = 30.sp),
                modifier = Modifier
                    .marque(Int.MAX_VALUE)
                    .layoutId(Constraints.ID_TITLE),
            )
            // Subtitle
            Text(
                text = state.subtitle ?: stringResource(id = R.string.unknown),
                style = fontMedium.copy(color = whiteColor, fontSize = 20.sp),
                modifier = Modifier
                    .layoutId(Constraints.ID_SUBTITLE),
                textAlign = TextAlign.Center
            )
        }

        // ProgressRow
        TimeBar(
            state = state,
            style = SEEKBAR_STYLE_WAVY,
            modifier = Modifier
                .layoutId(Constraints.ID_TIME_BAR)
                .offset(y = -10.dp),
        )

        // Controls
        ControlsBottomBar(
            state = state,
            style = PLAY_BUTTON_STYLE_NEUMORPHIC,
            modifier = Modifier
                .layoutId(Constraints.ID_CONTROLS)
                .offset(y = -30.dp)
        )

        // Options
        OtherOptions(
            state = state,
            modifier = Modifier
                .fillMaxWidth()
                .layoutId(Constraints.ID_OPTIONS)
                .offset(y = -50.dp),
        )
    }
}

private val WindowSize.isMobilePortrait
    get() = widthRange == Range.Compact && widthRange < heightRange

private val minimumWindowSizeForDetails = DpSize(600.dp, 550.dp)

private const val DETAILS_OF_NONE = -1

@Composable
fun Console(state: Console) {
    val windowSize by rememberUpdatedState(newValue = LocalWindowSize.current)
    var detailsOf by remember { mutableIntStateOf(DETAILS_OF_NONE) }
    Log.d(TAG, "windowSize: ${windowSize.value}")
    val view = LocalView.current
    val isInInspectionMode = LocalInspectionMode.current
    val controller = if (!isInInspectionMode)
        WindowCompat.getInsetsController((view.context as Activity).window, view)
    else
        null
    val onRequest = onRequest@{ request: Int ->
        if (request == detailsOf) {
            detailsOf = DETAILS_OF_NONE
            return@onRequest true
        }

        val isDetailsRequest =
            request == REQUEST_SHOW_PROPERTIES || request == REQUEST_SHOW_PLAYING_QUEUE
        val size = windowSize.value
        if (isDetailsRequest && minimumWindowSizeForDetails.contains(size)) {
            Log.d(TAG, "Window is too small to display details: ${windowSize.value}")
            return@onRequest false
        }

        when (request) {
            REQUEST_HANDLE_BACK_PRESS -> {
                if (state.visibility == Console.VISIBILITY_LOCKED) {
                    state.message = "\uD83D\uDD12 Long click to unlock."
                    return@onRequest true
                }
                if (detailsOf == DETAILS_OF_NONE) false
                else {
                    detailsOf = DETAILS_OF_NONE
                    true
                }
            }

            REQUEST_SHOW_PLAYING_QUEUE -> {
                detailsOf = request
                return@onRequest true
            }

            else -> {
                error("Unsupported request: $request")
            }
        }
    }

    val isInTwoPaneMode = detailsOf != DETAILS_OF_NONE

    val content = remember {
        movableContentOf {
            val showController =
                state.visibility == Console.VISIBILITY_VISIBLE || state.visibility == Console.VISIBILITY_ALWAYS
            val insets = (if (windowSize.isMobilePortrait && detailsOf != DETAILS_OF_NONE)
                WindowInsets.statusBars
            else WindowInsets.systemBars)
                .asDpRect
            BoxWithConstraints {
                val newWindowSize = WindowSize(DpSize(maxWidth, maxHeight))
                val constraints = remember(newWindowSize, showController, insets) {
                    calculateConstraintSet(newWindowSize, insets)
                }
                SideEffect { controller?.immersiveMode(!showController) }
                MainContent(
                    state = state,
                    constraints = constraints,
                    onRequest = onRequest,
                    modifier = Modifier
                        .fillMaxSize()
                )
            }
        }
    }
    val owner = LocalLifecycleOwner.current
    DisposableEffect(key1 = owner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE && state.isVideo) {
                state.isPlaying = false
            }
        }
        owner.lifecycle.addObserver(observer)
        onDispose {
            owner.lifecycle.removeObserver(observer)
        }
    }
    if (!isInTwoPaneMode)
        return content()
}
