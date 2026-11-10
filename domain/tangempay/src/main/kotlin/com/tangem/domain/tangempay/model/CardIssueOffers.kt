package com.tangem.domain.tangempay.model

data class CardIssueOffers(
    val virtual: Offer?,
    val plastic: Offer?,
    val plasticArtworkUrl: String? = null,
) {
    val hasAny: Boolean get() = virtual != null || plastic != null
}