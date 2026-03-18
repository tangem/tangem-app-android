package com.tangem.features.onramp.main.entity

import com.tangem.domain.onramp.model.OnrampCountry
import kotlinx.serialization.Serializable

@Serializable
sealed interface OnrampMainBottomSheetConfig {
    @Serializable
    data class ConfirmResidency(val country: OnrampCountry) : OnrampMainBottomSheetConfig

    @Serializable
    data object CurrenciesList : OnrampMainBottomSheetConfig

    @Serializable
    data class AllOffers(val amountCurrencyCode: String) : OnrampMainBottomSheetConfig
}