package com.example.myaudiolibrary.console

import androidx.compose.animation.core.LinearEasing
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import com.example.myaudiolibrary.MainActivity
import com.example.myaudiolibrary.R
import com.example.myaudiolibrary.core.Anim
import com.example.myaudiolibrary.core.MediumDurationMills
import com.example.myaudiolibrary.core.compose.Artwork
import com.example.myaudiolibrary.core.compose.LocalNavController
import com.example.myaudiolibrary.core.compose.LottieAnimButton
import com.example.myaudiolibrary.core.playback.RemoteInterface
import com.example.myaudiolibrary.core.playback.artworkUri
import com.example.myaudiolibrary.core.playback.subtitle
import com.example.myaudiolibrary.core.playback.title
import com.example.myaudiolibrary.impl.progress
import com.example.myaudiolibrary.ui.theme.fontRegular
import com.example.myaudiolibrary.ui.theme.fontSemiBold
import com.example.myaudiolibrary.ui.theme.lightGreenWhite
import com.example.myaudiolibrary.ui.theme.lightWhitePurple
import com.example.myaudiolibrary.ui.theme.mediumGreen
import com.example.myaudiolibrary.ui.theme.whiteColor
import com.primex.material2.Label
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private const val TAG = "MiniPlayer"

private val IN_FOCUS_EVENTS = intArrayOf(
    Player.EVENT_MEDIA_ITEM_TRANSITION,
    Player.EVENT_IS_PLAYING_CHANGED
)

private const val DRAG_STIFF_CONST = 3

@Composable
fun PopupMiniMediaPlayer() {
    val remote = (LocalView.current.context as MainActivity).remoteInterface
    val isLoaded by remote.loaded.collectAsState(initial = false)

    if (!isLoaded)
        return Spacer(modifier = Modifier)
    var item by remember { mutableStateOf(remote.current) }
    var progress by remember { mutableFloatStateOf(remote.progress) }
    var isPlaying by rememberSaveable { mutableStateOf(remote.playWhenReady) }
    LaunchedEffect(key1 = Unit) {
        var job: Job? = null
        remote.events.collect { event ->
            if (event != null && !event.containsAny(*IN_FOCUS_EVENTS))
                return@collect
            item = remote.current
            isPlaying = remote.playWhenReady
            job?.cancel()
            if (!remote.playWhenReady) return@collect
            job = launch(Dispatchers.Main) {
                while (true) {
                    progress = remote.position / remote.duration.toFloat()
                    delay(1000)
                }
            }
        }
    }

    val navController = LocalNavController.current

    if (isPlaying == remote.isPlaying || isPlaying) {
        Row(
            modifier = Modifier.clickable(
                remember { MutableInteractionSource() },
                null,
                onClick = {
                    navController.navigate(Console.direction())
                })
        ) {
            val density = LocalDensity.current
            item?.let {
                Layout(
                    value = it,
                    progress = progress,
                    isPlaying = isPlaying,
                    remoteInterface = remote,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(shape = RoundedCornerShape(15.dp))
                        .draggable(
                            orientation = Orientation.Horizontal,
                            state = rememberDraggableState() { delta ->
                                val dp = with(density) { (delta / DRAG_STIFF_CONST).toDp().value }
                                val milliseconds = (dp * 1000).roundToInt()
                                val newPositionMillis = milliseconds + remote.position
                                progress = newPositionMillis / remote.duration.toFloat()
                                remote.seekTo(newPositionMillis)
                            }
                        )
                )
            }
        }
    }
}

//mini player layout
@Composable
private fun Layout(
    value: MediaItem,
    progress: Float,
    isPlaying: Boolean,
    remoteInterface: RemoteInterface,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = Modifier
            .height(100.dp)
            .fillMaxWidth()
            .clip(shape = RoundedCornerShape(30.dp))
            .background(lightWhitePurple.copy(alpha = 0.3f))
            .padding(start = 15.dp, end = 15.dp, top = 3.dp, bottom = 1.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(0.9f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Start
                ) {
                    Artwork(
                        data = value.artworkUri,
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape),
                    )
                    Column(
                        modifier = Modifier.padding(start = 10.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Label(
                            text = value.title.toString(),
                            style = fontSemiBold.copy(color = whiteColor, fontSize = 20.sp)
                        )
                        Spacer(
                            modifier = modifier
                                .fillMaxWidth()
                                .height(5.dp)
                        )
                        Label(
                            text = value.subtitle.toString(),
                            style = fontRegular.copy(color = whiteColor, fontSize = 16.sp)
                        )
                    }
                }

                // Play Toggle
                Box(
                    modifier = Modifier
                        .padding(start = 5.dp)
                        .size(50.dp)
                        .clip(shape = CircleShape)
                        .border(width = 2.dp, shape = CircleShape, color = lightWhitePurple)
                        .background(whiteColor),
                    contentAlignment = Alignment.Center
                ) {
                    LottieAnimButton(
                        id = R.raw.lt_play_pause,
                        atEnd = !isPlaying,
                        scale = 1.5f,
                        progressRange = 0.0f..0.29f,
                        duration = Anim.MediumDurationMills,
                        easing = LinearEasing,
                        onClick = {
                            if (isPlaying) remoteInterface.pause() else remoteInterface.play(true)
                        },
                    )
                }
            }

            LinearProgressIndicator(
                progress = progress,
                modifier = Modifier
                    .offset(y = 6.dp)
                    .height(2.dp)
                    .fillMaxWidth(),
                backgroundColor = lightGreenWhite,
                color = mediumGreen
            )
        }
    }
}
