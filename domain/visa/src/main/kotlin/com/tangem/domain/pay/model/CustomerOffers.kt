package com.tangem.domain.pay.model

data class CustomerOffers(
    val orderable: List<Offer>,
    val artwork: Map<Offer.Type, String>,
) {

    companion object {
        val EMPTY = CustomerOffers(orderable = emptyList(), artwork = emptyMap())
    }
}