package com.tangem.features.introduction.impl.engine

import android.content.Context
import android.provider.Settings
import androidx.annotation.OptIn
import androidx.annotation.RawRes
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.RawResourceDataSource
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

@OptIn(UnstableApi::class)
internal class DefaultIntroductionVideoPlayerFactory @Inject constructor(
    @ApplicationContext private val context: Context,
) : IntroductionVideoPlayer.Factory {

    override fun create(@RawRes videoRes: Int, listener: IntroductionVideoPlayer.Listener): IntroductionVideoPlayer {
        return DefaultIntroductionVideoPlayer(
            player = buildPlayer(),
            videoItem = MediaItem.fromUri(RawResourceDataSource.buildRawResourceUri(videoRes)),
            isMotionEnabled = isMotionEnabled(),
            listener = listener,
        )
    }

    private fun buildPlayer(): ExoPlayer = ExoPlayer
        .Builder(
            context,
            // Some vendor decoders refuse a file the software one accepts.
            DefaultRenderersFactory(context).setEnableDecoderFallback(true),
        )
        .build()
        .apply {
            // Holds even if a re-export of the file ever ships an audio track.
            setAudioAttributes(AudioAttributes.DEFAULT, /* handleAudioFocus = */ false)
            volume = 0f
            // Resizing a SurfaceView instead is one of the few things that still flashes black.
            videoScalingMode = C.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING
        }

    private fun isMotionEnabled(): Boolean = Settings.Global
        .getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) != 0f
}