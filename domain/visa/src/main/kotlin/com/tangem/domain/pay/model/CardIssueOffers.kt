package com.tangem.domain.pay.model

data class CardIssueOffers(
    val virtual: Offer?,
    val plastic: Offer?,
) {
    val hasAny: Boolean get() = virtual != null || plastic != null
}