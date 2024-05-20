package com.example.myaudiolibrary.settings

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.myaudiolibrary.core.Route
import com.primex.core.Text
import com.primex.preferences.booleanPreferenceKey
import com.primex.preferences.intPreferenceKey
import com.primex.preferences.stringSetPreferenceKey
import com.primex.preferences.Key

private const val TAG = "Settings"

@Stable
data class Preference<out P>(
    @JvmField val value: P,
    @JvmField val title: Text,
    @JvmField val vector: ImageVector? = null,
    @JvmField val summery: Text? = null,
)

@Stable
interface Settings {
    companion object : Route {
        override val route = "settings"
        override val title: Text get() = Text("Settings")
        override val icon: ImageVector get() = Icons.Outlined.Settings

        private const val PREFIX = "Audiofy"

        val MIN_TRACK_LENGTH_SECS =
            intPreferenceKey(PREFIX + "_track_duration_", 30)

        val USE_LEGACY_ARTWORK_METHOD = booleanPreferenceKey(PREFIX + "_artwork_from_ms", true)

        val BLACKLISTED_FILES = stringSetPreferenceKey(PREFIX + "_blacklisted_files")


        val USE_IN_BUILT_AUDIO_FX = booleanPreferenceKey(PREFIX + "_use_in_built_audio_fx", true)
    }

    val useInbuiltAudioFx: Preference<Boolean>

    fun <S, O> set(key: Key<S, O>, value: O)
}