package com.tangem.domain.onramp.model

import com.tangem.domain.tokens.model.Amount
import java.math.BigDecimal

sealed class OnrampQuote {

    abstract val paymentMethod: OnrampPaymentMethod
    abstract val provider: OnrampProvider

    data class Data(
        override val paymentMethod: OnrampPaymentMethod,
        override val provider: OnrampProvider,
        val fromAmount: BigDecimal,
        val toAmount: BigDecimal,
        val minFromAmount: BigDecimal,
        val maxFromAmount: BigDecimal,
    ) : OnrampQuote()

    sealed class Error : OnrampQuote() {

        data class AmountTooSmallError(
            override val paymentMethod: OnrampPaymentMethod,
            override val provider: OnrampProvider,
            val amount: Amount,
        ) : Error()

        data class AmountTooBigError(
            override val paymentMethod: OnrampPaymentMethod,
            override val provider: OnrampProvider,
            val amount: Amount,
        ) : Error()
    }
}
