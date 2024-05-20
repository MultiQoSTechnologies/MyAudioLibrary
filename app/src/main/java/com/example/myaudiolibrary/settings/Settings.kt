package com.example.myaudiolibrary.settings

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Scaffold
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.myaudiolibrary.R
import com.example.myaudiolibrary.library.Header
import com.example.myaudiolibrary.library.Library
import com.example.myaudiolibrary.ui.theme.lightGreenWhite
import com.example.myaudiolibrary.ui.theme.lightPink
import com.example.myaudiolibrary.ui.theme.lightPurple
import com.primex.core.stringResource
import com.primex.material2.SwitchPreference

@SuppressLint("UnusedMaterialScaffoldPaddingParameter")
@Composable
fun Settings(navController: NavHostController, state: Settings) {
    val gradientGreenRed = Brush.horizontalGradient(0f to lightPurple, 1000f to lightPink)
    Scaffold {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(brush = gradientGreenRed)
                .verticalScroll(rememberScrollState())
        ) {
            Header(
                firstIcon = painterResource(id = R.drawable.ic_back_black),
                inPadding= 10.dp,
                modifier = Modifier.padding(top = 10.dp, bottom = 20.dp),
                text = "Settings",
                isShowSecButton = false,
                onSecondClick = {
                },
                onFirstClick = {
                    navController.navigate(Library.route)
                }
            )
            General(state = state)
        }
    }
}

context(ColumnScope)
@Composable
private fun General(
    state: Settings,
) {
    val useInbuiltAudioFx = state.useInbuiltAudioFx
    Row(modifier = Modifier
        .padding( start = 15.dp, end = 15.dp)
        .clip(shape = RoundedCornerShape(15.dp))
        .background(lightGreenWhite)) {
        SwitchPreference(
            title = stringResource(value = useInbuiltAudioFx.title),
            checked = useInbuiltAudioFx.value,
            summery = stringResource(value = useInbuiltAudioFx.summery),
            onCheckedChange = {
                state.set(Settings.USE_IN_BUILT_AUDIO_FX, it)
            },
            icon = Icons.Outlined.Tune
        )
    }
}