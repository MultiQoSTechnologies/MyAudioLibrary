@file:Suppress("CrossfadeLabel", "FunctionName")

package com.example.myaudiolibrary.library

import android.annotation.SuppressLint
import android.content.pm.PackageManager
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.navigation.NavHostController
import com.example.myaudiolibrary.App
import com.example.myaudiolibrary.MainActivity
import com.example.myaudiolibrary.PERMISSION_ROUTE
import com.example.myaudiolibrary.R
import com.example.myaudiolibrary.console.Console
import com.example.myaudiolibrary.core.compose.Artwork
import com.example.myaudiolibrary.core.compose.LocalNavController
import com.example.myaudiolibrary.core.compose.LocalWindowSize
import com.example.myaudiolibrary.core.compose.Placeholder
import com.example.myaudiolibrary.core.compose.Range
import com.example.myaudiolibrary.core.db.Audio
import com.example.myaudiolibrary.core.db.Playlist
import com.example.myaudiolibrary.core.db.albumUri
import com.example.myaudiolibrary.ui.theme.blackColor
import com.example.myaudiolibrary.ui.theme.fontSemiBold
import com.example.myaudiolibrary.ui.theme.lightPink
import com.example.myaudiolibrary.ui.theme.lightPurple
import com.example.myaudiolibrary.ui.theme.lightWhitePink
import com.example.myaudiolibrary.ui.theme.whiteColor
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberPermissionState
import com.primex.material2.OutlinedButton
import com.primex.material2.Text

@SuppressLint("UnusedMaterialScaffoldPaddingParameter")
@Composable
fun HomeScreen(
    navController: NavHostController,
    state: Library,
) {
    val gradientGreenRed = Brush.horizontalGradient(0f to lightPurple, 1000f to lightPink)
    val recents by state.recent.collectAsState(initial = null)
    val audios by state.newlyAdded.collectAsState(initial = null)
    val recentUri = mutableStateOf("")

    val remote = (LocalView.current.context as MainActivity).remoteInterface
    var isPlaying by rememberSaveable { mutableStateOf(remote.playWhenReady) }
    val context = LocalContext.current

    when (ContextCompat.checkSelfPermission(context, App.STORAGE_PERMISSION)) {
        PackageManager.PERMISSION_GRANTED -> {
            LaunchedEffect(key1 = Unit) {
                remote.events.collect { event ->
                    isPlaying = remote.playWhenReady
                    if (!remote.playWhenReady) return@collect
                }
            }
        }
        else -> {
            navController.navigate(PERMISSION_ROUTE)
        }
    }

    Scaffold(
        content = {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(brush = gradientGreenRed),
                content = {
                    Header(
                        firstIcon = painterResource(id = R.drawable.ic_default_music_icon),
                        modifier = Modifier
                            .padding(top = 10.dp),
                        text = "",
                        isShowSecButton = true,
                        onSecondClick = {
                            navController.navigate(com.example.myaudiolibrary.settings.Settings.route)
                        },
                        onFirstClick = {}
                    )
                    Column(
                        modifier = Modifier
                            .verticalScroll(rememberScrollState())
                    ) {
                        if (recents?.isNotEmpty() == true) {
                            HeaderForHistory(
                                text = "Recently Played",
                            )
                            RecentlyPlayedList(
                                recentUri,
                                recents,
                            ) { uri ->
                                state.onClickRecentFile(uri)
                            }
                        }

                        if (audios?.size != 0) {
                            HeaderForHistory(
                                text = "You Might Like",
                            )

                            NewlyAddedList(
                                audios,
                            ) { track ->
                                state.onClickRecentAddedFile(track)
                                navController.navigate(Console.direction())
                            }
                        }
                    }
                }
            )
        }
    )
}


@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun Permission() {
    val controller = LocalNavController.current
    val permission = rememberPermissionState(permission = App.STORAGE_PERMISSION) {
        if (!it) return@rememberPermissionState
        controller.graph.setStartDestination(Library.route)
        controller.navigate(Library.route) { popUpTo(PERMISSION_ROUTE) { inclusive = true } }
    }
    Placeholder(
        iconResId = R.raw.lt_permission,
        title = stringResource(R.string.permission_screen_title),
        message = stringResource(R.string.permission_screen_desc),
        vertical = LocalWindowSize.current.widthRange == Range.Compact
    ) {
        OutlinedButton(
            onClick = { permission.launchPermissionRequest() },
            modifier = Modifier.size(width = 200.dp, height = 46.dp),
            elevation = null,
            label = stringResource(R.string.allow),
            border = ButtonDefaults.outlinedBorder,
            shape = CircleShape,
            colors = ButtonDefaults.outlinedButtonColors(backgroundColor = Color.Transparent)
        )
    }
}


@Composable
fun Header(
    text: CharSequence,
    firstIcon: Painter,
    inPadding: Dp = 2.dp,
    modifier: Modifier = Modifier,
    isShowSecButton: Boolean,
    onSecondClick: () -> Unit,
    onFirstClick: () -> Unit,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(90.dp)
            .padding(end = 16.dp, start = 16.dp, top = 25.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(45.dp)
                .clip(shape = CircleShape)
                .background(lightWhitePink.copy(alpha = 0.3f))
                .clickable(
                    onClick = {
                        onFirstClick()
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = firstIcon,
                contentDescription = "",
                colorFilter = ColorFilter.tint(blackColor),
                modifier = Modifier
                    .padding(inPadding)
                    .size(40.dp)
            )
        }

        Text(
            style = fontSemiBold.copy(color = whiteColor, fontSize = 25.sp),
            text = text
        )
        if (isShowSecButton) {
            Box(
                modifier = Modifier
                    .size(45.dp)
                    .clip(shape = CircleShape)
                    .background(lightWhitePink.copy(alpha = 0.3f))
                    .clickable(
                        onClick = {
                            onSecondClick()
                        }
                    ), contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_settings),
                    contentDescription = "",
                    modifier = Modifier
                        .padding(6.dp)
                        .size(40.dp)
                )
            }
        } else {
            Box(
                modifier = Modifier
                    .size(45.dp)
            ) {}
        }
    }
}

@Composable
private fun HeaderForHistory(
    text: CharSequence,
) {
    Row(
        modifier = Modifier
            .padding(16.dp, 18.dp, 16.dp, 25.dp)
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Title
        Text(
            style = fontSemiBold.copy(color = whiteColor, fontSize = 25.sp),
            text = text
        )
        Image(
            painter = painterResource(id = R.drawable.ic_back),
            contentDescription = "",
            modifier = Modifier.size(26.dp)
        )
    }
}

@Composable
fun NewlyAddedList(
    audios: List<Audio>?,
    onClick: (trackId: Long) -> Unit,
) {
    audios?.forEachIndexed { index, audio ->
        TrackListItem(
            track = audio,
        ) {
            onClick(audio.id)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun RecentlyPlayedList(
    recentUri: MutableState<String>,
    recents: List<Playlist.Member>?,
    onClick: (uri: String) -> Unit,
) {
    LazyRow(modifier = Modifier.padding(horizontal = 10.dp)) {
        recents?.size?.let {
            items(it) { index ->
                recentUri.value = recents[0].uri
                val recentsD = recents[index]
                RecentItem(
                    label = recentsD.title,
                    onClick = { onClick(recentsD.uri) },
                    modifier = Modifier
                        .animateItemPlacement(),
                    artworkUri = recentsD.artwork,
                )
            }
        }
    }
}

@Composable
private fun RecentItem(
    label: CharSequence,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    artworkUri: String? = null,
) {
    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clickable(onClick = onClick)
    ) {
        Artwork(
            data = artworkUri,
            modifier = Modifier
                .padding(end = 15.dp)
                .size(150.dp)
                .clip(shape = RoundedCornerShape(15.dp))
        )

        Text(
            text = label,
            modifier = Modifier
                .padding(end = 15.dp, top = 10.dp)
                .width(150.dp),
            style = fontSemiBold.copy(color = whiteColor),
            maxLines = 2,
            textAlign = TextAlign.Center,
            minLines = 2
        )
    }
}

@Composable
fun TrackListItem(track: Audio, onClick: (trackId: Long) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .padding(horizontal = 14.dp, vertical = 5.dp)
            .clickable(onClick = {
                onClick(track.id)
            }),
    ) {
        Artwork(
            data = track.albumUri,
            modifier = Modifier
                .size(64.dp)
                .padding(5.dp)
                .clip(shape = CircleShape)
        )
        Column(
            modifier = Modifier
                .padding(start = 10.dp, end = 10.dp)
                .weight(weight = 1f)
        ) {
            Text(
                text = track.name,
                modifier = Modifier
                    .fillMaxWidth()
                    .basicMarquee(),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = whiteColor,
                style = fontSemiBold.copy(fontSize = 18.sp),

                )
            Text(
                text = track.artist,
                modifier = Modifier
                    .padding(top = 3.dp)
                    .fillMaxWidth()
                    .basicMarquee(),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontSize = 14.sp,
                color = whiteColor,
                style = fontSemiBold
            )
        }
    }
}