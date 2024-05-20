package com.example.myaudiolibrary.soundeffects

import androidx.compose.runtime.Stable

@Stable
interface AudioFx {
    companion object {
        const val route = "audio_fx"
        const val EFFECT_STATUS_NOT_SUPPORTED = -1
        const val EFFECT_STATUS_DISABLED = 0
        const val EFFECT_STATUS_ENABLED = 1
        const val EFFECT_STATUS_NO_CONTROL = 2
        const val EFFECT_STATUS_NOT_READY = 3
    }
    val stateOfEqualizer: Int
    var isEqualizerEnabled: Boolean
    val eqPresets: Array<String>
    val eqNumberOfBands: Int
    var eqCurrentPreset: Int
    val eqBandLevels: List<Float>
    val eqBandLevelRange: ClosedFloatingPointRange<Float>
    fun getBandFreqRange(band: Int): ClosedFloatingPointRange<Float>
    fun setBandLevel(band: Int, level: Float)
    fun getBandCenterFreq(band: Int): Int
    fun apply()
}