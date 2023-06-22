package com.tangem.feature.wallet.presentation.wallet.ui

import androidx.activity.compose.BackHandler
import androidx.compose.material.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.tangem.feature.wallet.presentation.wallet.state.WalletStateHolder
import com.tangem.feature.wallet.presentation.wallet.ui.components.WalletHeader

/**
 * Wallet screen
 *
 * @param state screen state
 *
[REDACTED_AUTHOR]
 */
@Composable
internal fun WalletScreen(state: WalletStateHolder, modifier: Modifier = Modifier) {
    BackHandler(onBack = state.onBackClick)

    Scaffold(
        modifier = modifier,
        topBar = { WalletHeader(config = state.headerConfig) },
    ) {
        // TODO: [REDACTED_TASK_KEY] Design a body with tokens and transactions
    }
}