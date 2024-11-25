package com.tangem.domain.onramp.model

import com.tangem.domain.tokens.model.Amount
import java.math.BigDecimal

sealed class OnrampQuote {

    data class Content(
        val fromAmount: BigDecimal,
        val toAmount: BigDecimal,
        val minFromAmount: BigDecimal,
        val maxFromAmount: BigDecimal,
        val paymentMethodId: String,
        val providerId: String,
    ) : OnrampQuote()

    sealed class Error : OnrampQuote() {

        data class AmountTooSmallError(val amount: Amount) : Error()

        data class AmountTooBigError(val amount: Amount) : Error()
    }
}
