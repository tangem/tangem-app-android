package com.tangem.core.media

import android.content.Context
import android.os.Handler
import android.os.Looper
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import javax.inject.Inject

internal class DefaultOneShotVideoControllerFactory @Inject constructor(
    @ApplicationContext private val context: Context,
) : OneShotVideoController.Factory {

    private val mainHandler = Handler(Looper.getMainLooper())

    override fun create(
        scope: CoroutineScope,
        cache: RemoteClipCache,
        diagnosticsSubject: String,
        onCueReached: (OneShotVideoController.CueReason) -> Unit,
    ): OneShotVideoController = OneShotVideoController(
        scope = scope,
        cache = cache,
        diagnosticsSubject = diagnosticsSubject,
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