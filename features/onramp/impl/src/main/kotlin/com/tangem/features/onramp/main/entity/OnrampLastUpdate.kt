package com.tangem.features.onramp.main.entity

import com.tangem.domain.onramp.model.OnrampAmount
import com.tangem.domain.onramp.model.OnrampPaymentMethod

data class OnrampLastUpdate(
    val fromAmount: OnrampAmount,
    val countryCode: String,
    val paymentMethod: OnrampPaymentMethod,
)