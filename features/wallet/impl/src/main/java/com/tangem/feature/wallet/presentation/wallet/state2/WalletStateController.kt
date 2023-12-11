package com.tangem.feature.wallet.presentation.wallet.state2

import com.tangem.core.ui.event.consumedEvent
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.feature.wallet.presentation.wallet.state.components.WalletTopBarConfig
import com.tangem.feature.wallet.presentation.wallet.state2.model.NOT_INITIALIZED_WALLET_INDEX
import com.tangem.feature.wallet.presentation.wallet.state2.model.WalletScreenState
import com.tangem.feature.wallet.presentation.wallet.state2.model.WalletState
import com.tangem.feature.wallet.presentation.wallet.state2.transformers.WalletScreenStateTransformer
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Wallet state holder
 *
[REDACTED_AUTHOR]
 */
@Singleton
internal class WalletStateController @Inject constructor() {

    val uiState: StateFlow<WalletScreenState> get() = mutableUiState
    val value: WalletScreenState get() = uiState.value

    private val mutableUiState: MutableStateFlow<WalletScreenState> = MutableStateFlow(value = getInitialState())

    fun update(function: (WalletScreenState) -> WalletScreenState) {
        mutableUiState.update(function = function)
    }

    fun update(transformer: WalletScreenStateTransformer) {
        mutableUiState.update(function = transformer::transform)
    }

    fun clear() {
        mutableUiState.update { getInitialState() }
    }

    fun getSelectedWallet(): WalletState {
        return with(value) { wallets[selectedWalletIndex] }
    }

    fun getSelectedWalletId(): UserWalletId {
        return with(value) { wallets[selectedWalletIndex].walletCardState.id }
    }

    private fun getInitialState(): WalletScreenState {
        return WalletScreenState(
            onBackClick = {},
            topBarConfig = WalletTopBarConfig(onDetailsClick = {}),
            selectedWalletIndex = NOT_INITIALIZED_WALLET_INDEX,
            wallets = persistentListOf(),
            onWalletChange = {},
            event = consumedEvent(),
            isHidingMode = false,
        )
    }
}