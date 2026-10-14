package com.tangem.features.collectibles.impl.packopening.video.source

/**
 * One reveal video, its sound muxed in.
 *
 * @property cardRevealMs   when the pack's animation ends and the card takes over; the video keeps playing under it
 * @property skipToMs where a tap jumps to: the first frame of the burst, on a keyframe, default 6100
 */
internal data class PackRevealAsset(
    val videoUrl: String,
    val cardRevealMs: Long = 7_000L,
    val skipToMs: Long = 6_100L,
)