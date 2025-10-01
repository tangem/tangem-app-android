package com.tangem.domain.onramp.model

import java.math.BigDecimal

data class OnrampPaymentMethodGroup(
    val paymentMethod: OnrampPaymentMethod,
    val offers: List<OnrampOffer>,
    val bestRateOffer: OnrampOffer?,
    val providerCount: Int,
    val isBestPaymentMethod: Boolean,
) {

    val bestRateAmount: BigDecimal? = bestRateOffer?.let { offer ->
        when (val quote = offer.quote) {
            is OnrampQuote.Data -> quote.toAmount.value
            else -> BigDecimal.ZERO
        }
    }
}