package com.tangem.core.media

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

@OptIn(UnstableApi::class)
internal class DefaultSoundClipFactory @Inject constructor(
    @ApplicationContext private val context: Context,
) : SoundClip.Factory {

    override fun create(cache: RemoteClipCache, diagnosticsSubject: String): SoundClip {
        val player = ExoPlayer.Builder(context)
            .setMediaSourceFactory(progressiveMediaSourceFactory(cache.dataSourceFactory))
            .build()
            .apply { setAudioAttributes(MovieAudioAttributes, /* handleAudioFocus = */ false) }
        return SoundClip(player = player, cache = cache, diagnosticsSubject = diagnosticsSubject)
    }
}