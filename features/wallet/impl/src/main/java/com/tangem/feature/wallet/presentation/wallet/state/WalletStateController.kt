package com.tangem.feature.wallet.presentation.wallet.state

import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfigContent
import com.tangem.core.ui.event.consumedEvent
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.feature.wallet.presentation.wallet.state.model.*
import com.tangem.feature.wallet.presentation.wallet.state.model.NOT_INITIALIZED_WALLET_INDEX
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletScreenState
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletState
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletTopBarConfig
import com.tangem.feature.wallet.presentation.wallet.state.transformers.CloseBottomSheetTransformer
import com.tangem.feature.wallet.presentation.wallet.state.transformers.OpenBottomSheetTransformer
import com.tangem.feature.wallet.presentation.wallet.state.transformers.WalletScreenStateTransformer
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import timber.log.Timber
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

    val isInitialized: Boolean
        get() = value.selectedWalletIndex != NOT_INITIALIZED_WALLET_INDEX

    private val mutableUiState: MutableStateFlow<WalletScreenState> = MutableStateFlow(value = getInitialState())

    fun update(function: (WalletScreenState) -> WalletScreenState) {
        mutableUiState.update(function = function)
    }

    fun update(transformer: WalletScreenStateTransformer) {
        Timber.d("Applying: ${transformer::class.simpleName}")
        mutableUiState.update(function = transformer::transform)
    }

    fun clear() {
        mutableUiState.update { getInitialState() }
    }

    fun getWalletState(userWalletId: UserWalletId): WalletState? {
        return value.wallets.firstOrNull { it.walletCardState.id == userWalletId }
    }

    fun getWalletStateIfSelected(walletId: UserWalletId): WalletState? {
        val selectedWalletId = getSelectedWalletId()

        return value.wallets.firstOrNull {
            it.walletCardState.id == walletId && it.walletCardState.id == selectedWalletId
        }
    }

    fun getSelectedWallet(): WalletState {
        return with(value) { wallets[selectedWalletIndex] }
    }

    fun getSelectedWalletId(): UserWalletId {
        return with(value) { wallets[selectedWalletIndex].walletCardState.id }
    }

    fun showBottomSheet(
        content: TangemBottomSheetConfigContent,
        userWalletId: UserWalletId = getSelectedWalletId(),
        onDismiss: (() -> Unit)? = null,
    ) {
        update(
            OpenBottomSheetTransformer(
                userWalletId = userWalletId,
                content = content,
                onDismissBottomSheet = {
                    onDismiss?.invoke()
                    update(CloseBottomSheetTransformer(userWalletId))
                },
            ),
        )
    }

    private fun getInitialState(): WalletScreenState {
        return WalletScreenState(
            topBarConfig = WalletTopBarConfig(onDetailsClick = {}),
            selectedWalletIndex = NOT_INITIALIZED_WALLET_INDEX,
            wallets = persistentListOf(),
            onWalletChange = { _, _ -> },
            event = consumedEvent(),
            isHidingMode = false,
            showMarketsOnboarding = false,
            onDismissMarketsOnboarding = {},
        )
    }
}