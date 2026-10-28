package com.tangem.core.media

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer

@OptIn(UnstableApi::class)
internal fun buildVideoPlayer(context: Context, diagnosticsSubject: String): ExoPlayer = ExoPlayer
    .Builder(
        context,
        // Some vendor decoders refuse a file the software one accepts.
        DefaultRenderersFactory(context).setEnableDecoderFallback(true),
    )
    .build()
    .apply {
        // A decorative clip must not pause the user's music.
        setAudioAttributes(AudioAttributes.DEFAULT, /* handleAudioFocus = */ false)
        volume = 0f
        videoScalingMode = C.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING
        addAnalyticsListener(VideoPlaybackDiagnostics(subject = diagnosticsSubject))
    }