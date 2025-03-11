package com.tangem.features.onramp.providers.entity

import com.tangem.features.onramp.paymentmethod.entity.PaymentMethodUM
import kotlinx.collections.immutable.ImmutableList
import kotlinx.serialization.Serializable

@Serializable
internal sealed interface ProviderListBottomSheetConfig {

    @Serializable
    data class PaymentMethods(
        val selectedMethodId: String,
        val paymentMethodsUM: ImmutableList<PaymentMethodUM>,
    ) : ProviderListBottomSheetConfig
}