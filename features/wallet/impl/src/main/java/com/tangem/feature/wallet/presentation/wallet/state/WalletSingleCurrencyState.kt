package com.tangem.feature.wallet.presentation.wallet.state

import com.tangem.core.ui.components.marketprice.MarketPriceBlockState
import com.tangem.core.ui.components.transactions.state.TxHistoryState
import com.tangem.feature.wallet.presentation.wallet.state.components.*
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

/**
 * Single currency wallet content state
 *
[REDACTED_AUTHOR]
 */
internal sealed class WalletSingleCurrencyState : WalletState.ContentState() {

    /** Manage buttons */
    abstract val buttons: ImmutableList<WalletManageButton>

    /** Market price block state */
    abstract val marketPriceBlockState: MarketPriceBlockState?

    /** Transactions history state */
    abstract val txHistoryState: TxHistoryState

    data class Content(
        override val onBackClick: () -> Unit,
        override val topBarConfig: WalletTopBarConfig,
        override val walletsListConfig: WalletsListConfig,
        override val pullToRefreshConfig: WalletPullToRefreshConfig,
        override val notifications: ImmutableList<WalletNotification>,
        override val bottomSheetConfig: WalletBottomSheetConfig?,
        override val buttons: ImmutableList<WalletManageButton>,
        override val marketPriceBlockState: MarketPriceBlockState,
        override val txHistoryState: TxHistoryState,
    ) : WalletSingleCurrencyState()

    data class Locked(
        override val onBackClick: () -> Unit,
        override val topBarConfig: WalletTopBarConfig,
        override val walletsListConfig: WalletsListConfig,
        override val pullToRefreshConfig: WalletPullToRefreshConfig,
        override val buttons: ImmutableList<WalletManageButton>,
        override val onUnlockWalletsNotificationClick: () -> Unit,
        override val onUnlockClick: () -> Unit,
        override val onScanClick: () -> Unit,
        override val isBottomSheetShow: Boolean = false,
        override val onBottomSheetDismiss: () -> Unit = {},
        val onExploreClick: () -> Unit,
    ) : WalletSingleCurrencyState(), WalletLockedState {

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

        override val marketPriceBlockState = null

        override val txHistoryState: TxHistoryState = TxHistoryState.Locked(onExploreClick)
    }
}