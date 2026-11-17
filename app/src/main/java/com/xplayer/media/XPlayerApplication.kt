package com.xplayer.media

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.VideoFrameDecoder
import com.xplayer.media.data.local.AppDatabase
import com.xplayer.media.data.preferences.UserPreferencesRepository
import com.xplayer.media.data.repository.MediaRepository
import com.xplayer.media.data.repository.MediaStoreScanner

class XPlayerApplication : Application(), ImageLoaderFactory {

    val database: AppDatabase by lazy { AppDatabase.getInstance(this) }
    val preferencesRepository: UserPreferencesRepository by lazy { UserPreferencesRepository(this) }
    val mediaRepository: MediaRepository by lazy {
        MediaRepository(
            scanner = MediaStoreScanner(this),
            mediaDao = database.mediaDao()
        )
    }

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .components {
                add(VideoFrameDecoder.Factory())
            }
            .crossfade(true)
            .build()
    }
}
