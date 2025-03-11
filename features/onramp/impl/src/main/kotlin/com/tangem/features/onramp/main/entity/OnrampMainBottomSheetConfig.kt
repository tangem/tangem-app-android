package com.tangem.features.onramp.main.entity

import com.tangem.domain.onramp.model.OnrampCountry
import com.tangem.domain.onramp.model.OnrampPaymentMethod
import kotlinx.serialization.Serializable

@Serializable
internal sealed interface OnrampMainBottomSheetConfig {
    @Serializable
    data class ConfirmResidency(val country: OnrampCountry) : OnrampMainBottomSheetConfig

    @Serializable
    data object CurrenciesList : OnrampMainBottomSheetConfig

    @Serializable
    data class ProvidersList(
        val selectedProviderId: String,
        val selectedPaymentMethod: OnrampPaymentMethod,
    ) : OnrampMainBottomSheetConfig
}