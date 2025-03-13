package com.tangem.feature.wallet.presentation.wallet.state.model

import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfigContent

internal data class BalancesAndLimitsBottomSheetConfig(
    val balance: Balance,
    val limit: Limit,
) : TangemBottomSheetConfigContent {

    data class Balance(
        val totalBalance: String,
        val availableBalance: String,
        val blockedBalance: String,
        val debit: String,
        val amlVerified: String,
        val onInfoClick: () -> Unit,
    )

    data class Limit(
        val availableBy: String,
        val total: String,
        val other: String,
        val singleTransaction: String,
        val onInfoClick: () -> Unit,
    )
}