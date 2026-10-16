package com.tangem.features.collectibles.impl.packopening.video.source

import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import javax.inject.Inject

@OptIn(UnstableApi::class)
internal class DefaultPackRevealMediaSourceFactory @Inject constructor(
    private val cache: PackRevealAssetCache,
) : PackRevealMediaSourceFactory {

    override suspend fun create(url: String): MediaSource {
        if (!cache.isFullyCached(url)) cache.download(url)

        return ProgressiveMediaSource.Factory(cache.dataSourceFactory).createMediaSource(MediaItem.fromUri(url))
    }
}