package com.cosmin23.mediaplayer.image

import android.media.MediaMetadataRetriever
import android.os.Build
import android.util.Size
import androidx.core.graphics.drawable.toDrawable
import coil.ImageLoader
import coil.decode.DataSource
import coil.decode.ImageSource
import coil.fetch.DrawableResult
import coil.fetch.FetchResult
import coil.fetch.Fetcher
import coil.fetch.SourceResult
import coil.key.Keyer
import coil.request.Options
import com.cosmin23.mediaplayer.data.AudioArtwork
import okio.Buffer
import okio.buffer
import okio.source

/**
 * Resolves [AudioArtwork] to a bitmap through three fallbacks so artwork shows up reliably:
 *  1. the MediaStore album-art content uri (fast, cached by the system),
 *  2. `ContentResolver.loadThumbnail` on API 29+ (works when the album-art uri is empty),
 *  3. artwork embedded in the file, read with [MediaMetadataRetriever].
 */
class AudioArtworkFetcher(
    private val data: AudioArtwork,
    private val options: Options
) : Fetcher {

    override suspend fun fetch(): FetchResult? {
        val context = options.context
        val resolver = context.contentResolver

        // 1. Album-art content uri.
        data.albumArtUri?.let { artUri ->
            runCatching { resolver.openInputStream(artUri) }.getOrNull()?.let { stream ->
                return SourceResult(
                    source = ImageSource(stream.source().buffer(), context),
                    mimeType = null,
                    dataSource = DataSource.DISK
                )
            }
        }

        // 2. System thumbnail (API 29+).
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            runCatching {
                resolver.loadThumbnail(data.mediaUri, Size(512, 512), null)
            }.getOrNull()?.let { bitmap ->
                return DrawableResult(
                    drawable = bitmap.toDrawable(context.resources),
                    isSampled = false,
                    dataSource = DataSource.DISK
                )
            }
        }

        // 3. Embedded picture.
        val retriever = MediaMetadataRetriever()
        try {
            resolver.openFileDescriptor(data.mediaUri, "r")?.use { pfd ->
                retriever.setDataSource(pfd.fileDescriptor)
                retriever.embeddedPicture?.let { bytes ->
                    return SourceResult(
                        source = ImageSource(Buffer().apply { write(bytes) }, context),
                        mimeType = null,
                        dataSource = DataSource.DISK
                    )
                }
            }
        } catch (_: Throwable) {
            // fall through to null → placeholder
        } finally {
            runCatching { retriever.release() }
        }

        return null
    }

    class Factory : Fetcher.Factory<AudioArtwork> {
        override fun create(data: AudioArtwork, options: Options, imageLoader: ImageLoader): Fetcher =
            AudioArtworkFetcher(data, options)
    }
}

/** Stable cache key for [AudioArtwork] so the memory/disk caches work per track. */
class AudioArtworkKeyer : Keyer<AudioArtwork> {
    override fun key(data: AudioArtwork, options: Options): String =
        data.albumArtUri?.toString() ?: data.mediaUri.toString()
}
