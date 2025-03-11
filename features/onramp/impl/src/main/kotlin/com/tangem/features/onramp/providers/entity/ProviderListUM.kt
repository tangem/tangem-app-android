package com.tangem.features.onramp.providers.entity

import com.tangem.domain.onramp.model.OnrampPaymentMethod
import kotlinx.collections.immutable.ImmutableList

internal data class SelectPaymentAndProviderUM(
    val paymentMethods: ImmutableList<PaymentProviderUM>,
    val selectedPaymentMethod: PaymentProviderUM,
    val selectedProviderId: String,
    val isPaymentMethodClickEnabled: Boolean,
    val onPaymentMethodClick: () -> Unit,
)

internal data class PaymentProviderUM(
    val paymentMethod: OnrampPaymentMethod,
    val providers: ImmutableList<ProviderListItemUM>,
)