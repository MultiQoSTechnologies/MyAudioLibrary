package com.example.myaudiolibrary

import android.animation.ObjectAnimator
import android.content.Intent
import android.media.audiofx.AudioEffect
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.animation.AnticipateInterpolator
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.core.animation.doOnEnd
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import com.example.myaudiolibrary.core.compose.Channel
import com.example.myaudiolibrary.core.compose.LocalSystemFacade
import com.example.myaudiolibrary.core.compose.LocalWindowSize
import com.example.myaudiolibrary.core.compose.SystemFacade
import com.example.myaudiolibrary.core.compose.calculateWindowSizeClass
import com.example.myaudiolibrary.core.playback.RemoteInterface
import com.primex.preferences.Key
import com.example.myaudiolibrary.ui.theme.MyAudioLibraryTheme
import com.example.myaudiolibrary.ui.theme.OrientRed
import com.primex.core.Text
import com.primex.preferences.Preferences
import com.primex.preferences.observeAsState
import com.primex.preferences.value
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject


context(ComponentActivity)
private fun initSplashScreen(isColdStart: Boolean) {
    installSplashScreen().let { screen ->
        if (!isColdStart)
            return@let
        screen.setOnExitAnimationListener { provider ->
            val splashScreenView = provider.view
            val alpha = ObjectAnimator.ofFloat(
                splashScreenView, View.ALPHA, 1f, 0f
            )
            alpha.interpolator = AnticipateInterpolator()
            alpha.duration = 700L
            alpha.doOnEnd { provider.remove() }
            alpha.start()
        }
    }
}

@AndroidEntryPoint
class MainActivity : ComponentActivity(), SystemFacade {

    private val _inAppUpdateProgress = mutableFloatStateOf(Float.NaN)
    override val inAppUpdateProgress: Float
        get() = _inAppUpdateProgress.floatValue

    @Inject
    lateinit var preferences: Preferences
    @Inject
    lateinit var channel: Channel
    @Inject
    lateinit var remoteInterface: RemoteInterface

    override fun show(
        message: Text,
        title: Text?,
        action: Text?,
        icon: Any?,
        accent: Color,
        duration: Channel.Duration
    ) {
        lifecycleScope.launch {
            channel.show(message, title, action, icon, accent, duration)
        }
    }

    @Composable
    @NonRestartableComposable
    override fun <S, O> observeAsState(key: Key.Key1<S, O>): State<O?> =
        preferences.observeAsState(key = key)

    @Composable
    @NonRestartableComposable
    override fun <S, O> observeAsState(key: Key.Key2<S, O>): State<O> =
        preferences.observeAsState(key = key)

    override fun launchEqualizer(id: Int) {
        lifecycleScope.launch {
            if (id == AudioEffect.ERROR_BAD_VALUE)
                return@launch show(R.string.msg_unknown_error, R.string.error)
            val result = kotlin.runCatching {
                startActivity(
                    Intent(AudioEffect.ACTION_DISPLAY_AUDIO_EFFECT_CONTROL_PANEL).apply {
                        putExtra(AudioEffect.EXTRA_PACKAGE_NAME, packageName)
                        putExtra(AudioEffect.EXTRA_AUDIO_SESSION, id)
                        putExtra(AudioEffect.EXTRA_CONTENT_TYPE, AudioEffect.CONTENT_TYPE_MUSIC);
                    }
                )
            }
            if (!result.isFailure)
                return@launch
            val res = channel.show(
                message = R.string.msg_3rd_party_equalizer_not_found,
                action = R.string.launch,
                accent = OrientRed,
                duration = Channel.Duration.Short
            )
            if (res != Channel.Result.ActionPerformed)
                return@launch
            runCatching {
                val intent = Intent(Intent.ACTION_VIEW)
                intent.data = Uri.parse("market://search?q=equalizer")
                startActivity(intent)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val isColdStart = savedInstanceState == null //why?
        initSplashScreen(isColdStart)
        if (isColdStart) {
            val counter = preferences.value(App.KEY_LAUNCH_COUNTER) ?: 0
            preferences[App.KEY_LAUNCH_COUNTER] = counter + 1
            lifecycleScope.launch {
                delay(1000)
                onNewIntent(intent)
            }
        }
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContent {
            MyAudioLibraryTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val windowSizeClass = calculateWindowSizeClass(activity = this)
                    CompositionLocalProvider(
                        LocalSystemFacade provides this,
                        LocalWindowSize provides windowSizeClass,
                        content = { Dashboard(channel = channel) }
                    )
                }
            }
        }
    }
}