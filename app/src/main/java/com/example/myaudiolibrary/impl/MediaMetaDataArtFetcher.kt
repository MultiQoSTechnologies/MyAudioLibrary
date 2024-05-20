package com.example.myaudiolibrary.impl

import android.content.ContentUris
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.MediaStore
import androidx.core.graphics.drawable.toDrawable
import coil.decode.DataSource
import coil.decode.DecodeUtils
import coil.fetch.DrawableResult
import coil.fetch.FetchResult
import coil.fetch.Fetcher
import coil.request.Options
import coil.size.Dimension
import com.example.myaudiolibrary.R
import com.example.myaudiolibrary.core.db.query2

private var DEFAULT_RESULT: FetchResult? = null

class MediaMetaDataArtFetcher(
    private val data: Uri,
    private val options: Options
) : Fetcher {
    override suspend fun fetch(): FetchResult {
        if (DEFAULT_RESULT == null) {
            DEFAULT_RESULT = DrawableResult(
                options.context.getDrawable(R.drawable.ic_default_music_icon)!!,
                isSampled = false,
                dataSource = DataSource.MEMORY
            )
        }
        val resolver = options.context.contentResolver
        val projection = arrayOf(MediaStore.Audio.Media.DATA)
        val selection = "${MediaStore.Audio.Media.ALBUM_ID} == ${ContentUris.parseId(data)}"
        val parent = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI

        val data = resolver.query2(parent, projection, selection, null, limit = 1).use {
            if (it == null || !it.moveToFirst())
                return DEFAULT_RESULT!!
            it.getString(0) ?: return DEFAULT_RESULT!!
        }

        return MediaMetadataRetriever().use { retriever ->
            retriever.setDataSource(data)
            val bytes = retriever.embeddedPicture ?: return DEFAULT_RESULT!!
            var isSampled: Boolean
            val bitmap = BitmapFactory.Options().let {
                it.inJustDecodeBounds = true
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size, it)
                val width = (options.size.width as? Dimension.Pixels)?.px ?: it.outWidth
                val height = (options.size.height as? Dimension.Pixels)?.px ?: it.outHeight
                isSampled = if (it.outWidth > 0 && it.outHeight > 0) {
                    DecodeUtils.computeSizeMultiplier(
                        srcWidth = it.outWidth,
                        srcHeight = it.outHeight,
                        dstWidth = width,
                        dstHeight = height,
                        scale = options.scale
                    ) < 1.0
                } else {
                    true
                }
                it.inSampleSize = DecodeUtils.calculateInSampleSize(
                    it.outWidth,
                    it.outHeight,
                    width,
                    height,
                    options.scale
                )

                it.inJustDecodeBounds = false

                BitmapFactory.decodeByteArray(bytes, 0, bytes.size, it)
            }

            DrawableResult(
                drawable = bitmap.toDrawable(options.context.resources),
                isSampled = isSampled,
                dataSource = DataSource.DISK
            )
        }
    }
}