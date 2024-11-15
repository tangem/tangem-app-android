package com.tangem.features.onramp.main.entity

import com.tangem.domain.onramp.model.OnrampCountry
import kotlinx.serialization.Serializable

@Serializable
internal sealed interface OnrampMainBottomSheetConfig {
    @Serializable
    data class ConfirmResidency(val country: OnrampCountry) : OnrampMainBottomSheetConfig

    @Serializable
    data object CurrenciesList : OnrampMainBottomSheetConfig
}