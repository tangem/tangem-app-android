package com.tangem.feature.wallet.presentation.wallet.state2.model

import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfigContent

internal data class BalancesAndLimitsBottomSheetConfig(
    val currency: String,
    val balance: Balance,
    val limit: Limit,
    val onBalanceInfoClick: () -> Unit,
    val onLimitInfoClick: () -> Unit,
) : TangemBottomSheetConfigContent {

    data class Balance(
        val totalBalance: String,
        val availableBalance: String,
        val blockedBalance: String,
        val debit: String,
        val pending: String,
        val amlVerified: String,
    )

    data class Limit(
        val availableBy: String,
        val inStore: String,
        val other: String,
        val singleTransaction: String,
    )
}