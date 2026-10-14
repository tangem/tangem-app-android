package com.tangem.features.collectibles.impl.packopening.ui.state

/** @property accent colours the case's glow and the rarity badge by how rare the card is */
internal data class RewardCardUM(
    val rarity: String,
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