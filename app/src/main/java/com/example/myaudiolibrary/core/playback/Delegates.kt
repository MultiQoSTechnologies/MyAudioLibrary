package com.example.myaudiolibrary.core.playback

import android.content.Context
import android.graphics.Typeface
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.StyleSpan
import androidx.annotation.OptIn
import androidx.documentfile.provider.DocumentFile
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata.*
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.ShuffleOrder.DefaultShuffleOrder
import com.example.myaudiolibrary.R
import com.example.myaudiolibrary.core.db.Playlist
import com.example.myaudiolibrary.core.db.Playlists
import com.example.myaudiolibrary.core.util.Member
import java.io.File
import java.io.FileOutputStream

private const val TAG = "Delegates"

fun MediaSource(
    uri: Uri,
    title: CharSequence,
    subtitle: CharSequence,
    artwork: Uri? = null,
) = MediaItem.Builder()
    .setMediaId("no_empty")
    .setUri(uri)
    .setRequestMetadata(MediaItem.RequestMetadata.Builder().setMediaUri(uri).build())
    .setMediaMetadata(
        Builder().setIsBrowsable(false)
            .setIsPlayable(true)
            .setTitle(title)
            .setArtist(subtitle)
            .setArtworkUri(artwork)
            .setSubtitle(subtitle)
            .build()
    )
    .build()

private fun Bold(value: CharSequence): CharSequence =
    SpannableStringBuilder(value).apply {
        setSpan(StyleSpan(Typeface.BOLD), 0, value.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
    }

val Playlist.Member.toMediaSource
    get() = MediaSource(Uri.parse(uri), Bold(title), subtitle, artwork?.let { Uri.parse(it) })

val MediaItem.toMediaSource
    get() = MediaSource(
        requestMetadata.mediaUri!!,
        Bold(mediaMetadata.title ?: ""),
        mediaMetadata.subtitle ?: "",
        mediaMetadata.artworkUri
    )

val Player.orders: IntArray
    @OptIn(androidx.media3.common.util.UnstableApi::class)
    get() {
        require(this is ExoPlayer)
        val f1 = this.javaClass.getDeclaredField("shuffleOrder")
        f1.isAccessible = true
        val order2 = f1.get(this)
        require(order2 is DefaultShuffleOrder)
        val f2 = order2.javaClass.getDeclaredField("shuffled")
        f2.isAccessible = true
        return f2.get(order2) as IntArray
    }

inline val Player.mediaItems
    get() = List(this.mediaItemCount) {
        getMediaItemAt(it)
    }

val Player.queue get() = if (!shuffleModeEnabled) mediaItems else orders.map(::getMediaItemAt)

fun MediaItem(
    uri: Uri,
    title: String,
    subtitle: String,
    id: String = "non_empty",
    artwork: Uri? = null,
) = MediaItem.Builder()
    .setMediaId(id)
    .setRequestMetadata(MediaItem.RequestMetadata.Builder().setMediaUri(uri).build())
    .setMediaMetadata(
        Builder()
            .setArtworkUri(artwork)
            .setTitle(title)
            .setSubtitle(subtitle)
            .setIsBrowsable(false)
            .setIsPlayable(true)
            .build()
    ).build()

val MediaItem.artworkUri get() = mediaMetadata.artworkUri
val MediaItem.title get() = mediaMetadata.title
val MediaItem.subtitle get() = mediaMetadata.subtitle
val MediaItem.mediaUri get() = requestMetadata.mediaUri

context(Playback)
suspend fun Playlists.addToRecent(item: MediaItem, limit: Long) {

    val playlistId =
        get(Playback.PLAYLIST_RECENT)?.id ?: insert(Playlist(name = Playback.PLAYLIST_RECENT))
    val playlist = get(playlistId)!!
    update(playlist = playlist.copy(dateModified = System.currentTimeMillis()))

    val member = get(playlistId, item.requestMetadata.mediaUri.toString())

    when (member != null) {
        true -> {
            update(member = member.copy(order = 0))
        }
        else -> {
            insert(Member(item, playlistId, 0))
        }
    }
}

context(Playback)
suspend fun Playlists.save(items: List<MediaItem>) {
    val id = get(Playback.PLAYLIST_QUEUE)?.id ?: insert(
        Playlist(name = Playback.PLAYLIST_QUEUE)
    )

    var order = 0
    val members = items.map { Member(it, id, order++) }
    insert(members)
}

private fun Context.fileName(uri: Uri): String? {
    val displayName = DocumentFile.fromSingleUri(this, uri)?.name
    if (displayName != null) {
        return displayName
    }
    val cursor = this.contentResolver.query(uri, null, null, null, null)
    cursor?.use {
        if (it.moveToFirst()) {
            val displayNameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (displayNameIndex != -1) {
                return it.getString(displayNameIndex)
            }
            val dataIndex = it.getColumnIndex(MediaStore.MediaColumns.DATA)
            if (dataIndex != -1) {
                return it.getString(dataIndex).substringAfterLast('/')
            }
        }
    }
    val fileName = File(uri.path ?: return null).name
    if (fileName.isNotEmpty()) {
        return fileName
    }
    return null
}

fun MediaItem(context: Context, uri: Uri): MediaItem {
    val retriever = com.primex.core.runCatching(TAG) {
        MediaMetadataRetriever().also { it.setDataSource(context, uri) }
    }

    val imageUri = com.primex.core.runCatching(TAG) {
        val file = File(context.cacheDir, "tmp_artwork.png")
        file.delete()
        val bytes = retriever?.embeddedPicture ?: return@runCatching null
        val fos = FileOutputStream(file)
        fos.write(bytes)
        fos.close()
        Uri.fromFile(file)
    }
    val title = retriever?.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
        ?: context.fileName(uri) ?: context.getString(R.string.unknown)
    val subtitle = retriever?.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
        ?: context.getString(R.string.unknown)
    return MediaItem(uri, title, subtitle, artwork = imageUri)
}
