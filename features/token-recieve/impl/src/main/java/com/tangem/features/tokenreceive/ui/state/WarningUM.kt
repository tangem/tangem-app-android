package com.tangem.features.tokenreceive.ui.state

import com.tangem.core.ui.components.currency.icon.CurrencyIconState

internal data class WarningUM(
    val network: String,
    val iconState: CurrencyIconState,
    val onWarningAcknowledged: () -> Unit,
)