package com.tangem.features.onramp.mainv2.entity

import com.tangem.domain.onramp.model.OnrampCountry
import kotlinx.serialization.Serializable

@Serializable
sealed interface OnrampV2MainBottomSheetConfig {
    @Serializable
    data class ConfirmResidency(val country: OnrampCountry) : OnrampV2MainBottomSheetConfig

    @Serializable
    data object CurrenciesList : OnrampV2MainBottomSheetConfig

    @Serializable
    data object AllOffers : OnrampV2MainBottomSheetConfig
}