package com.example.myaudiolibrary.console


import androidx.annotation.FloatRange
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.Slider
import androidx.compose.material.SliderDefaults
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DoneAll
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.TimerOff
import androidx.compose.runtime.Composable
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.myaudiolibrary.R
import com.example.myaudiolibrary.ui.theme.blackColor
import com.example.myaudiolibrary.ui.theme.fontSemiBold
import com.example.myaudiolibrary.ui.theme.lightPink
import com.example.myaudiolibrary.ui.theme.lightPurple
import com.example.myaudiolibrary.ui.theme.lightWhitePurple
import com.example.myaudiolibrary.ui.theme.whiteColor
import com.primex.core.MetroGreen
import com.primex.core.textResource
import com.primex.material2.Dialog
import com.primex.material2.IconButton
import com.primex.material2.Label
import com.primex.material2.menu.DropDownMenu
import kotlin.math.roundToInt

private const val TAG = "Track Timer And speed Dialog"

@Composable
@NonRestartableComposable
private fun TopBar(
    onRequestTimerOff: () -> Unit,
    modifier: Modifier = Modifier,
) {
    androidx.compose.material.TopAppBar(
        title = {
            Label(
                text = textResource(R.string.sleep_timer_dialog_title),
                style = fontSemiBold.copy(color = whiteColor)
            )
        },
        backgroundColor = lightPurple,
        contentColor = whiteColor,
        modifier = modifier,
        elevation = 10.dp,
        actions = {
            IconButton(imageVector = Icons.Outlined.TimerOff, onClick = onRequestTimerOff)
        }
    )
}


//SleepTimer
context(ColumnScope)
@Composable
private inline fun Layout(
    crossinline onValueChange: (value: Long) -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    var value by remember { mutableFloatStateOf(10f) }

    Label(
        text = stringResource(R.string.sleep_timer_dialog_minute_s, value.roundToInt()),
        modifier = Modifier
            .align(Alignment.CenterHorizontally)
            .padding(top = 8.dp),
        style = fontSemiBold.copy(color = lightPurple)
    )

    Slider(
        value = value,
        onValueChange = {
            value = it
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        },
        valueRange = 10f..100f,
        modifier = Modifier
            .align(Alignment.CenterHorizontally)
            .padding(16.dp),
        steps = 7,
        colors = SliderDefaults.colors(
            activeTrackColor = lightPurple,
            disabledActiveTickColor = lightWhitePurple,
            thumbColor = lightPurple
        )
    )

    // Buttons
    Row(
        horizontalArrangement = Arrangement.End,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Box(
            modifier = Modifier
                .padding(start = 10.dp, bottom = 15.dp)
                .padding(horizontal = 15.dp, vertical = 5.dp)
                .clickable(
                    onClick = {
                        onValueChange(-2)
                    },
                )
        ) {
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
                        onValueChange((value.roundToInt() * 60 * 1_000L))
                    },
                )
        ) {
            Text(
                text = stringResource(id = R.string.start),
                color = lightPurple,
                style = fontSemiBold
            )
        }
    }
}

//SleepTimer
@Composable
@NonRestartableComposable
fun SleepTimer(
    expanded: Boolean,
    onValueChange: (value: Long) -> Unit,
) {
    Dialog(
        expanded = expanded,
        onDismissRequest = { onValueChange(-2) }
    ) {
        Surface(
            contentColor = blackColor,
            content = {
                Column {
                    TopBar(onRequestTimerOff = { onValueChange(-1) })
                    Layout(onValueChange = onValueChange)
                }
            }
        )
    }
}

//Track Speed.
@Composable
@NonRestartableComposable
fun PlaybackSpeed(
    expanded: Boolean,
    @FloatRange(0.25, 2.0) value: Float,
    onValueChange: (value: Float) -> Unit,
) {
    DropDownMenu(
        expanded = expanded,
        onDismissRequest = { onValueChange(-1f) },
        content = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .widthIn(max = 250.dp)
            ) {
                val (state, callback) = remember { mutableFloatStateOf(value) }
                // Label
                Label(
                    text = stringResource(R.string.playback_speed_dialog_x_f, state),
                    fontWeight = FontWeight.Bold,
                    style =  fontSemiBold.copy(color = lightPurple),
                    modifier = Modifier.padding(
                        start = 16.dp,
                        end = 8.dp
                    )
                )

                Slider(
                    value = state,
                    onValueChange = callback,
                    valueRange = 0.25f..2f,
                    modifier = Modifier.weight(1f),
                    steps = 6,
                    colors = SliderDefaults.colors(
                        activeTrackColor = lightPurple,
                        disabledActiveTickColor = lightPink,
                        thumbColor = lightPurple
                    )
                )

                IconButton(
                    imageVector = if (value != state) Icons.Outlined.DoneAll else Icons.Outlined.Speed,
                    onClick = { onValueChange(state) },
                    tint = if (value == state) lightPurple else Color.MetroGreen,
                    enabled = value != state,
                )
            }
        },
    )
}

