package com.example.myaudiolibrary.impl

import android.content.ComponentName
import android.content.Context
import android.media.AudioManager
import android.media.audiofx.Equalizer
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaBrowser
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.SessionToken
import com.example.myaudiolibrary.core.playback.Playback
import com.example.myaudiolibrary.core.playback.RemoteInterface
import com.example.myaudiolibrary.core.playback.mediaUri
import com.example.myaudiolibrary.core.util.await
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import androidx.media3.session.SessionCommand as Command

private const val TAG = "Remote"

private inline fun Command(action: String, args: Bundle.() -> Unit = { Bundle.EMPTY }) =
    Command(action, Bundle().apply(args))

private val MediaBrowser.nextMediaItem
    get() = if (hasNextMediaItem()) getMediaItemAt(nextMediaItemIndex) else null

fun Remote(context: Context): RemoteInterface = RemoteImpl(context)

private fun Context.browser(listener: MediaBrowser.Listener) =
    MediaBrowser
        .Builder(this, SessionToken(this, ComponentName(this, Playback::class.java)))
        .setListener(listener)
        .buildAsync()


private class RemoteImpl(val context: Context) : RemoteInterface, MediaBrowser.Listener {
    private val channel = MutableSharedFlow<String>()

    override fun onChildrenChanged(
        browser: MediaBrowser,
        parentId: String,
        itemCount: Int,
        params: MediaLibraryService.LibraryParams?,
    ) {
        GlobalScope.launch { channel.emit(parentId) }
    }

    private var fBrowser: ListenableFuture<MediaBrowser> = context.browser(this)
        get() {
            val value =
                if (field.isCancelled) context.browser(this)
                else field
            field = value
            return value
        }

    val browser get() = if (fBrowser.isDone) fBrowser.get() else null

    override val position
        get() = browser?.currentPosition ?: C.TIME_UNSET
    override val duration
        get() = browser?.duration ?: C.TIME_UNSET
    override val repeatMode: Int
        get() = browser?.repeatMode ?: Player.REPEAT_MODE_OFF
    override val meta: MediaMetadata?
        get() = browser?.mediaMetadata
    override val current: MediaItem?
        get() = browser?.currentMediaItem
    override val next: MediaItem?
        get() = browser?.nextMediaItem
    override val index: Int
        get() = browser?.currentMediaItemIndex ?: C.INDEX_UNSET
    override val nextIndex: Int
        get() = browser?.nextMediaItemIndex ?: C.INDEX_UNSET
    override var shuffle: Boolean
        get() = browser?.shuffleModeEnabled ?: false
        set(value) {
            browser?.shuffleModeEnabled = value
        }

    override val isPlaying: Boolean get() = browser?.isPlaying ?: false
    override val playWhenReady: Boolean get() = browser?.playWhenReady ?: false

    override var audioSessionId: Int = AudioManager.AUDIO_SESSION_ID_GENERATE
    override val hasPreviousTrack: Boolean get() = browser?.hasPreviousMediaItem() ?: false

    override var playbackSpeed: Float
        get() = browser?.playbackParameters?.speed ?: 1f
        set(value) {
            browser?.setPlaybackSpeed(value)
        }

    override val isCurrentMediaItemSeekable: Boolean
        get() = browser?.isCurrentMediaItemSeekable ?: false
    override val player: Player?
        get() = browser
    override val isCurrentMediaItemVideo: Boolean
        get() = player?.currentTracks?.groups?.find { it.type == C.TRACK_TYPE_VIDEO } != null


    override val events: Flow<Player.Events?> =
        callbackFlow {
            val observer = object : Player.Listener {
                override fun onEvents(player: Player, events: Player.Events) {
                    trySend(events)
                }
            }
            val browser = fBrowser.await()
            trySend(null)
            browser.addListener(observer)
            awaitClose {
                browser.removeListener(observer)
            }
        }
            .flowOn(Dispatchers.Main)

    override val loaded: Flow<Boolean> = events.map { current != null }

    @OptIn(FlowPreview::class)
    override val queue: Flow<List<MediaItem>> = channel
        .onStart { emit(Playback.ROOT_QUEUE) }
        .filter { it == Playback.ROOT_QUEUE }
        .debounce(500)
        .map { parent ->
            browser?.getChildren(parent, 0, Int.MAX_VALUE, null)?.await()?.value ?: emptyList()
        }

    init {
        GlobalScope.launch(Dispatchers.Main) {
            delay(3_000)
            val browser = fBrowser.await()
            browser.subscribe(Playback.ROOT_QUEUE, null)
            audioSessionId = getAudioSessionID()
            Log.d(TAG, "Audio Session ID: $audioSessionId")
        }
    }

    override fun skipToNext() {
        val browser = browser ?: return
        browser.seekToNextMediaItem()
    }

    override fun skipToPrev() {
        val browser = browser ?: return
        browser.seekToPreviousMediaItem()
    }

    override fun togglePlay() {
        val browser = browser ?: return
        if (browser.isPlaying) {
            browser.pause()
            return
        }
        browser.prepare()
        browser.playWhenReady = true
    }

    override fun cycleRepeatMode(): Int {
        val browser = browser ?: return Player.REPEAT_MODE_OFF // correct it
        val mode = browser.repeatMode
        browser.repeatMode = when (mode) {
            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_OFF
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ONE
            Player.REPEAT_MODE_ONE -> Player.REPEAT_MODE_ALL
            else -> error("repeat mode $mode")
        }
        return browser.repeatMode
    }

    override fun seekTo(mills: Long) {
        val browser = browser ?: return
        browser.seekTo(mills)
    }

    @Deprecated("use seekTo")
    override fun playTrackAt(position: Int) {
        val browser = browser ?: return
        browser.seekTo(position, C.TIME_UNSET)
    }

    @Deprecated("use the individual ones.")
    override fun onRequestPlay(shuffle: Boolean, index: Int, values: List<MediaItem>) {
        val browser = browser ?: return
        val l = ArrayList(values.distinctBy { it.mediaUri })
        val item = l.removeAt(index)
        l.add(0, item)
        browser.shuffleModeEnabled = shuffle
        browser.setMediaItems(l)
        browser.seekTo(0, C.TIME_UNSET)
        browser.prepare()
        browser.play()
    }

    @Deprecated("use alternative by uri.")
    override fun playTrack(id: Long) {
        val browser = browser ?: return
        repeat(browser.mediaItemCount) { pos ->
            val item = browser.getMediaItemAt(pos)
            if (item.mediaId == "$id") playTrackAt(pos)
        }
    }

    @Deprecated("use seek to instead.")
    override fun playTrack(uri: Uri) {
        val browser = browser ?: return
        repeat(browser.mediaItemCount) { pos ->
            val item = browser.getMediaItemAt(pos)
            if (item.requestMetadata.mediaUri == uri) playTrackAt(pos)
        }
    }

    override fun play(playWhenReady: Boolean) {
        val browser = browser ?: return
        browser.playWhenReady = playWhenReady
        browser.play()
    }

    override fun pause() {
        val browser = browser ?: return
        browser.pause()
    }

    override suspend fun set(vararg values: MediaItem): Int {
        val browser = fBrowser.await()
        val list = values.distinctBy { it.mediaUri }
        browser.setMediaItems(list)
        return list.size
    }

    private suspend fun map(index: Int): Int {
        val browser = fBrowser.await()
        if (!browser.shuffleModeEnabled)
            return index
        return index
    }

    override suspend fun add(vararg values: MediaItem, index: Int): Int {
        if (values.isEmpty())
            return 0
        val browser = fBrowser.await()
        if (browser.mediaItemCount == 0)
            return set(*values)
        val unique = values.distinctBy { it.mediaUri }.toMutableList()
        repeat(browser.mediaItemCount) {
            val item = browser.getMediaItemAt(it)
            unique.removeAll { it.mediaUri == item.mediaUri }
        }
        if (unique.isEmpty())
            return 0
        val newIndex = if (index == C.INDEX_UNSET) browser.mediaItemCount else map(index)
        browser.addMediaItems(newIndex.coerceIn(0, browser.mediaItemCount), unique)
        return unique.size
    }

    override suspend fun seekTo(position: Int, mills: Long) {
        val browser = fBrowser.await()
        browser.seekTo(position, mills)
    }

    override suspend fun seekTo(uri: Uri, mills: Long): Boolean {
        val index = indexOf(uri)
        if (index == C.INDEX_UNSET)
            return false
        seekTo(index, mills)
        return true
    }

    override suspend fun toggleShuffle(): Boolean {
        val browser = fBrowser.await()
        browser.shuffleModeEnabled = !browser.shuffleModeEnabled
        return browser.shuffleModeEnabled
    }

    override suspend fun indexOf(uri: Uri): Int {
        val browser = fBrowser.await()
        repeat(browser.mediaItemCount) { pos ->
            val item = browser.getMediaItemAt(pos)
            if (item.requestMetadata.mediaUri == uri) return pos
        }
        return C.INDEX_UNSET
    }

    suspend fun getAudioSessionID(): Int {
        val browser = fBrowser.await()
        val result = browser.sendCustomCommand(
            Command(Playback.ACTION_AUDIO_SESSION_ID, Bundle.EMPTY),
            Bundle.EMPTY
        )
        val extras = result.await().extras
        return extras.getInt(
            Playback.EXTRA_AUDIO_SESSION_ID,
            AudioManager.AUDIO_SESSION_ID_GENERATE
        )
    }

    override suspend fun setSleepTimeAt(mills: Long) {
        val browser = fBrowser.await()
        browser.sendCustomCommand(
            Command(Playback.ACTION_SCHEDULE_SLEEP_TIME) {
                putLong(Playback.EXTRA_SCHEDULED_TIME_MILLS, mills)
            },
            Bundle.EMPTY
        )
    }

    override suspend fun getSleepTimeAt(): Long {
        val browser = fBrowser.await()
        val result = browser.sendCustomCommand(
            Command(Playback.ACTION_SCHEDULE_SLEEP_TIME, Bundle.EMPTY),
            Bundle.EMPTY
        )
        return result.await().extras.getLong(Playback.EXTRA_SCHEDULED_TIME_MILLS)
    }

    override suspend fun setEqualizer(eq: Equalizer?) {
        val enabled = eq?.enabled ?: false
        val properties = eq?.properties?.toString()
        eq?.release()
        val browser = fBrowser.await()
        browser.sendCustomCommand(
            Command(Playback.ACTION_EQUALIZER_CONFIG) {
                putBoolean(Playback.EXTRA_EQUALIZER_ENABLED, enabled)
                putString(Playback.EXTRA_EQUALIZER_PROPERTIES, properties)
            },
            Bundle.EMPTY
        )
    }

    override suspend fun getEqualizer(priority: Int): Equalizer {
        val browser = fBrowser.await()
        val args =
            browser.sendCustomCommand(Command(Playback.ACTION_EQUALIZER_CONFIG), Bundle.EMPTY)
                .await()
        return Equalizer(
            priority,
            audioSessionId
        ).apply {
            enabled = args.extras.getBoolean(Playback.EXTRA_EQUALIZER_ENABLED)
            val properties = args.extras.getString(Playback.EXTRA_EQUALIZER_PROPERTIES)
            if (properties != null)
                setProperties(Equalizer.Settings(properties))
        }
    }
}
