package com.cosmin23.mediaplayer

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.cosmin23.mediaplayer.image.AudioArtworkFetcher
import com.cosmin23.mediaplayer.image.AudioArtworkKeyer

/**
 * Application entry point. Provides a shared Coil [ImageLoader] configured with the custom
 * artwork fetcher plus sensible memory/disk caches for smooth scrolling of large libraries.
 */
class MediaPlayerApp : Application(), ImageLoaderFactory {

    override fun newImageLoader(): ImageLoader =
        ImageLoader.Builder(this)
            .components {
                add(AudioArtworkKeyer())
                add(AudioArtworkFetcher.Factory())
            }
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.25)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("artwork_cache"))
                    .maxSizeBytes(60L * 1024 * 1024)
                    .build()
            }
            .crossfade(true)
            .build()
}
