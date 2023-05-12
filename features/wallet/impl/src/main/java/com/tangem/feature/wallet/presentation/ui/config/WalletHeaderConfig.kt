package com.tangem.feature.wallet.presentation.ui.config

import androidx.compose.ui.graphics.painter.Painter
import com.tangem.feature.wallet.presentation.ui.state.BalanceUIState

data class WalletHeaderConfig(
    val walletName: String,
    val balance: String,
    val additionalInfo: String,
    val balanceState: BalanceUIState,
    val cardImage: Painter,
    val onClick: (() -> Unit)?,
)