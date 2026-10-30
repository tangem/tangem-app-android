package com.tangem.core.media

import androidx.annotation.OptIn
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.analytics.AnalyticsListener
import com.tangem.utils.logging.TangemLogger

/** Logs what tells the causes of a frozen video apart: freezes reproduce only on some devices. */
@OptIn(UnstableApi::class)
internal class VideoPlaybackDiagnostics(private val subject: String) : AnalyticsListener {

    override fun onVideoDecoderInitialized(
        eventTime: AnalyticsListener.EventTime,
        decoderName: String,
        initializedTimestampMs: Long,
        initializationDurationMs: Long,
    ) {
        TangemLogger.i("$subject decoder $decoderName initialized in ${initializationDurationMs}ms")
    }

    override fun onDroppedVideoFrames(eventTime: AnalyticsListener.EventTime, droppedFrames: Int, elapsedMs: Long) {
        TangemLogger.i(
            "$subject dropped $droppedFrames frames in ${elapsedMs}ms at ${eventTime.eventPlaybackPositionMs}ms",
        )
    }

    /** Also fires after every surface change. */
    override fun onRenderedFirstFrame(eventTime: AnalyticsListener.EventTime, output: Any, renderTimeMs: Long) {
        TangemLogger.i("$subject first frame at ${eventTime.eventPlaybackPositionMs}ms")
    }

    /** 0x0 means no surface. */
    override fun onSurfaceSizeChanged(eventTime: AnalyticsListener.EventTime, width: Int, height: Int) {
        TangemLogger.i("$subject surface ${width}x$height at ${eventTime.eventPlaybackPositionMs}ms")
    }

    override fun onPlayWhenReadyChanged(eventTime: AnalyticsListener.EventTime, playWhenReady: Boolean, reason: Int) {
        val action = if (playWhenReady) "play" else "pause"
        TangemLogger.i("$subject $action at ${eventTime.eventPlaybackPositionMs}ms")
    }

    override fun onPlaybackStateChanged(eventTime: AnalyticsListener.EventTime, state: @Player.State Int) {
        TangemLogger.i("$subject state ${stateName(state)} at ${eventTime.eventPlaybackPositionMs}ms")
    }

    override fun onVideoCodecError(eventTime: AnalyticsListener.EventTime, videoCodecError: Exception) {
        TangemLogger.e("$subject codec failed at ${eventTime.eventPlaybackPositionMs}ms", videoCodecError)
    }

    private fun stateName(state: @Player.State Int): String = when (state) {
        Player.STATE_IDLE -> "IDLE"
        Player.STATE_BUFFERING -> "BUFFERING"
        Player.STATE_READY -> "READY"
        Player.STATE_ENDED -> "ENDED"
        else -> state.toString()
    }
}