package com.tangem.features.collectibles.impl.packopening.ui.state

import com.tangem.core.ui.extensions.TextReference

/** @property accent colours the case's glow and the rarity badge by how rare the card is */
internal data class RewardCardUM(
    val rarity: TextReference,
    val accent: Accent,
    val title: String,
    val editionNumber: String,
    val marketValue: String,
    val sellBackValue: String,
    val frontImageUrl: String? = null,
    val backImageUrl: String? = null,
) {

    enum class Accent { Neutral, Green, Blue, Violet }
}