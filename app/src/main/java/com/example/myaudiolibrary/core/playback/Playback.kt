package com.example.myaudiolibrary.core.playback

import android.annotation.SuppressLint
import android.app.*
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.ContentResolver
import android.content.Intent
import android.media.audiofx.Equalizer
import android.media.audiofx.Equalizer.Settings
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.media3.common.*
import androidx.media3.common.C.AUDIO_CONTENT_TYPE_MUSIC
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.ShuffleOrder.DefaultShuffleOrder
import androidx.media3.session.*
import androidx.media3.session.MediaLibraryService.MediaLibrarySession.Callback
import androidx.media3.session.MediaSession.ConnectionResult
import androidx.media3.session.MediaSession.ControllerInfo
import com.example.myaudiolibrary.R
import com.example.myaudiolibrary.console.Widget
import com.example.myaudiolibrary.core.db.Playlist
import com.example.myaudiolibrary.core.db.Playlists
import com.flaviofaria.kenburnsview.BuildConfig
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.primex.preferences.*
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import org.json.JSONArray
import javax.inject.Inject
import kotlin.Boolean
import kotlin.Int
import kotlin.IntArray
import kotlin.String
import kotlin.Unit
import kotlin.apply
import kotlin.getValue
import kotlin.lazy
import kotlin.random.Random
import kotlin.runCatching
import kotlin.with
import androidx.media3.session.SessionCommand as Command
import androidx.media3.session.SessionResult as Result

private inline fun Result(code: Int, args: Bundle.() -> Unit) =
    Result(code, Bundle().apply(args))

private const val TAG = "Playback"

private val PLAYLIST_RECENT = Playlists.PRIVATE_PLAYLIST_PREFIX + "recent"

private val PLAYLIST_QUEUE = Playlists.PRIVATE_PLAYLIST_PREFIX + "queue"

private const val ROOT_QUEUE = "com.prime.player.queue"

private val BROWSER_ROOT_QUEUE =
    MediaItem.Builder()
        .setMediaId(ROOT_QUEUE)
        .setMediaMetadata(
            MediaMetadata.Builder().setIsBrowsable(true)
                .setIsPlayable(false)
                .build()
        )
        .build()

// Keys used for saving various states in SharedPreferences.
private val PREF_KEY_SHUFFLE_MODE = booleanPreferenceKey("_shuffle", false)
private val PREF_KEY_REPEAT_MODE = intPreferenceKey("_repeat_mode", Player.REPEAT_MODE_OFF)
private val PREF_KEY_INDEX = intPreferenceKey("_index", C.INDEX_UNSET)
private val PREF_KEY_BOOKMARK = longPreferenceKey("_bookmark", C.TIME_UNSET)
private val PREF_KEY_RECENT_PLAYLIST_LIMIT = intPreferenceKey("_max_recent_size", 50)
private val PREF_KEY_EQUALIZER_ENABLED = booleanPreferenceKey(TAG + "_equalizer_enabled")
private val PREF_KEY_EQUALIZER_PROPERTIES = stringPreferenceKey(TAG + "_equalizer_properties")
private val PREF_KEY_CLOSE_WHEN_REMOVED = booleanPreferenceKey(TAG + "_stop_playback_when_removed", false)

private val PREF_KEY_ORDERS = stringPreferenceKey(
    "_orders",
    IntArray(0),
    object : StringSaver<IntArray> {
        override fun restore(value: String): IntArray {
            val arr = JSONArray(value)
            return IntArray(arr.length()) {
                arr.getInt(it)
            }
        }

        override fun save(value: IntArray): String {
            val arr = JSONArray(value)
            return arr.toString()
        }
    }
)

private val PlaybackAudioAttr = AudioAttributes.Builder()
    .setContentType(AUDIO_CONTENT_TYPE_MUSIC)
    .setUsage(C.USAGE_MEDIA)
    .build()

private const val SAVE_POSITION_DELAY_MILLS = 5_000L

private const val ACTION_AUDIO_SESSION_ID = BuildConfig.APPLICATION_ID + ".action.AUDIO_SESSION_ID"

private const val EXTRA_AUDIO_SESSION_ID = BuildConfig.APPLICATION_ID + ".extra.AUDIO_SESSION_ID"

private const val ACTION_SCHEDULE_SLEEP_TIME =
    BuildConfig.APPLICATION_ID + ".action.SCHEDULE_SLEEP_TIME"

private const val EXTRA_SCHEDULED_TIME_MILLS =
    BuildConfig.APPLICATION_ID + ".extra.AUDIO_SESSION_ID"

private const val UNINITIALIZED_SLEEP_TIME_MILLIS = -1L

private const val ACTION_EQUALIZER_CONFIG = BuildConfig.APPLICATION_ID + ".extra.EQUALIZER"

private const val EXTRA_EQUALIZER_ENABLED =
    BuildConfig.APPLICATION_ID + ".extra.EXTRA_EQUALIZER_ENABLED"

private const val EXTRA_EQUALIZER_PROPERTIES =
    BuildConfig.APPLICATION_ID + ".extra.EXTRA_EQUALIZER_PROPERTIES"

private val Uri.isThirdPartyUri get() = scheme == ContentResolver.SCHEME_CONTENT && authority != MediaStore.AUTHORITY
@AndroidEntryPoint
class Playback : MediaLibraryService(), Callback, Player.Listener {
    @Inject
    lateinit var preferences: Preferences

    @Inject
    lateinit var playlists: Playlists
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var playbackMonitorJob: Job? = null
    private var scheduledPauseTimeMillis = UNINITIALIZED_SLEEP_TIME_MILLIS
    private val activity by lazy {
        val intent = packageManager.getLaunchIntentForPackage(packageName)
        PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)
    }
    private val player: Player by lazy {
        ExoPlayer.Builder(this)
            .setAudioAttributes(PlaybackAudioAttr, true)
            .setHandleAudioBecomingNoisy(true).build()
    }

    private val session: MediaLibrarySession by lazy {
        MediaLibrarySession.Builder(this, player, this)
            .setSessionActivity(activity)
            .build()
    }

    private var equalizer: Equalizer? = null

    @SuppressLint("UnsafeOptInUsageError")
    override fun onCreate() {
        super.onCreate()
        scope.launch {
            runCatching { onRestoreSavedState() }
            player.addListener(this@Playback)

            onAudioSessionIdChanged(-1)
        }
    }
    @SuppressLint("UnsafeOptInUsageError")
    private suspend fun onRestoreSavedState() {
        with(player) {
            shuffleModeEnabled = preferences.value(PREF_KEY_SHUFFLE_MODE)
            repeatMode = preferences.value(PREF_KEY_REPEAT_MODE)

            val items = withContext(Dispatchers.IO) {
                playlists.getMembers(PLAYLIST_QUEUE).map(Playlist.Member::toMediaSource)
            }
            setMediaItems(items)

            (this as ExoPlayer).setShuffleOrder(
                DefaultShuffleOrder(preferences.value(PREF_KEY_ORDERS), Random.nextLong())
            )

            val index = preferences.value(PREF_KEY_INDEX)
            if (index != C.INDEX_UNSET) {
                seekTo(index, preferences.value(PREF_KEY_BOOKMARK))
                if (currentMediaItem?.mediaUri?.isThirdPartyUri == true)
                    player.removeMediaItem(index)
            }
        }
    }
    override fun onGetSession(controllerInfo: ControllerInfo) = session
    override fun onAddMediaItems(
        mediaSession: MediaSession,
        controller: ControllerInfo,
        mediaItems: MutableList<MediaItem>
    ): ListenableFuture<List<MediaItem>> =
        Futures.immediateFuture(mediaItems.map(MediaItem::toMediaSource))
    @SuppressLint("UnsafeOptInUsageError")
    override fun onGetLibraryRoot(
        session: MediaLibrarySession, browser: ControllerInfo, params: LibraryParams?
    ): ListenableFuture<LibraryResult<MediaItem>> =
        Futures.immediateFuture(LibraryResult.ofItem(BROWSER_ROOT_QUEUE, params))

    override fun onGetItem(
        session: MediaLibrarySession, browser: ControllerInfo, mediaId: String
    ): ListenableFuture<LibraryResult<MediaItem>> {
        val item = player.mediaItems.find { it.mediaId == mediaId }
        val result = if (item == null) LibraryResult.ofError(LibraryResult.RESULT_ERROR_BAD_VALUE)
        else LibraryResult.ofItem(item, /* params = */ null)
        return Futures.immediateFuture(result)
    }

    override fun onSubscribe(
        session: MediaLibrarySession,
        browser: ControllerInfo,
        parentId: String,
        params: LibraryParams?
    ): ListenableFuture<LibraryResult<Void>> {
        val children = when (parentId) {
            ROOT_QUEUE -> player.queue
            else -> return Futures.immediateFuture(
                LibraryResult.ofError(LibraryResult.RESULT_ERROR_BAD_VALUE)
            )
        }
        session.notifyChildrenChanged(browser, parentId, children.size, params)
        return Futures.immediateFuture(LibraryResult.ofVoid())
    }
    override fun onGetChildren(
        session: MediaLibrarySession,
        browser: ControllerInfo,
        parentId: String,
        page: Int,
        pageSize: Int,
        params: LibraryParams?
    ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> {
        val children = when (parentId) {
            ROOT_QUEUE -> player.queue
            else -> return Futures.immediateFuture(
                LibraryResult.ofError(LibraryResult.RESULT_ERROR_BAD_VALUE)
            )
        }
        return Futures.immediateFuture(LibraryResult.ofItemList(children, params))
    }
    override fun onMediaItemTransition(
        mediaItem: MediaItem?,
        reason: Int
    ) {
        preferences[PREF_KEY_INDEX] = player.currentMediaItemIndex
        if (mediaItem != null && mediaItem.mediaUri?.isThirdPartyUri == false) {
            val limit = preferences.value(PREF_KEY_RECENT_PLAYLIST_LIMIT)
            scope.launch(Dispatchers.IO) { playlists.addToRecent(mediaItem, limit.toLong()) }
            session.notifyChildrenChanged(ROOT_QUEUE, 0, null)
        }
    }
    override fun onShuffleModeEnabledChanged(
        shuffleModeEnabled: Boolean
    ) {
        preferences[PREF_KEY_SHUFFLE_MODE] = shuffleModeEnabled
        session.notifyChildrenChanged(ROOT_QUEUE, 0, null)
    }
    override fun onRepeatModeChanged(
        repeatMode: Int
    ) {
        preferences[PREF_KEY_REPEAT_MODE] = repeatMode
    }
    override fun onTimelineChanged(
        timeline: Timeline,
        reason: Int
    ) {
        if (reason == Player.TIMELINE_CHANGE_REASON_PLAYLIST_CHANGED) {
            val items = player.mediaItems
            scope.launch(Dispatchers.IO) { playlists.save(items) }
            preferences[PREF_KEY_ORDERS] = player.orders
            session.notifyChildrenChanged(ROOT_QUEUE, 0, null)
        }
    }
    override fun onUpdateNotification(
        session: MediaSession,
        startInForegroundRequired: Boolean
    ) {
        super.onUpdateNotification(session, startInForegroundRequired)
        val intent = Intent(this, Widget::class.java)
        intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE)

        val ids = AppWidgetManager.getInstance(application)
            .getAppWidgetIds(ComponentName(application, Widget::class.java))
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)

        sendBroadcast(intent)
    }
    override fun onPlayerError(
        error: PlaybackException
    ) {
        Toast.makeText(this, getString(R.string.msg_unplayable_file), Toast.LENGTH_SHORT).show()

        player.seekToNextMediaItem()
    }

    override fun onPlayWhenReadyChanged(isPlaying: Boolean, reason: Int) {
        super.onPlayWhenReadyChanged(isPlaying, reason)
        if (!isPlaying) {
            playbackMonitorJob?.cancel()
            scheduledPauseTimeMillis = UNINITIALIZED_SLEEP_TIME_MILLIS
        }
        else playbackMonitorJob = scope.launch {
            var isPlaying = player.playWhenReady
            do {
                preferences[PREF_KEY_BOOKMARK] = player.currentPosition
                Log.i(TAG, "Saved playback position: ${player.currentPosition}")

                if (scheduledPauseTimeMillis != UNINITIALIZED_SLEEP_TIME_MILLIS && scheduledPauseTimeMillis <= System.currentTimeMillis()) {
                    player.pause()

                    scheduledPauseTimeMillis = UNINITIALIZED_SLEEP_TIME_MILLIS
                }
                delay(SAVE_POSITION_DELAY_MILLS)
                isPlaying = player.isPlaying
            } while (isPlaying)
        }
    }
    @androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
    override fun onConnect(
        session: MediaSession,
        controller: ControllerInfo
    ): ConnectionResult {
        val available = ConnectionResult.DEFAULT_SESSION_AND_LIBRARY_COMMANDS.buildUpon()
        available.add(Command(ACTION_AUDIO_SESSION_ID, Bundle.EMPTY))
        available.add(Command(ACTION_SCHEDULE_SLEEP_TIME, Bundle.EMPTY))
        available.add(Command(ACTION_EQUALIZER_CONFIG, Bundle.EMPTY))

        // return the result.
        return ConnectionResult.AcceptedResultBuilder(session)
            .setAvailableSessionCommands(available.build())
            .build()
    }
    @SuppressLint("UnsafeOptInUsageError")
    override fun onAudioSessionIdChanged(audioSessionId: Int) {
        if (audioSessionId != -1)
            super.onAudioSessionIdChanged(audioSessionId)
        if (equalizer == null || audioSessionId != -1) {
            equalizer?.release()
            // TODO: Find the real reason why equalizer is not init when calling from onCreate.
            equalizer = com.primex.core.runCatching(TAG){
                Equalizer(0, (player as ExoPlayer).audioSessionId)
            }
        }
        equalizer?.enabled = preferences.value(PREF_KEY_EQUALIZER_ENABLED) ?: false
        val properties = preferences.value(PREF_KEY_EQUALIZER_PROPERTIES)
        if (!properties.isNullOrBlank()) {
            equalizer?.properties = Settings(properties)
        }
    }

    @androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
    override fun onCustomCommand(
        session: MediaSession,
        controller: ControllerInfo,
        command: Command,
        args: Bundle
    ): ListenableFuture<Result> {
        val action = command.customAction
        return when (action) {
            ACTION_AUDIO_SESSION_ID -> {
                val audioSessionId = (player as ExoPlayer).audioSessionId
                val result = Result(Result.RESULT_SUCCESS) {
                    putInt(EXTRA_AUDIO_SESSION_ID, audioSessionId)
                }

                Futures.immediateFuture(result)
            }

            ACTION_SCHEDULE_SLEEP_TIME -> {
                val newTimeMills = command.customExtras.getLong(EXTRA_SCHEDULED_TIME_MILLS)
                if (newTimeMills != 0L)
                    scheduledPauseTimeMillis = newTimeMills
                Futures.immediateFuture(
                    Result(Result.RESULT_SUCCESS) {
                        putLong(
                            EXTRA_SCHEDULED_TIME_MILLS,
                            scheduledPauseTimeMillis
                        )
                    }
                )
            }

            ACTION_EQUALIZER_CONFIG -> {
                val extras = command.customExtras

                if (!extras.isEmpty) {
                    val isEqualizerEnabled =
                        command.customExtras.getBoolean(EXTRA_EQUALIZER_ENABLED)
                    val properties = command.customExtras.getString(
                        EXTRA_EQUALIZER_PROPERTIES, null
                    )
                    preferences[PREF_KEY_EQUALIZER_PROPERTIES] = properties
                    preferences[PREF_KEY_EQUALIZER_ENABLED] = isEqualizerEnabled
                    onAudioSessionIdChanged(-1)
                }

                Futures.immediateFuture(
                    Result(Result.RESULT_SUCCESS) {
                        putBoolean(
                            EXTRA_EQUALIZER_ENABLED,
                            preferences.value(PREF_KEY_EQUALIZER_ENABLED) ?: false
                        )
                        putString(
                            EXTRA_EQUALIZER_PROPERTIES,
                            preferences.value(PREF_KEY_EQUALIZER_PROPERTIES)
                        )
                    }
                )
            }
            else -> Futures.immediateFuture(Result(Result.RESULT_ERROR_UNKNOWN))
        }
    }
    override fun onDestroy() {
        player.release()
        session.release()
        equalizer?.release()
        scope.cancel()
        super.onDestroy()
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        if (preferences.value(PREF_KEY_CLOSE_WHEN_REMOVED)){
            player.playWhenReady = false
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }
    companion object {
        @JvmField
        val PLAYLIST_RECENT = com.example.myaudiolibrary.core.playback.PLAYLIST_RECENT
        @JvmField
        val PLAYLIST_QUEUE = com.example.myaudiolibrary.core.playback.PLAYLIST_QUEUE
        const val ROOT_QUEUE = com.example.myaudiolibrary.core.playback.ROOT_QUEUE
        @JvmField
        val PREF_KEY_RECENT_PLAYLIST_LIMIT =
            com.example.myaudiolibrary.core.playback.PREF_KEY_RECENT_PLAYLIST_LIMIT
        @JvmField
        val PREF_KEY_CLOSE_WHEN_REMOVED =
            com.example.myaudiolibrary.core.playback.PREF_KEY_CLOSE_WHEN_REMOVED
        const val ACTION_AUDIO_SESSION_ID =
            com.example.myaudiolibrary.core.playback.ACTION_AUDIO_SESSION_ID
        const val EXTRA_AUDIO_SESSION_ID =
            com.example.myaudiolibrary.core.playback.EXTRA_AUDIO_SESSION_ID
        const val ACTION_SCHEDULE_SLEEP_TIME =
            com.example.myaudiolibrary.core.playback.ACTION_SCHEDULE_SLEEP_TIME
        const val EXTRA_SCHEDULED_TIME_MILLS =
            com.example.myaudiolibrary.core.playback.EXTRA_SCHEDULED_TIME_MILLS
        const val UNINITIALIZED_SLEEP_TIME_MILLIS =
            com.example.myaudiolibrary.core.playback.UNINITIALIZED_SLEEP_TIME_MILLIS
        const val ACTION_EQUALIZER_CONFIG =
            com.example.myaudiolibrary.core.playback.ACTION_EQUALIZER_CONFIG
        const val EXTRA_EQUALIZER_ENABLED =
            com.example.myaudiolibrary.core.playback.EXTRA_EQUALIZER_ENABLED
        const val EXTRA_EQUALIZER_PROPERTIES =
            com.example.myaudiolibrary.core.playback.EXTRA_EQUALIZER_PROPERTIES
    }
}