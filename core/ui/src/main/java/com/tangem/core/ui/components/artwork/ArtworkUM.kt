package com.tangem.core.ui.components.artwork

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.ImageBitmap

@Immutable
data class ArtworkUM(
    val verifiedArtwork: ImageBitmap? = null,
    val defaultUrl: String,
)