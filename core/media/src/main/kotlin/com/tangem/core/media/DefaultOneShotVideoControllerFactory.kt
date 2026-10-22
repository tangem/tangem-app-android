package com.tangem.core.media

import android.content.Context
import android.os.Handler
import android.os.Looper
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.plus
import javax.inject.Inject

internal class DefaultOneShotVideoControllerFactory @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dispatchers: CoroutineDispatcherProvider,
) : OneShotVideoController.Factory {

    private val mainHandler = Handler(Looper.getMainLooper())

    override fun create(
        scope: CoroutineScope,
        cache: RemoteClipCache,
        diagnosticsSubject: String,
        stallTimeoutMs: Long,
        onCueReached: (OneShotVideoController.CueReason) -> Unit,
    ): OneShotVideoController = OneShotVideoController(
        // The player only takes calls on the main thread.
        scope = scope + dispatchers.mainImmediate,
        cache = cache,
        diagnosticsSubject = diagnosticsSubject,
        stallTimeoutMs = stallTimeoutMs,
        isMotionEnabled = context.isSystemAnimationsEnabled(),
        onCueReached = onCueReached,
        createPlayer = { listener ->
            DefaultOneShotVideoPlayer(
                player = buildVideoPlayer(
                    context = context,
                    diagnosticsSubject = diagnosticsSubject,
                    audio = VideoAudio.MixedWithOthers,
                    dataSourceFactory = cache.dataSourceFactory,
                ),
                listener = listener,
                teardownScheduler = MainThreadVideoTeardownScheduler(),
                postToMain = { action -> mainHandler.post(action) },
            )
        },
    )
}