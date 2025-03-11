package com.tangem.features.onramp.confirmresidency.entity

import com.tangem.core.ui.extensions.TextReference

data class ConfirmResidencyUM(
    val country: String,
    val countryFlagUrl: String,
    val isCountrySupported: Boolean,
    val primaryButtonConfig: ActionButtonConfig,
    val secondaryButtonConfig: ActionButtonConfig,
) {
    data class ActionButtonConfig(
        val onClick: () -> Unit,
        val text: TextReference,
    )
}