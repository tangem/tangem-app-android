package com.tangem.feature.wallet.presentation.wallet.state

import com.tangem.core.ui.components.buttons.actions.ActionButtonConfig
import com.tangem.core.ui.components.marketprice.MarketPriceBlockState
import com.tangem.feature.wallet.presentation.wallet.state.components.*
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

/**
 * Single currency wallet content state
 *
 * @author Andrew Khokhlov on 07/08/2023
 */
internal sealed class WalletSingleCurrencyState : WalletStateHolder() {

    /** Manage buttons */
    abstract val buttons: ImmutableList<ActionButtonConfig>

    /** Market price block state */
    abstract val marketPriceBlockState: MarketPriceBlockState?

    /** Transactions history state */
    abstract val txHistoryState: WalletTxHistoryState

    data class Content(
        override val onBackClick: () -> Unit,
        override val topBarConfig: WalletTopBarConfig,
        override val walletsListConfig: WalletsListConfig,
        override val pullToRefreshConfig: WalletPullToRefreshConfig,
        override val notifications: ImmutableList<WalletNotification>,
        override val bottomSheetConfig: WalletBottomSheetConfig?,
        override val buttons: ImmutableList<ActionButtonConfig>,
        override val marketPriceBlockState: MarketPriceBlockState,
        override val txHistoryState: WalletTxHistoryState,
    ) : WalletSingleCurrencyState()

    data class Locked(
        override val onBackClick: () -> Unit,
        override val topBarConfig: WalletTopBarConfig,
        override val walletsListConfig: WalletsListConfig,
        override val pullToRefreshConfig: WalletPullToRefreshConfig,
        override val buttons: ImmutableList<ActionButtonConfig>,
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

        override val txHistoryState: WalletTxHistoryState = WalletTxHistoryState.Locked(onExploreClick)
    }
}
