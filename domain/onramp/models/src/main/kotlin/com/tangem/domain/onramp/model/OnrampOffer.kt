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
)

enum class OnrampOfferAdvantages {
    Default, BestRate, Fastest,
}

enum class OnrampOfferCategory {
    Recent, Recommended,
}