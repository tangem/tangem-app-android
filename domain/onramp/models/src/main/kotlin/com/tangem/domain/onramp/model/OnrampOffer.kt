package com.tangem.domain.onramp.model

data class OnrampOffersBlock(
    val category: OnrampOfferCategory,
    val offers: List<OnrampOffer>,
    val isVisible: Boolean = offers.isNotEmpty(),
)

data class OnrampOffer(
    val quote: OnrampQuote,
    val advantages: OnrampOfferAdvantages = OnrampOfferAdvantages.Default,
)

enum class OnrampOfferAdvantages {
    Default, BestRate, Fastest,
}

enum class OnrampOfferCategory {
    Recent, Recommended,
}