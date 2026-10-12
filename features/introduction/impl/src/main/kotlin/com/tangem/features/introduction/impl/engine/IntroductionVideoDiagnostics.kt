package com.tangem.features.introduction.impl.engine

import androidx.annotation.OptIn
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.analytics.AnalyticsListener
import com.tangem.utils.logging.TangemLogger

/**
 * Records signals that distinguish the causes of a frozen welcome video: the decoder selected by the device,
 * frames dropped by the renderer, and the player leaving the ready state. Freezes reproduce only on some devices,
 * and without these entries their logs show nothing about the failure.
 */
@OptIn(UnstableApi::class)
internal class IntroductionVideoDiagnostics : AnalyticsListener {

    override fun onVideoDecoderInitialized(
        eventTime: AnalyticsListener.EventTime,
        decoderName: String,
        initializedTimestampMs: Long,
        initializationDurationMs: Long,
    ) {
        TangemLogger.i("Welcome video decoder $decoderName initialized in ${initializationDurationMs}ms")
    }

    override fun onDroppedVideoFrames(eventTime: AnalyticsListener.EventTime, droppedFrames: Int, elapsedMs: Long) {
        TangemLogger.i(
            "Welcome video dropped $droppedFrames frames in ${elapsedMs}ms " +
                "at ${eventTime.eventPlaybackPositionMs}ms",
        )
    }

    /** Also fires after every change of the output, which is how a surface swapped under a playing video shows. */
    override fun onRenderedFirstFrame(eventTime: AnalyticsListener.EventTime, output: Any, renderTimeMs: Long) {
        TangemLogger.i("Welcome video first frame at ${eventTime.eventPlaybackPositionMs}ms")
    }

    /** 0x0 means the player has no surface to draw into. */
    override fun onSurfaceSizeChanged(eventTime: AnalyticsListener.EventTime, width: Int, height: Int) {
        TangemLogger.i("Welcome video surface ${width}x$height at ${eventTime.eventPlaybackPositionMs}ms")
    }

    override fun onPlayWhenReadyChanged(eventTime: AnalyticsListener.EventTime, playWhenReady: Boolean, reason: Int) {
        val action = if (playWhenReady) "play" else "pause"
        TangemLogger.i("Welcome video $action at ${eventTime.eventPlaybackPositionMs}ms")
    }

    override fun onPlaybackStateChanged(eventTime: AnalyticsListener.EventTime, state: @Player.State Int) {
        TangemLogger.i("Welcome video state ${stateName(state)} at ${eventTime.eventPlaybackPositionMs}ms")
    }

    override fun onVideoCodecError(eventTime: AnalyticsListener.EventTime, videoCodecError: Exception) {
        TangemLogger.e("Welcome video codec failed at ${eventTime.eventPlaybackPositionMs}ms", videoCodecError)
    }

    private fun stateName(state: @Player.State Int): String = when (state) {
        Player.STATE_IDLE -> "IDLE"
        Player.STATE_BUFFERING -> "BUFFERING"
        Player.STATE_READY -> "READY"
        Player.STATE_ENDED -> "ENDED"
        else -> state.toString()
    }
}