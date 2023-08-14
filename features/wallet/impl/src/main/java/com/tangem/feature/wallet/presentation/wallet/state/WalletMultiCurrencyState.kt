package com.tangem.feature.wallet.presentation.wallet.state

import com.tangem.feature.wallet.presentation.wallet.state.components.*
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

/**
 * Multi currency wallet state
 *
[REDACTED_AUTHOR]
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
        override val bottomSheetConfig: WalletBottomSheetConfig?,
        override val tokensListState: WalletTokensListState,
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
    ) : WalletMultiCurrencyState(), WalletLockedState {

        override val notifications = persistentListOf(
            WalletNotification.UnlockWallets(onUnlockWalletsNotificationClick),
        )

        override val bottomSheetConfig = WalletBottomSheetConfig(
            isShow = isBottomSheetShow,
            onDismissRequest = onBottomSheetDismiss,
            content = WalletBottomSheetConfig.BottomSheetContentConfig.UnlockWallets(
                onUnlockClick = onUnlockClick,
                onScanClick = onScanClick,
            ),
        )

        override val tokensListState: WalletTokensListState = WalletTokensListState.Locked
    }
}