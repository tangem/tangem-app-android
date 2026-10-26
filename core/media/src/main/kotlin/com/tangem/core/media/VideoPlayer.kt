package com.tangem.core.media

import android.content.Context
import android.provider.Settings
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.ProgressiveMediaSource

internal enum class VideoAudio {
    Muted,

    /** Plays over other audio instead of pausing it. */
    MixedWithOthers,
}

/** @param dataSourceFactory `null` reads through media3's default sources */
@OptIn(UnstableApi::class)
internal fun buildVideoPlayer(
    context: Context,
    diagnosticsSubject: String,
    audio: VideoAudio = VideoAudio.Muted,
    dataSourceFactory: DataSource.Factory? = null,
): ExoPlayer = ExoPlayer
    .Builder(
        context,
        // Some vendor decoders refuse a file the software one accepts.
        DefaultRenderersFactory(context).setEnableDecoderFallback(true),
    )
    .apply { if (dataSourceFactory != null) setMediaSourceFactory(progressiveMediaSourceFactory(dataSourceFactory)) }
    .build()
    .apply {
        // No audio focus: a video must not pause the user's music.
        when (audio) {
            VideoAudio.Muted -> {
                setAudioAttributes(AudioAttributes.DEFAULT, /* handleAudioFocus = */ false)
                volume = 0f
            }
            VideoAudio.MixedWithOthers -> setAudioAttributes(MovieAudioAttributes, /* handleAudioFocus = */ false)
        }
        videoScalingMode = C.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING
        addAnalyticsListener(VideoPlaybackDiagnostics(subject = diagnosticsSubject))
    }

/** A remote clip is one file; the default factory guesses the source by address and throws on a stream one. */
@OptIn(UnstableApi::class)
internal fun progressiveMediaSourceFactory(dataSourceFactory: DataSource.Factory): ProgressiveMediaSource.Factory =
    ProgressiveMediaSource.Factory(dataSourceFactory)

internal val MovieAudioAttributes: AudioAttributes = AudioAttributes.Builder()
    .setUsage(C.USAGE_MEDIA)
    .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
    .build()

/** The closest Android has to Reduce Motion. */
internal fun Context.isSystemAnimationsEnabled(): Boolean =
    Settings.Global.getFloat(contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) != 0f