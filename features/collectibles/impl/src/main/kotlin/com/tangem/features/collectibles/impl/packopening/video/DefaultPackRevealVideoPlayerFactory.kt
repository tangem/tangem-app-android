package com.tangem.features.collectibles.impl.packopening.video

import android.content.Context
import android.provider.Settings
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import com.tangem.features.collectibles.impl.packopening.video.source.PackRevealMediaSourceFactory
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

@OptIn(UnstableApi::class)
internal class DefaultPackRevealVideoPlayerFactory @Inject constructor(
    @ApplicationContext private val context: Context,
    private val mediaSourceFactory: PackRevealMediaSourceFactory,
) : PackRevealVideoPlayer.Factory {

    override fun create(listener: PackRevealVideoPlayer.Listener): PackRevealVideoPlayer {
        return DefaultPackRevealVideoPlayer(
            videoPlayer = buildPlayer(),
            soundPlayer = buildSoundPlayer(),
            mediaSourceFactory = mediaSourceFactory,
            isMotionEnabled = isMotionEnabled(),
            listener = listener,
        )
    }

    private fun buildPlayer(): ExoPlayer = ExoPlayer
        .Builder(
            context,
            DefaultRenderersFactory(context).setEnableDecoderFallback(true),
        )
        .build()
        .apply {
            repeatMode = Player.REPEAT_MODE_OFF
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                    .build(),
                true,
            )
            videoScalingMode = C.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING
        }

    private fun buildSoundPlayer(): ExoPlayer = ExoPlayer.Builder(context)
        .build()
        .apply {
            repeatMode = Player.REPEAT_MODE_OFF
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                    .build(),
                false,
            )
        }

    private fun isMotionEnabled(): Boolean = Settings.Global
        .getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) != 0f
}