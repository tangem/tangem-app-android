package com.tangem.feature.wallet.presentation.wallet.viewmodels

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.tangem.feature.wallet.presentation.common.WalletPreviewData
import com.tangem.feature.wallet.presentation.router.InnerWalletRouter
import com.tangem.feature.wallet.presentation.wallet.state.WalletStateHolder
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlin.properties.Delegates

/**
 * Wallet screen view model
 *
[REDACTED_AUTHOR]
 */
@HiltViewModel
internal class WalletViewModel @Inject constructor() : ViewModel() {

    /** Feature router */
    var router: InnerWalletRouter by Delegates.notNull()

    /** Screen state */
    var uiState by mutableStateOf(getInitialState())
        private set

    // TODO: [REDACTED_TASK_KEY] Use production data instead of WalletPreviewData
    private fun getInitialState(): WalletStateHolder = WalletPreviewData.multicurrencyWalletScreenState.copy(
        onBackClick = { router.popBackStack() },
        topBarConfig = WalletPreviewData.multicurrencyWalletScreenState.topBarConfig.copy(
            onScanCardClick = { router.openOrganizeTokensScreen() },
        ),
        walletsListConfig = WalletPreviewData.multicurrencyWalletScreenState.walletsListConfig.copy(
            onWalletChange = ::selectWallet,
        ),
    )

    // TODO: [REDACTED_TASK_KEY] Use production data instead of WalletPreviewData
    private fun selectWallet(index: Int) {
        if (uiState.walletsListConfig.selectedWalletIndex == index) return

        Log.i("WalletViewModel", "selectWallet: $index")

        uiState = if (index % 2 == 0) {
            WalletPreviewData.multicurrencyWalletScreenState.copy(
                walletsListConfig = uiState.walletsListConfig.copy(selectedWalletIndex = index),
            )
        } else {
            WalletPreviewData.singleWalletScreenState.copy(
                walletsListConfig = uiState.walletsListConfig.copy(selectedWalletIndex = index),
            )
        }
    }
}