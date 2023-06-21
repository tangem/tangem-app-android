package com.tangem.feature.wallet.presentation.wallet.viewmodels

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
    private fun getInitialState(): WalletStateHolder = WalletPreviewData.groupedWalletScreenState.copy(
        onBackClick = { router.popBackStack() },
        topBarConfig = WalletPreviewData.groupedWalletScreenState.topBarConfig.copy(
            onScanCardClick = { router.openOrganizeTokensScreen() },
        ),
    )
}