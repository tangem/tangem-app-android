package com.tangem.feature.wallet.presentation.wallet.state

import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.event.StateEvent
import com.tangem.core.ui.event.consumedEvent
import com.tangem.feature.wallet.presentation.wallet.state.components.*
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

/**
 * Multi currency wallet state
 *
 * @author Andrew Khokhlov on 07/08/2023
 */
internal sealed class WalletMultiCurrencyState : WalletState.ContentState() {

    /** Tokens list state */
    abstract val tokensListState: WalletTokensListState

    data class Content(
        override val onBackClick: () -> Unit,
        override val topBarConfig: WalletTopBarConfig,
        override val walletsListConfig: WalletsListConfig,
        override val pullToRefreshConfig: WalletPullToRefreshConfig,
        override val notifications: ImmutableList<WalletNotification>,
        override val bottomSheetConfig: TangemBottomSheetConfig?,
        override val tokensListState: WalletTokensListState,
        override val event: StateEvent<WalletEvent> = consumedEvent(),
        override val isBalanceHidden: Boolean,
        val tokenActionsBottomSheet: ActionsBottomSheetConfig?,
        val onManageTokensClick: () -> Unit,
    ) : WalletMultiCurrencyState()

    data class Locked(
        override val onBackClick: () -> Unit,
        override val topBarConfig: WalletTopBarConfig,
        override val walletsListConfig: WalletsListConfig,
        override val pullToRefreshConfig: WalletPullToRefreshConfig,
        override val onUnlockWalletsNotificationClick: () -> Unit,
        override val onUnlockClick: () -> Unit,
        override val onScanClick: () -> Unit,
        override val isBottomSheetShow: Boolean = false,
        override val onBottomSheetDismiss: () -> Unit = {},
        override val event: StateEvent<WalletEvent> = consumedEvent(),
        override val isBalanceHidden: Boolean,
    ) : WalletMultiCurrencyState(), WalletLockedState {

        override val notifications = persistentListOf(
            WalletNotification.UnlockWallets(onUnlockWalletsNotificationClick),
        )

        override val bottomSheetConfig = TangemBottomSheetConfig(
            isShow = isBottomSheetShow,
            onDismissRequest = onBottomSheetDismiss,
            content = WalletBottomSheetConfig.UnlockWallets(
                onUnlockClick = onUnlockClick,
                onScanClick = onScanClick,
            ),
        )

        override val tokensListState: WalletTokensListState = WalletTokensListState.Locked
    }
}
