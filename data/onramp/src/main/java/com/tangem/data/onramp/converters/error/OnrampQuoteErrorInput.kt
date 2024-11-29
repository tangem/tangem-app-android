package com.tangem.data.onramp.converters.error

import com.tangem.domain.onramp.model.OnrampPaymentMethod
import com.tangem.domain.onramp.model.OnrampProvider
import com.tangem.domain.tokens.model.Amount

data class OnrampQuoteErrorInput(
    val errorBody: String,
    val amount: Amount,
    val paymentMethod: OnrampPaymentMethod,
    val provider: OnrampProvider,
)