package com.tangem.features.collectibles.impl.packopening.ui.state

import com.tangem.core.ui.extensions.TextReference
import com.tangem.features.collectibles.impl.common.ui.rewardcard.state.RewardCardAccent

/** @property accent colours the case's glow and the rarity badge by how rare the card is */
internal data class RewardCardUM(
    val rarity: TextReference,
    val accent: RewardCardAccent,
    val title: String,
    val editionNumber: String,
    val marketValue: String,
    val frontImageUrl: String? = null,
    val backImageUrl: String? = null,
)