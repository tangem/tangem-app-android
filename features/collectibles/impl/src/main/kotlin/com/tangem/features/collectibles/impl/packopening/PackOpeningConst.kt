package com.tangem.features.collectibles.impl.packopening

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.unit.dp

internal object PackOpeningConst {

    /** How long the pack waits for the reveal video before it throws the short version instead. */
    const val VIDEO_READY_TIMEOUT_MS = 5_000L

    /** How long the short version waits for the card's images before it throws the pack anyway. */
    const val CARD_IMAGES_TIMEOUT_MS = 3_000L

    /** `cameraDistance` is in camera inches, projected at 72 px each. */
    const val CAMERA_PX_PER_INCH = 72f

    /** Rail the case leaves around the card (Figma `3558:70925`, a 260 x 406 card in a 280 x 426 case). */
    val SLAB_INSET = PaddingValues(all = 10.dp)
}