package com.example.myaudiolibrary.impl

import android.content.ContentResolver
import android.content.Context
import com.example.myaudiolibrary.core.compose.Channel
import com.example.myaudiolibrary.core.db.Playlists
import com.primex.preferences.Preferences
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityRetainedComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.scopes.ActivityRetainedScoped
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SingletonModules {
    @Provides
    @Singleton
    fun preferences(@ApplicationContext context: Context) =
        Preferences(context, "Shared_Preferences")

    @Singleton
    @Provides
    fun playlists(@ApplicationContext context: Context) =
        Playlists(context)

    @Singleton
    @Provides
    fun resolver(@ApplicationContext context: Context): ContentResolver =
        context.contentResolver

    @Provides
    @Singleton
    fun remote(@ApplicationContext context: Context) = Remote(context)
}

@Module
@InstallIn(ActivityRetainedComponent::class)
object ActivityModules {
    @ActivityRetainedScoped
    @Provides
    fun toaster() = Channel()

    @ActivityRetainedScoped
    @Provides
    fun systemDelegate(@ApplicationContext ctx: Context, channel: Channel): SystemDelegate =
        SystemDelegate(ctx, channel)
}