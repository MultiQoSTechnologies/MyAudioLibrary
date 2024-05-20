package com.example.myaudiolibrary.settings

import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myaudiolibrary.R
import com.example.myaudiolibrary.core.util.asComposeState
import com.primex.core.Text
import com.primex.preferences.Key
import com.primex.preferences.Preferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import javax.inject.Inject


context (Preferences, ViewModel)
private fun <T> Flow<T>.asComposeState(): State<T> = asComposeState(runBlocking { first() })

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferences: Preferences
) : ViewModel(),Settings {

    override val useInbuiltAudioFx by with(preferences) {
        preferences[Settings.USE_IN_BUILT_AUDIO_FX].map {
            Preference(
                title = Text(R.string.pref_use_inbuilt_audio_effects),
                summery = Text(R.string.pref_use_inbuilt_audio_effects_summery),
                value = it
            )
        }.asComposeState()
    }

    override fun <S, O> set(key: Key<S, O>, value: O) {
        viewModelScope.launch {
            preferences[key] = value
        }
    }
}