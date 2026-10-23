package com.tangem.features.collectibles.impl.packopening.model

import com.tangem.domain.collectibles.models.CollectibleBuybackOffer

internal sealed interface BuybackState {

    data class Available(val offer: CollectibleBuybackOffer) : BuybackState

    data object Unavailable : BuybackState

    data class InProgress(val offer: CollectibleBuybackOffer) : BuybackState
}