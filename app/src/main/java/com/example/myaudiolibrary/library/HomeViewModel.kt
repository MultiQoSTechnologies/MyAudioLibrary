package com.example.myaudiolibrary.library

import android.net.Uri
import android.provider.MediaStore
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Error
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myaudiolibrary.R
import com.example.myaudiolibrary.core.compose.Channel
import com.example.myaudiolibrary.core.db.toMediaItem
import com.example.myaudiolibrary.core.db.uri
import com.example.myaudiolibrary.impl.Repository
import com.example.myaudiolibrary.core.playback.Playback
import com.example.myaudiolibrary.core.playback.RemoteInterface
import com.example.myaudiolibrary.ui.theme.Rose
import com.example.myaudiolibrary.core.util.toMediaItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.transform
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val CAROUSAL_DELAY_MILLS = 10_000L

private val TimeOutPolicy = SharingStarted.Lazily

private const val SHOW_CASE_MAX_ITEMS = 20

@HiltViewModel
class HomeViewModel @Inject constructor(
    repository: Repository,
    private val remoteInterface: RemoteInterface,
    private val channel: Channel
) : ViewModel(), Library {

    override val recent = repository.playlist(Playback.PLAYLIST_RECENT)
    override val carousel = repository
        .recent(SHOW_CASE_MAX_ITEMS)
        .transform { list ->
            if (list.isEmpty()) {
                emit(null)
                return@transform
            }
            var current = 0
            while (true) {
                if (current >= list.size)
                    current = 0
                emit(list[current])
                current++
                kotlinx.coroutines.delay(CAROUSAL_DELAY_MILLS)
            }
        }
        .stateIn(viewModelScope, TimeOutPolicy, null)

    override val newlyAdded = repository
        .observe(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI)
        .map {
            repository.getAudios(
                order = MediaStore.Audio.Media.DATE_MODIFIED,
                ascending = false,
                offset = 0,
                limit = SHOW_CASE_MAX_ITEMS
            )
        }

    override fun onClickRecentFile(uri: String) {
        viewModelScope.launch {
            val isAlreadyInPlaylist = remoteInterface.seekTo(Uri.parse(uri))
            if (isAlreadyInPlaylist) {
                remoteInterface.play(true)
                return@launch
            }
            val items = recent.firstOrNull()
            val item = items?.find { it.uri == uri }
            if (item == null) {
                channel.show(R.string.msg_unknown_error, leading = Icons.Outlined.Error, accent = Rose)
                return@launch
            }
            remoteInterface.add(item.toMediaItem, index = remoteInterface.nextIndex)
            remoteInterface.seekTo(Uri.parse(uri))
            remoteInterface.play(true)
        }
    }

    override fun onClickRecentAddedFile(id: Long) {
        viewModelScope.launch {
            val items = newlyAdded.firstOrNull()
            val item = items?.find { it.id == id }
            if (item == null) {
                channel.show(
                    R.string.msg_unknown_error,
                    R.string.error,
                    leading = Icons.Outlined.Error,
                    accent = Rose
                )
                return@launch
            }
            val isAlreadyInPlaylist = remoteInterface.seekTo(item.uri)
            if (isAlreadyInPlaylist) {
                remoteInterface.play(true)
                return@launch
            }
            remoteInterface.add(item.toMediaItem, index = remoteInterface.nextIndex)
            remoteInterface.seekTo(item.uri)
            remoteInterface.play(true)
        }
    }
}