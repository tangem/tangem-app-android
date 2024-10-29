package com.tangem.features.onramp.entity

import kotlinx.serialization.Serializable

@Serializable
internal sealed class OnrampBottomSheetConfig {

    @Serializable
    data object ConfirmResidency : OnrampBottomSheetConfig()
}