package com.tangem.core.ui.components.artwork

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

@Immutable
data class ArtworkUM(
    val verifiedArtwork: ImmutableList<Byte>? = null,
    val defaultUrl: String,
) {

    constructor(bytes: ByteArray?, defaultUrl: String) : this(
        verifiedArtwork = bytes?.toList()?.toImmutableList(),
        defaultUrl = defaultUrl,
    )
}