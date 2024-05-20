package com.example.myaudiolibrary

import android.app.Application
import android.content.ContentValues.TAG
import coil.ImageLoaderFactory
import coil.ImageLoader
import coil.fetch.Fetcher.Factory
import coil.memory.MemoryCache
import coil.request.CachePolicy
import android.provider.MediaStore
import android.net.Uri
import coil.request.Options
import coil.fetch.Fetcher

import android.os.Build
import coil.disk.DiskCache
import com.example.myaudiolibrary.impl.MediaMetaDataArtFetcher
import com.example.myaudiolibrary.settings.Settings
import com.primex.preferences.Preferences
import com.primex.preferences.intPreferenceKey
import com.primex.preferences.value
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject



@HiltAndroidApp
class App : Application(), ImageLoaderFactory {

    companion object {
        val KEY_LAUNCH_COUNTER =
            intPreferenceKey(TAG + "_launch_counter")

        val STORAGE_PERMISSION =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                android.Manifest.permission.READ_MEDIA_AUDIO
            else
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE
    }

    @Inject
    lateinit var preferences: Preferences

    private fun MediaMetaDataArtFactory() = object : Factory<Uri> {
        override fun create(data: Uri, options: Options, imageLoader: ImageLoader): Fetcher? {
            val isAlbumUri = let {
                if (data.authority != MediaStore.AUTHORITY) return@let false
                val segments = data.pathSegments
                val size = segments.size
                return@let size >= 3 && segments[size - 3] == "audio" && segments[size - 2] == "albums"
            }
            if (preferences.value(Settings.USE_LEGACY_ARTWORK_METHOD) && !isAlbumUri) return null
            return MediaMetaDataArtFetcher(data, options)
        }
    }


    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .components { add(MediaMetaDataArtFactory()) }
            .memoryCache {
                MemoryCache.Builder(this)
                    .strongReferencesEnabled(true)
                    .build()
            }
            .memoryCachePolicy(CachePolicy.ENABLED)
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache/coil"))
                    .maxSizeBytes(20_000)
                    .build()
            }
            .diskCachePolicy(CachePolicy.ENABLED)
            .build()
    }
}