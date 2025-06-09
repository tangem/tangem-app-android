package com.tangem.domain.tokens.model.warnings

import com.tangem.domain.models.currency.CryptoCurrency
import java.math.BigDecimal

sealed class KaspaWarnings : CryptoCurrencyWarning() {

    abstract val currency: CryptoCurrency

    data class IncompleteTransaction(
        override val currency: CryptoCurrency,
        val amount: BigDecimal,
        val currencySymbol: String,
        val currencyDecimals: Int,
    ) : KaspaWarnings()
}