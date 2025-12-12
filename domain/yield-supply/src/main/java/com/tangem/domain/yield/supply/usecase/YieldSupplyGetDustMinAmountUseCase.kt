package com.tangem.domain.yield.supply.usecase

import com.tangem.domain.appcurrency.model.AppCurrency
import java.math.BigDecimal

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