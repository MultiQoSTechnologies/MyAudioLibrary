package com.example.myaudiolibrary.soundeffects

import android.media.audiofx.Equalizer
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myaudiolibrary.core.playback.RemoteInterface
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.roundToInt


private val Equalizer.bandLevels
    get() = Array(numberOfBands.toInt()) {
        getBandLevel(it.toShort()).toFloat()
    }

private val Equalizer.presets
    get() = Array(numberOfPresets + 1) {
        if (it == 0) "Custom" else getPresetName((it - 1).toShort())
    }
private val Equalizer.status
    get() = when {
        !hasControl() -> AudioFx.EFFECT_STATUS_NO_CONTROL
        enabled -> AudioFx.EFFECT_STATUS_ENABLED
        else -> AudioFx.EFFECT_STATUS_DISABLED
    }
private const val PRESET_CUSTOM = 0
private suspend fun RemoteInterface.getEqualizerOrRetry(priority: Int): Equalizer? {
    var tries = 0
    var result: Equalizer? = null
    while (tries != 3) {
        tries++
        result = runCatching { getEqualizer(priority) }.getOrNull()
        if (result == null) {
            delay(tries * 100L)
        } else {
            break
        }
    }
    return result
}

@HiltViewModel
class AudioFxViewModel @Inject constructor(
    private val remoteInterface: RemoteInterface,
) : ViewModel(), AudioFx {

    private var equalizer: Equalizer? = null

    override var stateOfEqualizer: Int by mutableIntStateOf(AudioFx.EFFECT_STATUS_NOT_READY)
    private var _eqCurrentPreset: Int by mutableIntStateOf(0)

    override var eqNumberOfBands: Int = 0
    override val eqBandLevels = mutableStateListOf<Float>()
    override var eqBandLevelRange: ClosedFloatingPointRange<Float> = 0.0f..1.0f
    override var eqPresets: Array<String> = emptyArray()
    override fun getBandFreqRange(band: Int): ClosedFloatingPointRange<Float> =
        equalizer!!.getBandFreqRange(band.toShort()).let { it[0].toFloat()..it[1].toFloat() }

    override fun getBandCenterFreq(band: Int): Int =
        equalizer!!.getCenterFreq(band.toShort())

    override fun setBandLevel(band: Int, level: Float) {
        eqCurrentPreset = PRESET_CUSTOM
        equalizer!!.setBandLevel(band.toShort(), level.roundToInt().toShort())
    }


    override var isEqualizerEnabled: Boolean
        get() = stateOfEqualizer == AudioFx.EFFECT_STATUS_ENABLED
        set(value) {
            if (
                equalizer == null ||
                stateOfEqualizer == AudioFx.EFFECT_STATUS_NOT_READY ||
                stateOfEqualizer == AudioFx.EFFECT_STATUS_NOT_READY
            )
                return

            stateOfEqualizer =
                if (value) AudioFx.EFFECT_STATUS_ENABLED else AudioFx.EFFECT_STATUS_DISABLED
            equalizer!!.enabled = value
        }

    override var eqCurrentPreset: Int
        get() = _eqCurrentPreset
        set(value) {
            _eqCurrentPreset = value
            if (value in 1 until equalizer!!.numberOfPresets + 1)
                equalizer!!.usePreset((value - 1).toShort())

            val levels = equalizer!!.bandLevels

            repeat(levels.size) { index ->
                eqBandLevels[index] = equalizer!!.getBandLevel(index.toShort()).toFloat()
            }
        }

    override fun apply() {
        viewModelScope.launch {
            if (stateOfEqualizer == AudioFx.EFFECT_STATUS_NOT_READY || stateOfEqualizer == AudioFx.EFFECT_STATUS_NOT_SUPPORTED)
                return@launch
            remoteInterface.setEqualizer(equalizer)
        }
    }

    //initializer the variables.
    init {
        viewModelScope.launch {
            val result = remoteInterface.getEqualizerOrRetry(1)
                ?: return@launch
            equalizer = result
            _eqCurrentPreset = result.currentPreset + 1
            eqNumberOfBands = result.numberOfBands.toInt()
            eqBandLevels.addAll(result.bandLevels)
            eqBandLevelRange = result.bandLevelRange.let { it[0].toFloat()..it[1].toFloat() }
            eqPresets = result.presets
            stateOfEqualizer = result.status
        }
    }
}