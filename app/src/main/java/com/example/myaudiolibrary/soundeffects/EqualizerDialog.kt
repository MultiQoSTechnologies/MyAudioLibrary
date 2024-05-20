@file:OptIn(ExperimentalMaterialApi::class)

package com.example.myaudiolibrary.soundeffects

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ChipDefaults
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.FilterChip
import androidx.compose.material.Icon
import androidx.compose.material.Slider
import androidx.compose.material.SliderDefaults
import androidx.compose.material.Switch
import androidx.compose.material.SwitchDefaults
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myaudiolibrary.R
import com.example.myaudiolibrary.core.compose.LocalNavController
import com.example.myaudiolibrary.ui.theme.blackColor
import com.example.myaudiolibrary.ui.theme.fontRegular
import com.example.myaudiolibrary.ui.theme.fontSemiBold
import com.example.myaudiolibrary.ui.theme.lightPurple
import com.example.myaudiolibrary.ui.theme.lightWhitePurple
import com.example.myaudiolibrary.ui.theme.whiteColor
import com.primex.core.rotateTransform
import com.primex.core.textResource
import com.primex.material2.Label


private const val TAG = "Audio Equalizer Code."
@Composable
@NonRestartableComposable
private fun TopBar(
    enabled: Boolean,
    onToggleState: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    androidx.compose.material.TopAppBar(
        title = {
            Label(
                text = textResource(R.string.equalizer),
                style = fontSemiBold.copy(color = whiteColor),
            )
        },
        backgroundColor = lightPurple,
        contentColor = blackColor,
        modifier = modifier,
        elevation = 0.dp,
        actions = {
            Switch(
                checked = enabled, onCheckedChange = onToggleState, colors = SwitchDefaults.colors(
                    checkedThumbColor = whiteColor,
                    uncheckedThumbColor = whiteColor,
                    uncheckedTrackColor = lightWhitePurple,
                    checkedTrackColor = whiteColor,
                )
            )
        }
    )
}

@Composable
@NonRestartableComposable
private fun BottomBar(
    modifier: Modifier = Modifier,
    onDismissRequest: (apply: Boolean) -> Unit,
) {
    Row(
        horizontalArrangement = Arrangement.End,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Box(
            modifier = Modifier
                .padding(start = 10.dp, bottom = 15.dp)
                .padding(horizontal = 15.dp, vertical = 5.dp)
                .clickable(
                    onClick = {
                        onDismissRequest(false)
                    },
                )
        ){
            Text(
                text = stringResource(id = R.string.dismiss),
                color = lightPurple,
                style = fontSemiBold
            )
        }
        Box(
            modifier = Modifier
                .padding(start = 10.dp, bottom = 15.dp)
                .padding(horizontal = 15.dp, vertical = 5.dp)
                .clickable(
                    onClick = {
                        onDismissRequest(false)
                    },
                )
        ) {
            Text(
                text = stringResource(id = R.string.apply),
                color = lightPurple,
                style = fontSemiBold
            )
        }
    }
}

@Composable
@NonRestartableComposable
fun AudioFx(
    state: AudioFx,
) {
    Column(
        modifier = Modifier
            .clip(shape = RoundedCornerShape(10.dp))
            .background(whiteColor)
            .fillMaxWidth()
            .animateContentSize()
            .pointerInput(Unit) {}
    ) {
        TopBar(state.isEqualizerEnabled, onToggleState = { state.isEqualizerEnabled = it })
        if (state.isEqualizerReady) {
            Row(
                Modifier
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                state.eqPresets.forEachIndexed { index, s ->
                    val current = state.eqCurrentPreset
                    Preset(
                        label = s,
                        onClick = { state.eqCurrentPreset = index },
                        selected = current == index
                    )
                }
            }

            Equalizer(
                fx = state,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }
        val controller = LocalNavController.current
        BottomBar(
            onDismissRequest = {
                if (it)
                    state.apply()
                controller.navigateUp()
            }
        )
    }
}

@Composable
private fun Equalizer(
    fx: AudioFx,
    modifier: Modifier = Modifier,
) {
    Row(
        horizontalArrangement = Arrangement.Center,
        modifier = modifier
            .sizeIn(maxHeight = 220.dp)
            .horizontalScroll(rememberScrollState()),
    ) {
        Column(
            verticalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxHeight()
        ) {
            Column(
                modifier = Modifier.fillMaxHeight(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Label(
                    text = stringResource(
                        id = R.string.audio_fx_scr_abbr_db_suffix_d,
                        fx.eqBandLevelRange.endInclusive / 1000
                    )
                )
                Label(text = stringResource(id = R.string.audio_fx_scr_abbr_db_suffix_d, 0))
                Label(
                    text = stringResource(
                        id = R.string.audio_fx_scr_abbr_db_suffix_d,
                        fx.eqBandLevelRange.start / 1000
                    )
                )
            }
        }

        // Equalizer bars
        repeat(fx.eqNumberOfBands) { band ->
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Slider(
                    value = fx.eqBandLevels[band],
                    onValueChange = { fx.setBandLevel(band, it) },
                    modifier = Modifier
                        .padding(bottom = 8.dp)
                        .weight(1f)
                        .rotateTransform(false),
                    valueRange = fx.eqBandLevelRange,
                    colors = SliderDefaults.colors(
                        thumbColor = lightPurple,
                        activeTickColor = lightPurple,
                        disabledActiveTickColor = Color.White,
                        activeTrackColor = lightPurple
                    )
                )

                Label(
                    text = stringResource(
                        id = R.string.audio_fx_scr_abbr_hz_suffix_d,
                        fx.getBandCenterFreq(band) / 1000
                    ),
                    style = fontRegular.copy(fontSize = 12.sp),
                    modifier = Modifier.padding(bottom = 1.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
@NonRestartableComposable
fun Preset(
    label: CharSequence,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    enabled: Boolean = true,
    icon: ImageVector? = null,
) {
    val color = ChipDefaults.outlinedFilterChipColors(backgroundColor = if (!selected) whiteColor else lightPurple)
    FilterChip(
        onClick = onClick,
        colors = color,
        selected = selected,
        enabled = enabled,
        border = BorderStroke(1.dp, color = lightPurple),
        modifier = modifier.padding(4.dp)
    ) {
        Label(
            text = label,
            modifier = Modifier.padding(end = 4.dp),
            style = fontRegular.copy(fontSize = 12.sp, color = if (selected) whiteColor else blackColor)
        )

        if (icon == null)
            return@FilterChip
        Icon(
            imageVector = icon,
            contentDescription = label.toString(),
            modifier = Modifier.size(16.dp)
        )
    }
}

private inline val AudioFx.isEqualizerReady
    get() = stateOfEqualizer != AudioFx.EFFECT_STATUS_NOT_READY || stateOfEqualizer != AudioFx.EFFECT_STATUS_NOT_SUPPORTED



