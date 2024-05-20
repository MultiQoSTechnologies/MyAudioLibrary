@file:SuppressLint("UnsafeOptInUsageError")

package com.example.myaudiolibrary.impl

import android.annotation.SuppressLint
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.ui.DefaultTrackNameProvider
import coil.imageLoader
import coil.request.ImageRequest
import com.example.myaudiolibrary.console.Console
import com.example.myaudiolibrary.console.TrackInfo
import com.example.myaudiolibrary.core.playback.Playback.Companion.UNINITIALIZED_SLEEP_TIME_MILLIS
import com.example.myaudiolibrary.core.playback.RemoteInterface
import com.example.myaudiolibrary.core.playback.artworkUri
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.math.roundToLong


private val RemoteInterface.neighbours
    get() = when {
        !hasPreviousTrack && next == null -> 0
        hasPreviousTrack && next == null -> -1
        !hasPreviousTrack && next != null -> 1
        else -> 2
    }

val RemoteInterface.progress
    get() =
        if (!isCurrentMediaItemSeekable || duration == C.TIME_UNSET || position == C.TIME_UNSET) -1f else position / duration.toFloat()

context(ViewModel)
private inline fun suspended(
    context: CoroutineContext = EmptyCoroutineContext,
    start: CoroutineStart = CoroutineStart.DEFAULT,
    noinline block: suspend CoroutineScope.() -> Unit,
) = viewModelScope.launch(context, start, block)


private fun Player?.gatherSupportedTrackInfosOfType(
    type: Int,
    provider: DefaultTrackNameProvider,
): List<TrackInfo> {
    val tracks = this?.currentTracks ?: return emptyList()
    val groups = tracks.groups
    val list = ArrayList<TrackInfo>()
    for (index in groups.indices) {
        val group = groups[index]
        if (group.type != type)
            continue
        for (trackIndex in 0 until group.length) {
            if (!group.isTrackSupported(trackIndex))
                continue
            val format = group.getTrackFormat(trackIndex)
            val name = provider.getTrackName(format)
            val params = TrackSelectionOverride(group.mediaTrackGroup, trackIndex)
            list.add(TrackInfo(name, params))
        }
    }
    return list
}

private fun Player?.select(value: TrackInfo?, type: Int) {
    val player = this ?: return
    if (!player.isCommandAvailable(Player.COMMAND_SET_TRACK_SELECTION_PARAMETERS)) return
    player.trackSelectionParameters =
        player.trackSelectionParameters
            .buildUpon()
            .apply {
                when {
                    type == C.TRACK_TYPE_TEXT && value == null -> {
                        clearOverridesOfType(C.TRACK_TYPE_TEXT)
                        setIgnoredTextSelectionFlags(C.SELECTION_FLAG_FORCED.inv())
                    }

                    type == C.TRACK_TYPE_AUDIO && value == null -> {
                        clearOverridesOfType(C.TRACK_TYPE_AUDIO)
                            .setTrackTypeDisabled(C.TRACK_TYPE_AUDIO, false)
                    }

                    value != null -> {
                        setOverrideForType(value.params)
                        setTrackTypeDisabled(value.params.type, false)
                    }

                    else -> error("Track $value & $type cannot be null or invalid")
                }
            }.build()
}

private fun Player?.getSelectedTrack(
    provider: DefaultTrackNameProvider,
    type: Int,
): TrackInfo? {
    val player = this ?: return null
    if (!player.isCommandAvailable(Player.COMMAND_SET_TRACK_SELECTION_PARAMETERS)) return null
    val groups = player.currentTracks.groups
    for (group in groups) {
        if (group.type != type) continue
        for (trackIndex in 0 until group.length) {
            if (!group.isTrackSelected(trackIndex)) continue
            val format = group.getTrackFormat(trackIndex)
            val name = provider.getTrackName(format)
            val params = TrackSelectionOverride(group.mediaTrackGroup, trackIndex)
            return TrackInfo(name, params)
        }
    }
    return null
}

@HiltViewModel
class ConsoleViewModel @Inject constructor(
    private val remoteInterface: RemoteInterface,
    private val delegate: SystemDelegate,
) : ViewModel(), Console, SystemDelegate by delegate {

    private var _progress by mutableFloatStateOf(remoteInterface.progress)
    private var _sleepAfterMills by mutableLongStateOf(UNINITIALIZED_SLEEP_TIME_MILLIS)
    private var _playbackSpeed by mutableFloatStateOf(remoteInterface.playbackSpeed)
    private var _isPlaying by mutableStateOf(remoteInterface.isPlaying)
    private var _repeatMode by mutableIntStateOf(remoteInterface.repeatMode)
    private var _shuffle by mutableStateOf(remoteInterface.shuffle)
    override var isVideo: Boolean by mutableStateOf(remoteInterface.isCurrentMediaItemVideo)
    override var resizeMode: Int by mutableIntStateOf(Console.RESIZE_MORE_FIT)
    private var _visibility: Int by mutableIntStateOf(Console.VISIBILITY_ALWAYS)
    private var _message: CharSequence? by mutableStateOf(null)

    // simple properties
    override var artwork: ImageBitmap? by mutableStateOf(null)
    override val audioSessionId get() = remoteInterface.audioSessionId
    override var neighbours by mutableIntStateOf(remoteInterface.neighbours)
    override val queue: Flow<List<MediaItem>> = remoteInterface.queue
    override var current: MediaItem? by mutableStateOf(remoteInterface.current)
    override val player: Player? get() = remoteInterface.player

    private var jobs: Array<Job?> = Array(3) { null }

    // getter setters.
    override var visibility: Int
        get() = _visibility
        set(value) {
            suspended {
                if (value == Console.VISIBILITY_LOCKED)
                    delay(50L)
                _visibility = value
                jobs[1]?.cancel()
                if (_visibility != Console.VISIBILITY_VISIBLE)
                    return@suspended
                jobs[1] = suspended {
                    delay(5_000L)
                    _visibility = Console.VISIBILITY_HIDDEN
                }
            }
        }

    override var shuffle: Boolean
        get() = _shuffle
        set(value) {
            remoteInterface.shuffle = value; _shuffle = remoteInterface.shuffle
        }

    override var progress: Float
        get() = _progress
        set(value) {
            if (_progress !in 0f..1f) return
            _progress = value
            val mills = (remoteInterface.duration * value).roundToLong()
            suspended { remoteInterface.seekTo(mills = mills) }
        }

    override var sleepAfterMills: Long
        get() = _sleepAfterMills
        set(value) {
            suspended {
                if (!isPlaying)
                    return@suspended
                _sleepAfterMills = value
                val mills =
                    if (value == UNINITIALIZED_SLEEP_TIME_MILLIS) value else System.currentTimeMillis() + value
                remoteInterface.setSleepTimeAt(mills)
            }
        }

    override var playbackSpeed: Float
        get() = _playbackSpeed
        set(value) {
            remoteInterface.playbackSpeed = value
            _playbackSpeed = remoteInterface.playbackSpeed
        }

    override var isPlaying: Boolean
        get() = _isPlaying
        set(value) {
            if (value) remoteInterface.play(true)
            else remoteInterface.pause()
        }

    override var repeatMode: Int
        get() = _repeatMode
        set(value) {
            TODO("Not yet implemented")
        }

    private val trackNameProvider by lazy {
        object : DefaultTrackNameProvider(resources) {
            override fun getTrackName(format: Format): String {
                var trackName = super.getTrackName(format)
                val label = format.label
                if (!label.isNullOrBlank() && !trackName.startsWith(label)) { // HACK
                    trackName += " - $label";
                }
                return trackName
            }
        }
    }

    override val subtiles: List<TrackInfo>
        get() = player.gatherSupportedTrackInfosOfType(C.TRACK_TYPE_TEXT, trackNameProvider)

    override var currAudioTrack: TrackInfo?
        get() = player?.getSelectedTrack(trackNameProvider, C.TRACK_TYPE_AUDIO)
        set(value) = player.select(value, C.TRACK_TYPE_AUDIO)

    override var currSubtitleTrack: TrackInfo?
        get() = player?.getSelectedTrack(trackNameProvider, C.TRACK_TYPE_TEXT)
        set(value) = player.select(value, C.TRACK_TYPE_TEXT)

    override var message: CharSequence?
        get() = _message
        set(value) {
            _message = value
            jobs[2]?.cancel()
            if (value == null) return
            jobs[2] = suspended {
                delay(1_000L)
                _message = null
            }
        }


    override fun cycleRepeatMode(): Int = remoteInterface.cycleRepeatMode()
    override fun playTrackAt(position: Int) = seek(position = position)

    override fun seek(mills: Long, position: Int) {
        if (position == -2)
            remoteInterface.skipToPrev()
        else if (position == -3)
            remoteInterface.skipToNext()
        if (mills == C.TIME_UNSET)
            return
        val newMills =
            if (remoteInterface.position != C.TIME_UNSET && remoteInterface.isCurrentMediaItemSeekable) remoteInterface.position + mills
            else
                C.TIME_UNSET
        remoteInterface.seekTo(mills = newMills)
    }

    init {
        suspended {
            remoteInterface.events
                .collect {
                    if (it == null) {
                        onPlayerEvent(event = Player.EVENT_SHUFFLE_MODE_ENABLED_CHANGED)
                        onPlayerEvent(event = Player.EVENT_REPEAT_MODE_CHANGED)
                        onPlayerEvent(Player.EVENT_MEDIA_ITEM_TRANSITION)
                        onPlayerEvent(Player.EVENT_PLAY_WHEN_READY_CHANGED)
                        onPlayerEvent(Player.EVENT_PLAY_WHEN_READY_CHANGED)
                        onPlayerEvent(Player.EVENT_IS_PLAYING_CHANGED)
                        return@collect
                    }
                    repeat(it.size()) { index ->
                        onPlayerEvent(it.get(index))
                    }
                }
        }
    }

    private fun onPlayerEvent(event: Int) {
        neighbours = remoteInterface.neighbours
        when (event) {
            Player.EVENT_SHUFFLE_MODE_ENABLED_CHANGED -> _shuffle = remoteInterface.shuffle
            Player.EVENT_REPEAT_MODE_CHANGED -> _repeatMode = remoteInterface.repeatMode
            Player.EVENT_PLAY_WHEN_READY_CHANGED -> {
                _isPlaying = remoteInterface.playWhenReady
                _sleepAfterMills = UNINITIALIZED_SLEEP_TIME_MILLIS
                jobs[0]?.cancel()
                if (!remoteInterface.playWhenReady)
                    return
                jobs[0] = suspended {
                    while (true) {
                        val scheduled = remoteInterface.getSleepTimeAt()
                        _sleepAfterMills =
                            if (scheduled == UNINITIALIZED_SLEEP_TIME_MILLIS) scheduled else scheduled - System.currentTimeMillis()
                        _progress = remoteInterface.progress
                        delay(1000)
                    }
                }
            }

            Player.EVENT_MEDIA_ITEM_TRANSITION -> {
                val mediaItem = remoteInterface.current
                current = mediaItem
                suspended {
                    val artworkUri = mediaItem?.artworkUri
                    if (artworkUri == null) {
                        artwork = null
                        return@suspended
                    }
                    artwork = context.imageLoader.execute(
                        ImageRequest.Builder(context).data(artworkUri).build()
                    ).drawable?.toBitmap()?.asImageBitmap()
                }
            }
        }
    }
}