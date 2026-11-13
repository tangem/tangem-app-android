package com.tangem.core.media

import android.content.Context
import androidx.annotation.OptIn
import androidx.annotation.RawRes
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.RawResourceDataSource
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.plus
import javax.inject.Inject

@OptIn(UnstableApi::class)
internal class DefaultLoopingVideoControllerFactory @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dispatchers: CoroutineDispatcherProvider,
) : LoopingVideoController.Factory {

    override fun create(
        scope: CoroutineScope,
        @RawRes videoRes: Int,
        frameSize: VideoFrameSize,
        diagnosticsSubject: String,
        onFailed: (error: Throwable, hasRenderedFrame: Boolean) -> Unit,
    ): LoopingVideoController = LoopingVideoController(
        // The player only takes calls on the main thread.
        scope = scope + dispatchers.mainImmediate,
        frameSize = frameSize,
        diagnosticsSubject = diagnosticsSubject,
        onFailed = onFailed,
        createPlayer = { listener ->
            createPlayer(
                videoItem = MediaItem.fromUri(RawResourceDataSource.buildRawResourceUri(videoRes)),
                isSourceReady = true,
                dataSourceFactory = null,
                diagnosticsSubject = diagnosticsSubject,
                listener = listener,
            )
        },
    )

    override fun createRemote(
        scope: CoroutineScope,
        url: String,
        cache: RemoteClipCache,
        frameSize: VideoFrameSize,
        diagnosticsSubject: String,
        onFailed: (error: Throwable, hasRenderedFrame: Boolean) -> Unit,
    ): LoopingVideoController = LoopingVideoController(
        // The player only takes calls on the main thread.
        scope = scope + dispatchers.mainImmediate,
        frameSize = frameSize,
        diagnosticsSubject = diagnosticsSubject,
        onFailed = onFailed,
        loadSource = { cache.downloadIfMissing(url) },
        createPlayer = { listener ->
            createPlayer(
                videoItem = MediaItem.fromUri(url),
                isSourceReady = false,
                dataSourceFactory = cache.dataSourceFactory,
                diagnosticsSubject = diagnosticsSubject,
                listener = listener,
            )
        },
    )

    private fun createPlayer(
        videoItem: MediaItem,
        isSourceReady: Boolean,
        dataSourceFactory: DataSource.Factory?,
        diagnosticsSubject: String,
        listener: LoopingVideoPlayer.Listener,
    ): LoopingVideoPlayer {
        val player = buildVideoPlayer(
            context = context,
            diagnosticsSubject = diagnosticsSubject,
            dataSourceFactory = dataSourceFactory,
        )
        return DefaultLoopingVideoPlayer(
            player = player,
            decoderCounters = { player.videoDecoderCounters },
            videoItem = videoItem,
            isMotionEnabled = context.isSystemAnimationsEnabled(),
            isSourceReady = isSourceReady,
            listener = listener,
            teardownScheduler = MainThreadVideoTeardownScheduler(),
        )
    }
}