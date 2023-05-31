package com.tangem.feature.wallet.presentation.ui

import androidx.activity.compose.BackHandler
import androidx.compose.material.Scaffold
import androidx.compose.runtime.Composable
import com.tangem.feature.wallet.presentation.state.WalletStateHolder
import com.tangem.feature.wallet.presentation.ui.components.WalletHeader

/**
 * Wallet screen
 *
 * @param state screen state
 *
 * @author Andrew Khokhlov on 29/05/2023
 */
@Composable
internal fun WalletScreen(state: WalletStateHolder) {
    BackHandler(onBack = state.onBackClick)

    Scaffold(
        topBar = { WalletHeader(config = state.headerConfig) },
    ) {
        // TODO: AND-3642 Design a body with tokens and transactions
    }
}
