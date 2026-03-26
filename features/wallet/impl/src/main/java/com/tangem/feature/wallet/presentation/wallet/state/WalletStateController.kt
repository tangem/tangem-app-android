package com.tangem.feature.wallet.presentation.wallet.state

import com.tangem.core.ui.DesignFeatureToggles
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfigContent
import com.tangem.core.ui.event.consumedEvent
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.feature.wallet.presentation.wallet.state.model.*
import com.tangem.feature.wallet.presentation.wallet.state.transformers.CloseBottomSheetTransformer
import com.tangem.feature.wallet.presentation.wallet.state.transformers.OpenBottomSheetTransformer
import com.tangem.feature.wallet.presentation.wallet.state.transformers.WalletScreenStateTransformer
import com.tangem.utils.extensions.indexOfFirstOrNull
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
internal class WalletStateController @Inject constructor(
    private val designFeatureToggles: DesignFeatureToggles,
) {

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

    fun getWalletUM(userWalletId: UserWalletId): WalletUM? {
        return value.wallets2.firstOrNull { it.walletsBalanceUM.id == userWalletId }
    }

    fun getWalletStateIfSelected(walletId: UserWalletId): WalletState? {
        val selectedWalletId = getSelectedWalletId()

        return value.wallets.firstOrNull {
            it.walletCardState.id == walletId && it.walletCardState.id == selectedWalletId
        }
    }

    fun getWalletUMIfSelected(walletId: UserWalletId): WalletUM? {
        val selectedWalletId = getSelectedWalletId()

        return value.wallets2.firstOrNull {
            it.walletsBalanceUM.id == walletId && it.walletsBalanceUM.id == selectedWalletId
        }
    }

    fun getSelectedWallet(): WalletState {
        return with(value) { wallets[selectedWalletIndex] }
    }

    fun getSelectedWalletUM(): WalletUM {
        return with(value) { wallets2[selectedWalletIndex] }
    }

    fun getSelectedWalletId(): UserWalletId {
        return with(value) {
            if (designFeatureToggles.isRedesignEnabled) {
                wallets2[selectedWalletIndex].walletsBalanceUM.id
            } else {
                wallets[selectedWalletIndex].walletCardState.id
            }
        }
    }

    fun getWalletIndexByWalletId(userWalletId: UserWalletId): Int? {
        return with(value) {
            if (designFeatureToggles.isRedesignEnabled) {
                wallets2.indexOfFirstOrNull { it.walletsBalanceUM.id == userWalletId }
            } else {
                wallets.indexOfFirstOrNull { it.walletCardState.id == userWalletId }
            }
        }
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

    fun hideBottomSheet() {
        update(
            transformer = OpenBottomSheetTransformer(
                userWalletId = getSelectedWalletId(),
                content = TangemBottomSheetConfigContent.Empty,
                onDismissBottomSheet = {},
            ),
        )
    }

    private fun getInitialState(): WalletScreenState {
        return WalletScreenState(
            topBarConfig = WalletTopBarConfig(),
            selectedWalletIndex = NOT_INITIALIZED_WALLET_INDEX,
            wallets = persistentListOf(),
            wallets2 = persistentListOf(),
            onWalletChange = { _, _ -> },
            event = consumedEvent(),
            isHidingMode = false,
            showMarketsOnboarding = false,
            onDismissMarketsTooltip = {},
            isRedesignEnabled = designFeatureToggles.isRedesignEnabled,
        )
    }
}