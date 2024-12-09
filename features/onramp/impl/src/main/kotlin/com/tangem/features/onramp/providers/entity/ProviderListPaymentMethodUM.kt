package com.tangem.features.onramp.providers.entity

internal data class ProviderListPaymentMethodUM(
    val id: String,
    val name: String,
    val imageUrl: String,
    val enabled: Boolean,
    val onClick: () -> Unit,
)