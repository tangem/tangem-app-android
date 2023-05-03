package com.tangem.feature.main.presentation.ui

import androidx.compose.ui.graphics.painter.Painter

data class WalletHeaderConfig(
    val walletName: String,
    val balance: String,
    val additionalInfo: String,
    val balanceState: BalanceUIState,
    val cardImage: Painter,
    val onClick: (() -> Unit)?,
)