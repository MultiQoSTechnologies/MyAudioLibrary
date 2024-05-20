@file:Suppress("AnimateAsStateLabel")

package com.example.myaudiolibrary

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.dialog
import androidx.navigation.compose.rememberNavController
import com.example.myaudiolibrary.console.Console
import com.example.myaudiolibrary.console.PopupMiniMediaPlayer
import com.example.myaudiolibrary.core.compose.Channel
import com.example.myaudiolibrary.core.compose.LocalNavController
import com.example.myaudiolibrary.core.compose.LocalSystemFacade
import com.example.myaudiolibrary.core.compose.LocalWindowSize
import com.example.myaudiolibrary.core.compose.NavigationSuiteScaffold
import com.example.myaudiolibrary.core.compose.Range
import com.example.myaudiolibrary.core.compose.current
import com.example.myaudiolibrary.impl.ConsoleViewModel
import com.example.myaudiolibrary.library.HomeScreen
import com.example.myaudiolibrary.library.HomeViewModel
import com.example.myaudiolibrary.library.Library
import com.example.myaudiolibrary.library.Permission
import com.example.myaudiolibrary.settings.Settings
import com.example.myaudiolibrary.settings.SettingsViewModel
import com.example.myaudiolibrary.soundeffects.AudioFx
import com.example.myaudiolibrary.soundeffects.AudioFxViewModel
import com.example.myaudiolibrary.ui.theme.lightPink
import com.example.myaudiolibrary.ui.theme.lightPurple
import com.google.accompanist.permissions.ExperimentalPermissionsApi

private const val TAG = "Home"

private val EnterTransition = fadeIn(tween(700))
private val ExitTransition = fadeOut(tween(700))

const val PERMISSION_ROUTE = "_route_storage_permission"

@NonRestartableComposable
@Composable
private fun NavGraph(
    navController: NavHostController,
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = LocalNavController.current,
        modifier = modifier,
        startDestination =  Library.route, //
        enterTransition = { EnterTransition },
        exitTransition = { ExitTransition },
        builder = {
            //Permission
            composable(PERMISSION_ROUTE) {
                Permission()
            }
            composable(Library.route) {
                val viewModel = hiltViewModel<HomeViewModel>()
                HomeScreen(navController, viewModel)
            }
            composable(Settings.route) {
                val viewModel = hiltViewModel<SettingsViewModel>()
                Settings(navController,state = viewModel)
            }

            dialog(AudioFx.route) {
                val viewModel = hiltViewModel<AudioFxViewModel>()
                AudioFx(state = viewModel)
            }

            composable(Console.route) {
                val viewModel = hiltViewModel<ConsoleViewModel>()
                Console(state = viewModel)
            }
        },
    )
}

private val HIDDEN_DEST_ROUTES =
    arrayOf(Console.route, PERMISSION_ROUTE, AudioFx.route)

@Composable
fun Dashboard(channel: Channel) {
    val navController = rememberNavController()
    val clazz = LocalWindowSize.current
    CompositionLocalProvider(
        LocalNavController provides navController,
        content = {
            val facade = LocalSystemFacade.current
            val hideNavigationBar = navController.current in HIDDEN_DEST_ROUTES
            NavigationSuiteScaffold(
                vertical = clazz.widthRange < Range.Medium,
                channel = channel,
                hideNavigationBar = hideNavigationBar,
                progress = facade.inAppUpdateProgress,
                navBar = {
                    val gradientGreenRed = Brush.horizontalGradient(0f to lightPurple, 1000f to lightPink)
                    Column(modifier = Modifier.background(brush = gradientGreenRed).padding(bottom = 50.dp)) {
                        PopupMiniMediaPlayer()
                    }
                },
                content = {
                    NavGraph(
                        navController,
                        modifier = Modifier
                            .background(Color.Transparent)
                            .fillMaxSize()
                    )
                }
            )
        }
    )
}

