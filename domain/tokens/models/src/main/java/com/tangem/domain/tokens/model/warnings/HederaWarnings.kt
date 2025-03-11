package com.tangem.domain.tokens.model.warnings

import com.tangem.domain.tokens.model.CryptoCurrency
import java.math.BigDecimal

sealed class HederaWarnings : CryptoCurrencyWarning() {

    abstract val currency: CryptoCurrency

    data class AssociateWarning(override val currency: CryptoCurrency) : HederaWarnings()

    data class AssociateWarningWithFee(
        override val currency: CryptoCurrency,
        val fee: BigDecimal,
        val feeCurrencySymbol: String,
        val feeCurrencyDecimals: Int,
    ) : HederaWarnings()
}