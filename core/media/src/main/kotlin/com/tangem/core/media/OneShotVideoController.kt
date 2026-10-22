package com.tangem.core.media

import android.view.SurfaceView
import com.tangem.utils.logging.TangemLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * Plays a remote clip once and reports reaching its cue frame, or that it never will, so a waiting screen is never
 * stuck. A failed, stalled or ended clip stays stopped.
 *
 * @property isMotionEnabled `false` if animations were off system-wide at creation: nothing loads or plays
 */
class OneShotVideoController internal constructor(
    private val scope: CoroutineScope,
    private val cache: RemoteClipCache,
    private val diagnosticsSubject: String,
    val isMotionEnabled: Boolean,
    private val onCueReached: (CueReason) -> Unit,
    createPlayer: (OneShotVideoPlayer.Listener) -> OneShotVideoPlayer,
) {

    private val player = createPlayer(PlayerListener())

    val loadState: StateFlow<VideoLoadState>
        field = MutableStateFlow(VideoLoadState.Loading)

    private var playback = Playback.Idle
    private var isRunning = true
    private var stallWatch: Job? = null

    fun attachSurface(surfaceView: SurfaceView) {
        player.attachSurface(surfaceView)
    }

    fun detachSurface(surfaceView: SurfaceView) {
        player.detachSurface(surfaceView)
    }

    /**
     * Downloads [url] in full and prepares it to cue at [cueMs]; `null` fails at once. Ignored after [start], never
     * throws but to cancel; cancel an unfinished call before the next.
     */
    suspend fun prepare(url: String?, cueMs: Long) {
        if (playback != Playback.Idle) return
        if (url == null || !isMotionEnabled) {
            loadState.value = VideoLoadState.Failed
            return
        }

        // The previous clip must not report ready during the download.
        player.stop()
        loadState.value = VideoLoadState.Loading
        if (!cache.ensureCached(url = url, diagnosticsSubject = diagnosticsSubject)) {
            loadState.value = VideoLoadState.Failed
            return
        }
        // A start during the download has already reported NotReady.
        if (playback == Playback.Idle) player.prepare(url = url, cueMs = cueMs)
    }

    /** Without a ready clip, reports the cue at once. */
    fun start() {
        if (playback != Playback.Idle) return
        playback = Playback.Started

        if (loadState.value != VideoLoadState.Ready) {
            reachCue(CueReason.NotReady)
            return
        }

        if (isRunning) player.play()
        watchStalls()
    }

    /** Jumps to [positionMs] and mutes; `false` unless playing towards the cue and before [positionMs]. */
    fun skipTo(positionMs: Long): Boolean {
        if (playback != Playback.Started || player.positionMs >= positionMs) return false

        player.seekTo(positionMs)
        player.mute()
        return true
    }

    /** Whether the screen is in front; in the background the clip and its stall clock pause. */
    fun setRunning(isRunning: Boolean) {
        if (this.isRunning == isRunning) return
        this.isRunning = isRunning
        if (!playback.isUnderway) return

        if (isRunning) player.play() else player.pause()
        watchStalls()
    }

    fun release() {
        stallWatch?.cancel()
        playback = Playback.Stopped
        player.release()
    }

    /** From scratch, so a pause never counts towards a stall. */
    private fun watchStalls() {
        stallWatch?.cancel()
        stallWatch = if (playback == Playback.Started && isRunning) scope.launch { awaitStall() } else null
    }

    private suspend fun awaitStall() {
        var lastCount = player.presentedFrameCount
        var stalledMs = 0L
        while (stalledMs < STALL_TIMEOUT_MS) {
            delay(CHECK_INTERVAL_MS)
            val count = player.presentedFrameCount
            stalledMs = if (count == lastCount) stalledMs + CHECK_INTERVAL_MS else 0L
            lastCount = count
        }
        TangemLogger.e("$diagnosticsSubject showed no new frame for ${STALL_TIMEOUT_MS}ms at ${player.positionMs}ms")
        reachCue(CueReason.Stalled)
    }

    private fun reachCue(reason: CueReason) {
        if (playback != Playback.Started) return

        playback = if (reason == CueReason.Shown) Playback.PastCue else Playback.Stopped
        stallWatch?.cancel()
        if (playback == Playback.Stopped) player.pause()
        onCueReached(reason)
    }

    private inner class PlayerListener : OneShotVideoPlayer.Listener {

        override fun onReady() {
            if (loadState.value == VideoLoadState.Loading) loadState.value = VideoLoadState.Ready
        }

        override fun onCueFramePresented() = reachCue(CueReason.Shown)

        override fun onEnded() {
            if (playback == Playback.PastCue) playback = Playback.Stopped
            reachCue(CueReason.Ended)
        }

        override fun onError(error: Throwable) {
            TangemLogger.e("$diagnosticsSubject failed", error)
            loadState.value = VideoLoadState.Failed
            if (playback == Playback.PastCue) playback = Playback.Stopped
            reachCue(CueReason.Failed)
        }
    }

    enum class CueReason {
        /** A frame at the cue is on screen. */
        Shown,

        /** The clip ended before showing one. */
        Ended,

        Failed,

        /** No new frame for 3 s while the screen is in front. */
        Stalled,

        /** [start] came before the clip was ready. */
        NotReady,
    }

    private enum class Playback(val isUnderway: Boolean) {
        Idle(isUnderway = false),
        Started(isUnderway = true),
        PastCue(isUnderway = true),
        Stopped(isUnderway = false),
    }

    interface Factory {

        /**
         * @param diagnosticsSubject what the log entries are about, e.g. "Pack reveal video"
         * @param onCueReached       once, on the main thread, only after [start]
         */
        fun create(
            scope: CoroutineScope,
            cache: RemoteClipCache,
            diagnosticsSubject: String,
            onCueReached: (CueReason) -> Unit,
        ): OneShotVideoController
    }

    private companion object {
        const val CHECK_INTERVAL_MS = 250L
        const val STALL_TIMEOUT_MS = 3_000L
    }
}