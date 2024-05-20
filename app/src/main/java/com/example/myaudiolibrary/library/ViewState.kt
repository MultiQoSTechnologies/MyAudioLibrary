package com.example.myaudiolibrary.library

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.myaudiolibrary.core.Route
import com.example.myaudiolibrary.core.db.Audio
import com.example.myaudiolibrary.core.db.Playlist
import com.primex.core.Text
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface Library {
    companion object : Route {
        override val title: Text get() = Text("Library")
        override val icon: ImageVector get() = Icons.Outlined.LibraryMusic
        override val route: String get() = "route_library"
    }

    val recent: Flow<List<Playlist.Member>>
    val carousel: StateFlow<Long?>
    val newlyAdded: Flow<List<Audio>>

    fun onClickRecentFile(uri: String)

    fun onClickRecentAddedFile(id: Long)
}