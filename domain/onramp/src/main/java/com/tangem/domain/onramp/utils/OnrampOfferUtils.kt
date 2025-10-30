package com.tangem.domain.onramp.utils

import com.tangem.domain.onramp.model.OnrampQuote
import com.tangem.domain.onramp.model.PaymentMethodType
import java.math.BigDecimal

internal fun calculateRateDif(currentTokenRate: BigDecimal, bestRate: BigDecimal?): BigDecimal? {
    if (bestRate == null) return null
    if (currentTokenRate > bestRate) return null

    val rateDif = BigDecimal.ONE - currentTokenRate / bestRate
    return if (rateDif >= BigDecimal("0.0001")) rateDif else null
}

/**
 * @param isSepaPrioritized Sepa provider should be prioritized over all offers no matter what.
 */
internal fun compareOffersByRateSpeedAndPriority(
    isGooglePayAvailable: Boolean,
    isSepaPrioritized: Boolean = false,
): Comparator<OnrampQuote.Data> {
    return Comparator { quote1, quote2 ->
        if (isSepaPrioritized) {
            val isQuote1Sepa = quote1.paymentMethod.type == PaymentMethodType.SEPA
            val isQuote2Sepa = quote2.paymentMethod.type == PaymentMethodType.SEPA

            if (isQuote1Sepa && !isQuote2Sepa) return@Comparator 1
            if (!isQuote1Sepa && isQuote2Sepa) return@Comparator -1
        }

        val rateComparison = quote1.toAmount.value.compareTo(quote2.toAmount.value)
        if (rateComparison != 0) return@Comparator rateComparison

        val speedComparison =
            quote2.paymentMethod.type.getProcessingSpeed().speed
                .compareTo(quote1.paymentMethod.type.getProcessingSpeed().speed)
        if (speedComparison != 0) return@Comparator speedComparison

        quote2.paymentMethod.type.getPriorityBySpeed(isGooglePayAvailable)
            .compareTo(quote1.paymentMethod.type.getPriorityBySpeed(isGooglePayAvailable))
    }
}