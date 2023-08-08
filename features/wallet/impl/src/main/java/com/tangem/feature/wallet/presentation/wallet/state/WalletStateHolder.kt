package com.tangem.feature.wallet.presentation.wallet.state

import com.tangem.feature.wallet.presentation.wallet.state.components.*
import kotlinx.collections.immutable.ImmutableList

/**
 * Wallet screen state holder
 *
* [REDACTED_AUTHOR]
 */
internal sealed class WalletStateHolder {

    /** Lambda be invoked when back button is clicked */
    abstract val onBackClick: () -> Unit

    /** Top bar config */
    abstract val topBarConfig: WalletTopBarConfig

    /** Wallets list config */
    abstract val walletsListConfig: WalletsListConfig

    /** Pull to refresh config */
    abstract val pullToRefreshConfig: WalletPullToRefreshConfig

    /** Notifications */
    abstract val notifications: ImmutableList<WalletNotification>

    /** Bottom sheet config */
    abstract val bottomSheetConfig: WalletBottomSheetConfig?

    fun copySealed(
        onBackClick: () -> Unit = this.onBackClick,
        topBarConfig: WalletTopBarConfig = this.topBarConfig,
        walletsListConfig: WalletsListConfig = this.walletsListConfig,
        pullToRefreshConfig: WalletPullToRefreshConfig = this.pullToRefreshConfig,
        notifications: ImmutableList<WalletNotification> = this.notifications,
        bottomSheet: WalletBottomSheetConfig? = this.bottomSheetConfig,
    ): WalletStateHolder {
        return when (this) {
            is WalletLoading -> {
                copy(onBackClick = onBackClick)
            }
            is WalletMultiCurrencyState.Content -> {
                copy(
                    onBackClick = onBackClick,
                    topBarConfig = topBarConfig,
                    walletsListConfig = walletsListConfig,
                    pullToRefreshConfig = pullToRefreshConfig,
                    notifications = notifications,
                    bottomSheetConfig = bottomSheet,
                )
            }
            is WalletMultiCurrencyState.Locked -> {
                if (bottomSheet != null) {
                    copy(
                        onBackClick = onBackClick,
                        topBarConfig = topBarConfig,
                        walletsListConfig = walletsListConfig,
                        pullToRefreshConfig = pullToRefreshConfig,
                        isBottomSheetShow = bottomSheet.isShow,
                        onBottomSheetDismiss = bottomSheet.onDismissRequest,
                    )
                } else {
                    copy(
                        onBackClick = onBackClick,
                        topBarConfig = topBarConfig,
                        walletsListConfig = walletsListConfig,
                        pullToRefreshConfig = pullToRefreshConfig,
                    )
                }
            }
            is WalletSingleCurrencyState.Content -> {
                copy(
                    onBackClick = onBackClick,
                    topBarConfig = topBarConfig,
                    walletsListConfig = walletsListConfig,
                    pullToRefreshConfig = pullToRefreshConfig,
                    notifications = notifications,
                    bottomSheetConfig = bottomSheet,
                )
            }
            is WalletSingleCurrencyState.Locked -> {
                if (bottomSheet != null) {
                    copy(
                        onBackClick = onBackClick,
                        topBarConfig = topBarConfig,
                        walletsListConfig = walletsListConfig,
                        pullToRefreshConfig = pullToRefreshConfig,
                        isBottomSheetShow = bottomSheet.isShow,
                        onBottomSheetDismiss = bottomSheet.onDismissRequest,
                    )
                } else {
                    copy(
                        onBackClick = onBackClick,
                        topBarConfig = topBarConfig,
                        walletsListConfig = walletsListConfig,
                        pullToRefreshConfig = pullToRefreshConfig,
                    )
                }
            }
        }
    }
}
