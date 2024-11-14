package com.tangem.features.onramp.entity

import kotlinx.serialization.Serializable

@Serializable
internal sealed class ConfirmResidencyBottomSheetConfig {

    @Serializable
    data object SelectCountry : ConfirmResidencyBottomSheetConfig()
}