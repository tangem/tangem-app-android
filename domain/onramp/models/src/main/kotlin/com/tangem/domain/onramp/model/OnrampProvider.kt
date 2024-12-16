package com.tangem.domain.onramp.model

import kotlinx.serialization.Serializable

@Serializable
data class OnrampProvider(
    val id: String,
    val info: OnrampProviderInfo,
    val paymentMethods: List<OnrampPaymentMethod>,
)

@Serializable
data class OnrampProviderInfo(
    val name: String,
    val imageLarge: String,
    val termsOfUseLink: String?,
    val privacyPolicyLink: String?,
)