package com.example.myaudiolibrary.core.compose

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.core.content.res.ResourcesCompat
import com.primex.core.Text
import com.primex.preferences.Key

@Stable
interface SystemFacade {
    val inAppUpdateProgress: Float
    fun show(
        message: Text,
        title: Text? = null,
        action: Text? = null,
        icon: Any? = null,
        accent: Color = Color.Unspecified,
        duration: Channel.Duration = Channel.Duration.Short
    )

    fun show(
        @StringRes message: Int,
        @StringRes title: Int = ResourcesCompat.ID_NULL,
        @StringRes action: Int = ResourcesCompat.ID_NULL,
        icon: Any? = null,
        accent: Color = Color.Unspecified,
        duration: Channel.Duration = Channel.Duration.Short
    ) = show(
        Text(message),
        title = if (title == ResourcesCompat.ID_NULL) null else Text(title),
        action = if (action == ResourcesCompat.ID_NULL) null else Text(action),
        icon = icon,
        accent = accent,
        duration = duration
    )
    @Composable
    @NonRestartableComposable
    fun <S, O> observeAsState(key: Key.Key1<S, O>): State<O?>

    @Composable
    @NonRestartableComposable
    fun <S, O> observeAsState(key: Key.Key2<S, O>): State<O>
    fun launchEqualizer(id: Int)
}

val LocalSystemFacade =
    staticCompositionLocalOf<SystemFacade> {
        error("Provider not defined.")
    }

@Composable
inline fun <S, O> preference(key: Key.Key2<S, O>): State<O> {
    val provider = LocalSystemFacade.current
    return provider.observeAsState(key = key)
}