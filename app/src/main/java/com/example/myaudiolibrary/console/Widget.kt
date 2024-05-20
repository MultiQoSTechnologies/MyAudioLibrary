package com.example.myaudiolibrary.console

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.SystemClock
import android.widget.RemoteViews
import androidx.core.graphics.drawable.toBitmap
import androidx.media3.common.MediaItem
import coil.imageLoader
import coil.request.ImageRequest
import com.example.myaudiolibrary.R
import com.example.myaudiolibrary.core.playback.RemoteInterface
import com.example.myaudiolibrary.core.playback.mediaUri
import dagger.hilt.android.AndroidEntryPoint
import android.util.Log
import android.widget.Toast
import androidx.media3.common.C
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject

private inline fun PendingActivity(ctx: Context): PendingIntent {
    val intent = ctx.packageManager.getLaunchIntentForPackage(ctx.packageName)
    return PendingIntent.getActivity(ctx, 0, intent, PendingIntent.FLAG_IMMUTABLE)
}
private inline fun PendingAction(ctx: Context, action: String): PendingIntent {
    val intent = Intent(action, null, ctx, Widget::class.java)
    return PendingIntent.getBroadcast(ctx, 0, intent, PendingIntent.FLAG_IMMUTABLE)
}
private suspend inline fun <T> runOnUiThread(noinline block: suspend CoroutineScope.() -> T): T =
    withContext(Dispatchers.Main, block)

private const val ACTION_SEEK_BACK_MILLS = "action_seek_back_mills"
private const val ACTION_SEEK_NEXT = "action_seek_next"
private const val ACTION_TOGGLE = "action_toggle"

@AndroidEntryPoint
class Widget : AppWidgetProvider() {
    @Inject
    lateinit var remoteInterface: RemoteInterface
    private var current: MediaItem? = null
    private var artwork: Bitmap? = null
    private suspend fun loadArtwork(ctx: Context) {
        val old = current
        current = runOnUiThread { remoteInterface.current }
        if (old?.mediaUri == current?.mediaUri)
            return
        val uri = current?.mediaMetadata?.artworkUri
        val request = ImageRequest.Builder(ctx).error(R.drawable.ic_default_music_icon)
            .placeholder(R.drawable.ic_default_music_icon).data(uri).build()
        artwork = ctx.imageLoader.execute(request).drawable?.toBitmap()
    }
    private suspend fun ensureRunning() {
        runOnUiThread { remoteInterface.loaded.first() }
    }

    private suspend fun AppWidgetManager.onUpdate(ctx: Context, id: Int) {
        // fetch_artwork maybe.
        loadArtwork(ctx)
        val view = RemoteViews(ctx.packageName, R.layout.widget_style_notification).apply {
            setOnClickPendingIntent(R.id.widget_seek_back_10, PendingAction(ctx, ACTION_SEEK_BACK_MILLS))
            setOnClickPendingIntent(R.id.widget_play_toggle, PendingAction(ctx, ACTION_TOGGLE))
            setOnClickPendingIntent(R.id.skip_to_next, PendingAction(ctx, ACTION_SEEK_NEXT))
            setOnClickPendingIntent(R.id.widget, PendingActivity(ctx))
            val isPLaying = runOnUiThread { remoteInterface.isPlaying }
            setImageViewResource(
                R.id.widget_play_toggle,
                if (isPLaying) R.drawable.media3_notification_pause else R.drawable.media3_notification_play
            )
            val elapsed: Long = SystemClock.elapsedRealtime() - runOnUiThread { remoteInterface.position }
            setChronometer(R.id.widget_chronometer, elapsed, "%tH%tM:%tS", isPLaying)
            setImageViewBitmap(R.id.widget_artwork, artwork)
            setTextViewText(R.id.widget_title, current?.mediaMetadata?.title)
            setTextViewText(R.id.widget_subtitle, current?.mediaMetadata?.subtitle)
        }
        Log.d("TAG", "update $id")
        updateAppWidget(id, view)
    }

    override fun onUpdate(ctx: Context, mgr: AppWidgetManager, Ids: IntArray) {
        GlobalScope.launch {
            ensureRunning()
            for (id in Ids) mgr.onUpdate(ctx, id)
        }
    }

    override fun onEnabled(ctx: Context) {
        super.onEnabled(ctx)
        Toast.makeText(ctx, "Use the handles on the corners to resize the widget.", Toast.LENGTH_SHORT).show()
    }

    private suspend fun onAction(action: String){
        when(action){
            ACTION_SEEK_NEXT -> remoteInterface.skipToNext()
            ACTION_TOGGLE -> remoteInterface.togglePlay()
            ACTION_SEEK_BACK_MILLS -> {
                val position = remoteInterface.position
                val duration = remoteInterface.duration
                if (position == C.TIME_UNSET || duration == C.TIME_UNSET)
                    return
                val mills = position + TimeUnit.SECONDS.toMillis(10)
                remoteInterface.seekTo(mills.coerceIn(0, duration))
            }
        }
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        super.onReceive(context, intent)
        val action = intent?.action ?: return
        GlobalScope.launch {
            ensureRunning()
            runOnUiThread { onAction(action) }
        }
    }
}