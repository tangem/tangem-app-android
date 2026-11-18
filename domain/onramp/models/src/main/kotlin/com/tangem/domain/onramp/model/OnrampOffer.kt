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
 * The Recommended row an offer was picked for. Set where the row is chosen rather than read from
 * [OnrampOffer.advantages], because a sole or restricted-only offer takes the best row with no advantage badge.
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