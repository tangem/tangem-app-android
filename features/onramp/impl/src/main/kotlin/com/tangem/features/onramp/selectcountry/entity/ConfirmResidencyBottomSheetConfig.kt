package com.tangem.features.onramp.selectcountry.entity

import kotlinx.serialization.Serializable

@Serializable
internal sealed class ConfirmResidencyBottomSheetConfig {

    @Serializable
    data object SelectCountry : ConfirmResidencyBottomSheetConfig()
}
