package com.example.myaudiolibrary.core.playback

import android.media.audiofx.Equalizer
import android.net.Uri
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import kotlinx.coroutines.flow.*

interface RemoteInterface {
    var shuffle: Boolean
    val position: Long
    val duration: Long
    val isPlaying: Boolean
    val playWhenReady: Boolean
    val repeatMode: Int
    val meta: MediaMetadata?
    val current: MediaItem?
    val next: MediaItem?
    val audioSessionId: Int
    val hasPreviousTrack: Boolean
    var playbackSpeed: Float
    val queue: Flow<List<MediaItem>>
    val events: Flow<Player.Events?>
    val loaded: Flow<Boolean>
    val index: Int
    val nextIndex: Int
    val player: Player?
    val isCurrentMediaItemSeekable: Boolean
    val isCurrentMediaItemVideo: Boolean
    fun play(playWhenReady: Boolean = true)
    fun pause()
    fun togglePlay()
    fun skipToNext()
    fun skipToPrev()
    @Deprecated("use the seekTo with suspend")
    fun seekTo(mills: Long)
    suspend fun seekTo(position: Int, mills: Long)
    fun cycleRepeatMode(): Int
    @Deprecated("use seekTo")
    fun playTrackAt(position: Int)
    @Deprecated("use alternative by uri.")
    fun playTrack(id: Long)
    suspend fun seekTo(uri: Uri, mills: Long = C.TIME_UNSET): Boolean
    @Deprecated("use seek to instead.")
    fun playTrack(uri: Uri)
    @Deprecated("use the individual ones.")
    fun onRequestPlay(shuffle: Boolean, index: Int = C.INDEX_UNSET, values: List<MediaItem>)
    suspend fun set(vararg values: MediaItem): Int
    @Deprecated("use set with vararg.")
    suspend fun set(values: List<MediaItem>): Int = set(*values.toTypedArray())
    suspend fun add(vararg values: MediaItem, index: Int = -1): Int

    suspend fun toggleShuffle(): Boolean
    suspend fun indexOf(uri: Uri): Int
    suspend fun setSleepTimeAt(mills: Long)
    suspend fun getSleepTimeAt(): Long
    suspend fun setEqualizer(eq: Equalizer?)
    suspend fun getEqualizer(priority: Int): Equalizer
}