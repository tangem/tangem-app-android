package com.tangem.core.media

import android.content.Context
import android.provider.Settings
import androidx.annotation.OptIn
import androidx.annotation.RawRes
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
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
        createPlayer = { listener -> createPlayer(videoRes, diagnosticsSubject, listener) },
    )

    private fun createPlayer(
        @RawRes videoRes: Int,
        diagnosticsSubject: String,
        listener: LoopingVideoPlayer.Listener,
    ): LoopingVideoPlayer {
        val player = buildVideoPlayer(context = context, diagnosticsSubject = diagnosticsSubject)
        return DefaultLoopingVideoPlayer(
            player = player,
            decoderCounters = { player.videoDecoderCounters },
            videoItem = MediaItem.fromUri(RawResourceDataSource.buildRawResourceUri(videoRes)),
            isMotionEnabled = context.isSystemAnimationsEnabled(),
            listener = listener,
            teardownScheduler = MainThreadVideoTeardownScheduler(),
        )
    }

    // The closest Android has to Reduce Motion.
    private fun Context.isSystemAnimationsEnabled(): Boolean =
        Settings.Global.getFloat(contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) != 0f
}