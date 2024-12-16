package com.tangem.features.onramp.providers.entity

import com.tangem.domain.onramp.model.OnrampPaymentMethod
import kotlinx.collections.immutable.ImmutableList

internal data class SelectPaymentAndProviderUM(
    val paymentMethods: ImmutableList<OnrampPaymentMethod>,
    val selectedPaymentMethod: SelectProviderUM,
    val isPaymentMethodClickEnabled: Boolean,
    val onPaymentMethodClick: () -> Unit,
)

internal data class SelectProviderUM(
    val paymentMethod: OnrampPaymentMethod,
    val providers: ImmutableList<ProviderListItemUM>,
)