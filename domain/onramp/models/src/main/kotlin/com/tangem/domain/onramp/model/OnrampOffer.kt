package com.tangem.domain.onramp.model

import java.math.BigDecimal

data class OnrampOffersBlock(
    val category: OnrampOfferCategory,
    val offers: List<OnrampOffer>,
    val hasMoreOffers: Boolean,
)

data class OnrampOffer(
    val quote: OnrampQuote,
    val rateDif: BigDecimal?,
    val advantages: OnrampOfferAdvantages = OnrampOfferAdvantages.Default,
    val recommendedSlot: OnrampRecommendedSlot? = null,
)

/**
 * The Recommended row an offer was picked for, which a slot banner replaces. A sole offer, restricted ones included,
 * takes no row: it was not picked over anything, so a banner is shown next to it instead.
 */
enum class OnrampRecommendedSlot {
    BEST, FASTEST,
}

enum class OnrampOfferAdvantages {
    Default, BestRate, Fastest, GreatRate,
}

enum class OnrampOfferCategory {
    Recent, Recommended,
}