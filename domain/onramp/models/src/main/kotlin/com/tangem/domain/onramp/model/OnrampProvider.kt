package com.tangem.domain.onramp.model

data class OnrampProvider(
    val id: String,
    val info: OnrampProviderInfo,
    val paymentMethods: List<OnrampPaymentMethod>,
)

data class OnrampProviderInfo(val name: String, val imageLarge: String)
