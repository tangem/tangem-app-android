package com.tangem.features.yield.supply.impl.warning.ui

import com.tangem.core.ui.components.currency.icon.CurrencyIconState

internal data class YieldSupplyDepositedWarningUM(
    val network: String,
    val iconState: CurrencyIconState,
    val onWarningAcknowledged: () -> Unit,
)