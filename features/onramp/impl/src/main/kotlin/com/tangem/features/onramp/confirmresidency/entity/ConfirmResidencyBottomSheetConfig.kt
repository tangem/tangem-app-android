package com.tangem.features.onramp.confirmresidency.entity

import kotlinx.serialization.Serializable

@Serializable
internal sealed class ConfirmResidencyBottomSheetConfig {

    @Serializable
    data object SelectCountry : ConfirmResidencyBottomSheetConfig()
}