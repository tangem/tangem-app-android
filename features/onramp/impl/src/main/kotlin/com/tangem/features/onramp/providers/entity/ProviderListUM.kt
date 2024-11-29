package com.tangem.features.onramp.providers.entity

import kotlinx.collections.immutable.ImmutableList

internal data class ProviderListUM(
    val paymentMethod: ProviderListPaymentMethodUM,
    val providers: ImmutableList<ProviderListItemUM>,
)