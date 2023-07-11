package com.tangem.feature.wallet.presentation.wallet.viewmodels

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tangem.common.doOnFailure
import com.tangem.common.doOnSuccess
import com.tangem.domain.card.ScanCardProcessor
import com.tangem.domain.tokens.GetTokenListUseCase
import com.tangem.feature.wallet.presentation.common.WalletPreviewData
import com.tangem.feature.wallet.presentation.router.InnerWalletRouter
import com.tangem.feature.wallet.presentation.wallet.state.WalletStateHolder
import com.tangem.feature.wallet.presentation.wallet.state.WalletTopBarConfig
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.properties.Delegates

/**
 * Wallet screen view model
 *
[REDACTED_AUTHOR]
 */
@HiltViewModel
internal class WalletViewModel @Inject constructor(
    @Suppress("unused") // TODO: [REDACTED_JIRA]
    private val getTokenListUseCase: GetTokenListUseCase,
    private val scanCardProcessor: ScanCardProcessor,
    private val dispatchers: CoroutineDispatcherProvider,
) : ViewModel() {

    /** Feature router */
    var router: InnerWalletRouter by Delegates.notNull()

    /** Screen state */
    var uiState by mutableStateOf(getInitialState())
        private set

    // TODO: [REDACTED_TASK_KEY] Use production data instead of WalletPreviewData
    private fun getInitialState(): WalletStateHolder = WalletPreviewData.multicurrencyWalletScreenState.copy(
        onBackClick = ::onBackClick,
        topBarConfig = createTopBarConfig(),
        walletsListConfig = WalletPreviewData.multicurrencyWalletScreenState.walletsListConfig.copy(
            onWalletChange = ::selectWallet,
        ),
    )

    private fun onBackClick() {
        router.popBackStack()
    }

    private fun createTopBarConfig(): WalletTopBarConfig {
        return WalletTopBarConfig(
            onScanCardClick = ::onScanCardClick,
            onMoreClick = { router.openDetailsScreen() },
        )
    }

    private fun onScanCardClick() {
        viewModelScope.launch(dispatchers.io) {
            scanCardProcessor.scan(allowsRequestAccessCodeFromRepository = true)
                .doOnSuccess { /* [REDACTED_TODO_COMMENT] */ }
                .doOnFailure { /* [REDACTED_TODO_COMMENT] */ }
        }
    }

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