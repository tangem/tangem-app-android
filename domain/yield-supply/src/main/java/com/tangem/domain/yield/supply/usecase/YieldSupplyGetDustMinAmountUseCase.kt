package com.tangem.domain.yield.supply.usecase

import com.tangem.domain.appcurrency.model.AppCurrency
import java.math.BigDecimal

/**
 * Use case for getting dust minimum amount for yield supply.
 *
 * Returns the minimum amount based on the selected [AppCurrency].
 * If [AppCurrency] is in [SUPPORTED_DUST_CURRENCIES], returns [DUST_MIN_AMOUNT],
 * otherwise returns the original [minAmount] with trailing zeros stripped.
 */
class YieldSupplyGetDustMinAmountUseCase {

    operator fun invoke(minAmount: BigDecimal, appCurrency: AppCurrency): BigDecimal {
        return if (appCurrency.code in SUPPORTED_DUST_CURRENCIES) {
            DUST_MIN_AMOUNT
        } else {
            minAmount.stripTrailingZeros()
        }
    }

    companion object {
        private val DUST_MIN_AMOUNT = BigDecimal("0.1")
        private val SUPPORTED_DUST_CURRENCIES = setOf("EUR", "USD", "AUD", "CAD", "GBP")
    }
}