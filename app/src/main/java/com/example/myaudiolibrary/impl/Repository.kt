package com.example.myaudiolibrary.impl

import android.content.ContentResolver
import android.content.ContentUris
import android.net.Uri
import android.provider.MediaStore
import androidx.annotation.WorkerThread
import com.example.myaudiolibrary.core.db.Audio
import com.example.myaudiolibrary.core.db.Playlist
import com.example.myaudiolibrary.core.db.Playlists
import com.example.myaudiolibrary.core.db.findAudio
import com.example.myaudiolibrary.core.db.getAudios
import com.example.myaudiolibrary.core.db.observe
import com.example.myaudiolibrary.core.db.query2
import com.example.myaudiolibrary.settings.Settings
import com.primex.preferences.Preferences
import com.primex.preferences.value
import dagger.hilt.android.scopes.ActivityRetainedScoped
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

@ActivityRetainedScoped
class Repository @Inject constructor(
    private val playlistz: Playlists,
    private val resolver: ContentResolver,
    private val preferences: Preferences,
) {
    companion object {
        private const val ALBUM_ART_URI: String = "content://media/external/audio/albumart"

        fun toAlbumArtUri(id: Long): Uri = ContentUris.withAppendedId(Uri.parse(ALBUM_ART_URI), id)
    }

    fun observe(uri: Uri) = combine(
        flow = resolver.observe(uri),
        flow2 = preferences[Settings.BLACKLISTED_FILES],
        flow3 = preferences[Settings.MIN_TRACK_LENGTH_SECS]
    ) { self, _, _ ->
        self
    }

    fun recent(limit: Int) =
        observe(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI).map {
            resolver.query2(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                arrayOf(MediaStore.Audio.Media.ALBUM_ID),
                order = MediaStore.Audio.Media.DATE_MODIFIED,
                limit = limit,
                ascending = false,
            ) { c -> Array(c.count) { c.moveToPosition(it); c.getLong(0) } } ?: emptyArray()
        }

    val playlists =
        playlistz.observe().map { playlists ->
            playlists.filter { it.name.indexOf(Playlists.PRIVATE_PLAYLIST_PREFIX) != 0 }
        }

    private suspend fun filter(values: List<Audio>): List<Audio> {
        val excludePaths = preferences.value(Settings.BLACKLISTED_FILES)

        fun isExcluded(path: String): Boolean {
            if (excludePaths == null) return false
            for (p in excludePaths) {
                if (p == path || path.startsWith("$p/"))
                    return true
            }
            return false
        }

        val limit = preferences.value(Settings.MIN_TRACK_LENGTH_SECS)
        return values.filter { audio ->
            (audio.duration / 1000) > limit && !isExcluded(audio.data)
        }
    }

    suspend fun getAudios(
        query: String? = null,
        order: String = MediaStore.Audio.Media.TITLE,
        ascending: Boolean = true,
        offset: Int = 0,
        limit: Int = Int.MAX_VALUE,
    ) = filter(resolver.getAudios(query, order, ascending, offset = offset, limit = limit))

    fun playlist(id: Long): Flow<List<Playlist.Member>> =
        playlistz.observe2(id)

    fun playlist(name: String): Flow<List<Playlist.Member>> =
        playlistz.observe2(name)

    suspend fun findAudio(id: Long) = resolver.findAudio(id)

    suspend fun exists(playlistName: String): Boolean = playlistz.get(playlistName) != null

    suspend fun create(playlist: Playlist): Long {
        if (exists(playlist.name)) return -1L
        return playlistz.insert(playlist)
    }
    @Deprecated("Use create(Playlist)")
    suspend fun createPlaylist(
        name: String, desc: String = "",
    ): Long = create(Playlist(name = name, desc = desc))

    @Deprecated("Use update(Playlist)")
    suspend fun updatePlaylist(value: Playlist): Boolean =  false

    @WorkerThread
    @Deprecated("use directly findAudio")
    fun getAudioById(id: Long) = runBlocking { findAudio(id) }

}