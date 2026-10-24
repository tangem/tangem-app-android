package com.tangem.features.collectibles.impl.packopening.video

import com.tangem.core.media.OneShotVideoController
import com.tangem.core.media.SoundClip
import com.tangem.features.collectibles.impl.packopening.video.source.PackRevealAssetCache
import kotlinx.coroutines.CoroutineScope
import javax.inject.Inject

internal class DefaultPackRevealVideoPlayerFactory @Inject constructor(
    private val cache: PackRevealAssetCache,
    private val videoFactory: OneShotVideoController.Factory,
    private val soundFactory: SoundClip.Factory,
) : PackRevealVideoPlayer.Factory {

    override fun create(scope: CoroutineScope, listener: PackRevealVideoPlayer.Listener): PackRevealVideoPlayer =
        DefaultPackRevealVideoPlayer(
            scope = scope,
            cache = cache,
            videoFactory = videoFactory,
            soundFactory = soundFactory,
            listener = listener,
        )
}