package com.tangem.features.onramp.confirmresidency.entity

import kotlinx.serialization.Serializable

@Serializable
internal sealed class ConfirmResidencyBottomSheetConfig {

    @Serializable
    data class SelectCountry(val onDismiss: () -> Unit) : ConfirmResidencyBottomSheetConfig()
}