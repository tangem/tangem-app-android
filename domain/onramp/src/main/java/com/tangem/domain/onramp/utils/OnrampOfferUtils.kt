package com.tangem.domain.onramp.utils

import com.tangem.domain.onramp.model.OnrampQuote
import java.math.BigDecimal

internal fun calculateRateDif(currentTokenRate: BigDecimal, bestRate: BigDecimal?): BigDecimal? {
    if (bestRate == null) return null
    return BigDecimal.ONE - currentTokenRate / bestRate
}

internal fun compareOffersByRateSpeedAndPriority(isGooglePayAvailable: Boolean): Comparator<OnrampQuote.Data> {
    return Comparator { quote1, quote2 ->
        val rateComparison = quote1
            .toAmount
            .value
            .compareTo(quote2.toAmount.value)
        if (rateComparison != 0) return@Comparator rateComparison

        val speedComparison =
            quote2
                .paymentMethod
                .type
                .getProcessingSpeed()
                .speed
                .compareTo(quote1.paymentMethod.type.getProcessingSpeed().speed)
        if (speedComparison != 0) return@Comparator speedComparison

        quote1
            .paymentMethod
            .type
            .getPriority(isGooglePayAvailable)
            .compareTo(quote2.paymentMethod.type.getPriority(isGooglePayAvailable))
    }
}