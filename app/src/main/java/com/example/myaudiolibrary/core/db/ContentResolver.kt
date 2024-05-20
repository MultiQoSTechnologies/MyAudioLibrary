package com.example.myaudiolibrary.core.db

import android.content.ContentResolver
import android.content.ContentUris
import android.database.ContentObserver
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import androidx.compose.runtime.Stable
import com.example.myaudiolibrary.impl.Repository
import com.example.myaudiolibrary.core.playback.MediaItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.withContext as using


private const val TAG = "ContentResolver2"

private const val DUMMY_SELECTION = "${MediaStore.Audio.Media._ID} != 0"

suspend fun ContentResolver.query2(
    uri: Uri,
    projection: Array<String>? = null,
    selection: String = DUMMY_SELECTION,
    args: Array<String>? = null,
    order: String = MediaStore.MediaColumns._ID,
    ascending: Boolean = true,
    offset: Int = 0,
    limit: Int = Int.MAX_VALUE,
): Cursor? {
    return using(Dispatchers.Default) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // compose the args
            val args2 = Bundle().apply {
                putInt(ContentResolver.QUERY_ARG_LIMIT, limit)
                putInt(ContentResolver.QUERY_ARG_OFFSET, offset)

                putStringArray(ContentResolver.QUERY_ARG_SORT_COLUMNS, arrayOf(order))
                putInt(
                    ContentResolver.QUERY_ARG_SORT_DIRECTION,
                    if (ascending) ContentResolver.QUERY_SORT_DIRECTION_ASCENDING else ContentResolver.QUERY_SORT_DIRECTION_DESCENDING
                )
                if (args != null) putStringArray(ContentResolver.QUERY_ARG_SQL_SELECTION_ARGS, args)
                putString(ContentResolver.QUERY_ARG_SQL_SELECTION, selection)
            }
            query(uri, projection, args2, null)
        } else {
            //language=SQL
            val order2 =
                order + (if (ascending) " ASC" else " DESC") + " LIMIT $limit OFFSET $offset"
            query(uri, projection, selection, args, order2)
        }
    }
}

internal suspend inline fun <T> ContentResolver.query2(
    uri: Uri,
    projection: Array<String>? = null,
    selection: String = DUMMY_SELECTION,
    args: Array<String>? = null,
    order: String = MediaStore.MediaColumns._ID,
    ascending: Boolean = true,
    offset: Int = 0,
    limit: Int = Int.MAX_VALUE,
    transform: (Cursor) -> T,
): T? {
    return query2(uri, projection, selection, args, order, ascending, offset, limit)?.use(transform)
}

@Stable
data class Audio(
    @JvmField val id: Long,
    @JvmField val name: String,
    @JvmField val albumId: Long,
    @JvmField val data: String,
    @JvmField val album: String,
    @JvmField val artist: String,
    @JvmField val composer: String,
    @JvmField val mimeType: String,
    @JvmField val track: Int,
    @JvmField val dateAdded: Long,
    @JvmField val dateModified: Long,
    @JvmField val duration: Int,
    @JvmField val size: Long,
    @JvmField val year: Int,
)

private inline val Cursor.toAudio
    get() = Audio(
        id = getLong(0),
        name = getString(1) ?: MediaStore.UNKNOWN_STRING,
        albumId = getLong(4),
        data = getString(8),
        album = getString(3) ?: MediaStore.UNKNOWN_STRING,
        artist = getString(2) ?: MediaStore.UNKNOWN_STRING,
        composer = getString(6) ?: MediaStore.UNKNOWN_STRING,
        mimeType = getString(10) ?: "audio/*",
        track = getInt(11),
        dateAdded = getLong(5) * 1000,
        dateModified = getLong(13) * 1000,
        duration = getInt(9),
        size = getLong(12),
        year = getInt(7)
    )

private val AUDIO_PROJECTION
    get() = arrayOf(
        MediaStore.Audio.Media._ID, //0
        MediaStore.Audio.Media.TITLE, // 1
        MediaStore.Audio.Media.ARTIST, // 2
        MediaStore.Audio.Media.ALBUM, // 3
        MediaStore.Audio.Media.ALBUM_ID, // 4
        MediaStore.Audio.Media.DATE_ADDED,  //5
        MediaStore.Audio.Media.COMPOSER, // , // 6
        MediaStore.Audio.Media.YEAR, // 7
        MediaStore.Audio.Media.DATA, // 8
        MediaStore.Audio.Media.DURATION, // 9
        MediaStore.Audio.Media.MIME_TYPE, // 10
        MediaStore.Audio.Media.TRACK, // 11
        MediaStore.Audio.Media.SIZE, //12
        MediaStore.Audio.Media.DATE_MODIFIED, // 14
    )

private const val DEFAULT_AUDIO_SELECTION = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
private const val DEFAULT_AUDIO_ORDER = MediaStore.Audio.Media.TITLE

suspend fun ContentResolver.getAudios(
    filter: String? = null,
    order: String = DEFAULT_AUDIO_ORDER,
    ascending: Boolean = true,
    offset: Int = 0,
    limit: Int = Int.MAX_VALUE,
): List<Audio> {
    val title = MediaStore.Audio.Media.TITLE
    val artist = MediaStore.Audio.Media.ARTIST
    val album = MediaStore.Audio.Media.ALBUM
    return query2(
        uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
        projection = AUDIO_PROJECTION,
        ascending = ascending,
        selection = DEFAULT_AUDIO_SELECTION + if (filter != null) " AND $title || $artist || $album LIKE ?" else "",
        args = if (filter != null) arrayOf("%$filter%") else null,
        order = order,
        offset = offset,
        limit = limit,
        transform = { c ->
            List(c.count) {
                c.moveToPosition(it);
                c.toAudio
            }
        },
    ) ?: emptyList()
}


@Stable
data class Album(
    @JvmField val id: Long,
    @JvmField val title: String,
    @JvmField val artist: String,
    @JvmField val firstYear: Int,
    @JvmField val lastYear: Int,
    @JvmField val cardinality: Int,
)

inline val Audio.toMediaItem
    get() = MediaItem(uri, name, artist, "$id", albumUri)

fun ContentResolver.observe(uri: Uri) = callbackFlow {
    val observer = object : ContentObserver(null) {
        override fun onChange(selfChange: Boolean) {
            trySend(selfChange)
        }
    }
    registerContentObserver(uri, true, observer)
    // trigger first.
    trySend(false)
    awaitClose {
        unregisterContentObserver(observer)
    }
}


suspend fun ContentResolver.findAudio(id: Long): Audio? {
    return query2(
        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
        AUDIO_PROJECTION,
        "${MediaStore.Audio.Media._ID} ==?",
        arrayOf("$id"),
    ) {
        if (!it.moveToFirst()) return@query2 null else it.toAudio
    }
}

val Audio.uri
    get() = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id)

val Audio.albumUri
    get() = Repository.toAlbumArtUri(albumId)

val Album.uri
    get() = Repository.toAlbumArtUri(id)

val Audio.key get() = uri.toString()
