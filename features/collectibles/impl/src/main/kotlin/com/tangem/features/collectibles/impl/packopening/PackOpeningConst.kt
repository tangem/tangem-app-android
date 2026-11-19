package com.tangem.features.collectibles.impl.packopening

internal object PackOpeningConst {

    /** How long the pack waits for the reveal video before it throws the short version instead. */
    const val VIDEO_READY_TIMEOUT_MS = 5_000L

    /** How long the reveal video may show no new frame before the card takes over. */
    const val VIDEO_STALL_TIMEOUT_MS = 2_000L

    const val CARD_IMAGES_TIMEOUT_MS = 3_000L
}