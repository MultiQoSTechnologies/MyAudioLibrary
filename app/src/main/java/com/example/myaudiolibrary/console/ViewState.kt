package com.example.myaudiolibrary.console

import android.annotation.SuppressLint
import androidx.annotation.IntDef
import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.ImageBitmap
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.Player.RepeatMode
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.ui.AspectRatioFrameLayout
import kotlinx.coroutines.flow.Flow

data class TrackInfo(val name: String, val params: TrackSelectionOverride)

@Stable
interface PlayingQueue {
    val queue: Flow<List<MediaItem>>
    var shuffle: Boolean
    val current: MediaItem?
    val playing: Boolean
    val isLast: Boolean
    fun playTrackAt(position: Int)
    fun toggleShuffle()
}

@Stable
interface Console : PlayingQueue {
    var progress: Float
    var sleepAfterMills: Long
    var playbackSpeed: Float
    var isPlaying: Boolean
    @get:RepeatMode
    @setparam:RepeatMode
    var repeatMode: Int
    val artwork: ImageBitmap?
    val neighbours: Int
    val audioSessionId: Int
    val isVideo: Boolean
    var resizeMode: Int
    val player: Player?
    fun seek(mills: Long = C.TIME_UNSET, position: Int = C.INDEX_UNSET)

    @RepeatMode
    fun cycleRepeatMode(): Int
    val isFirst get() = neighbours != -1 && neighbours != 2
    fun skipToPrev() = seek(position = -2)
    fun skipToNext() = seek(position = -3)

    val isSeekable get() = progress in 0f..1f

    fun togglePlay() {
        isPlaying = !isPlaying
    }

    override fun toggleShuffle() {
        shuffle = !shuffle
    }

    override val isLast: Boolean get() = neighbours <= 0
    override val playing: Boolean get() = isPlaying
    var currAudioTrack: TrackInfo?

    val subtiles: List<TrackInfo>

    var currSubtitleTrack: TrackInfo?
    @get:Visibility
    @set:Visibility
    var visibility: Int
    var message: CharSequence?

    companion object {
        const val route = "route_console"

        fun direction() = route

        @SuppressLint("UnsafeOptInUsageError")
        const val RESIZE_MORE_FIT = AspectRatioFrameLayout.RESIZE_MODE_FIT

        @SuppressLint("UnsafeOptInUsageError")
        const val VISIBILITY_VISIBLE = 0
        const val VISIBILITY_ALWAYS = 2
        const val VISIBILITY_HIDDEN = 3
        const val VISIBILITY_LOCKED = 4
    }
}
@IntDef(
    Console.VISIBILITY_VISIBLE,
    Console.VISIBILITY_HIDDEN,
    Console.VISIBILITY_ALWAYS,
    Console.VISIBILITY_LOCKED
)
@Target(AnnotationTarget.PROPERTY_SETTER, AnnotationTarget.PROPERTY_GETTER)
@Retention(AnnotationRetention.SOURCE)
@MustBeDocumented
annotation class Visibility