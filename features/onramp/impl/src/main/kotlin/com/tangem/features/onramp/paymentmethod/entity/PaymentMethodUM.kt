package com.tangem.features.onramp.paymentmethod.entity

import com.tangem.domain.onramp.model.OnrampPaymentMethod
import kotlinx.serialization.Serializable

@Serializable
internal data class PaymentMethodUM(
    val method: OnrampPaymentMethod,
    val onSelect: () -> Unit,
)