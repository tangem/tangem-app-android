package com.tangem.features.collectibles.impl.packopening.video.source

import androidx.media3.exoplayer.source.MediaSource

/** Turns a reveal URL into a media source; an interface so the player can be tested on the JVM without media3. */
internal interface PackRevealMediaSourceFactory {

    /** Downloads [url] in full; throws when it cannot be fetched or is not media. */
    suspend fun create(url: String): MediaSource
}