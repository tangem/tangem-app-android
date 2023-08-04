package com.tangem.feature.wallet.presentation.wallet.state

import com.tangem.core.ui.components.buttons.actions.ActionButtonConfig
import com.tangem.core.ui.components.marketprice.MarketPriceBlockState
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.feature.wallet.presentation.wallet.state.content.WalletLockedContentState
import com.tangem.feature.wallet.presentation.wallet.state.content.WalletTokensListState
import com.tangem.feature.wallet.presentation.wallet.state.content.WalletTxHistoryState
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

/**
 * Wallet screen state holder
 *
 * @property onBackClick            lambda be invoked when back button is clicked
 * @property topBarConfig           top bar config
 * @property walletsListConfig      wallets list config
 * @property pullToRefreshConfig    pull to refresh config
 * @property notifications          notifications
 *
* [REDACTED_AUTHOR]
 */
internal sealed class WalletStateHolder(
    open val onBackClick: () -> Unit,
    open val topBarConfig: WalletTopBarConfig,
    open val walletsListConfig: WalletsListConfig,
    open val pullToRefreshConfig: WalletPullToRefreshConfig,
    open val notifications: ImmutableList<WalletNotification>,
    open val bottomSheet: WalletBottomSheetConfig? = null,
) {

    fun copySealed(
        onBackClick: () -> Unit = this.onBackClick,
        topBarConfig: WalletTopBarConfig = this.topBarConfig,
        walletsListConfig: WalletsListConfig = this.walletsListConfig,
        pullToRefreshConfig: WalletPullToRefreshConfig = this.pullToRefreshConfig,
        notifications: ImmutableList<WalletNotification> = this.notifications,
        bottomSheet: WalletBottomSheetConfig? = this.bottomSheet,
    ): WalletStateHolder {
        return when (this) {
            is MultiCurrencyContent -> this.copy(
                onBackClick = onBackClick,
                topBarConfig = topBarConfig,
                walletsListConfig = walletsListConfig,
                pullToRefreshConfig = pullToRefreshConfig,
                notifications = notifications,
                bottomSheet = bottomSheet,
            )
            is SingleCurrencyContent -> this.copy(
                onBackClick = onBackClick,
                topBarConfig = topBarConfig,
                walletsListConfig = walletsListConfig,
                pullToRefreshConfig = pullToRefreshConfig,
                notifications = notifications,
                bottomSheet = bottomSheet,
            )
            is UnlockWalletContent -> this.copy(
                onBackClick = onBackClick,
                topBarConfig = topBarConfig,
                walletsListConfig = walletsListConfig,
                pullToRefreshConfig = pullToRefreshConfig,
            )
            is Loading -> copy(onBackClick = onBackClick)
        }
    }

    /**
     * Multi currency wallet content state
     *
     * @property onBackClick            lambda be invoked when back button is clicked
     * @property topBarConfig           top bar config
     * @property walletsListConfig      wallets list config
     * @property pullToRefreshConfig    pull to refresh config
     * @property tokensListState        token list state
     * @property notifications          notifications
     */
    data class MultiCurrencyContent(
        override val onBackClick: () -> Unit,
        override val topBarConfig: WalletTopBarConfig,
        override val walletsListConfig: WalletsListConfig,
        override val pullToRefreshConfig: WalletPullToRefreshConfig,
        override val notifications: ImmutableList<WalletNotification>,
        override val bottomSheet: WalletBottomSheetConfig? = null,
        val tokensListState: WalletTokensListState,
    ) : WalletStateHolder(
        onBackClick = onBackClick,
        topBarConfig = topBarConfig,
        walletsListConfig = walletsListConfig,
        pullToRefreshConfig = pullToRefreshConfig,
        notifications = notifications,
        bottomSheet = bottomSheet,
    )

    /**
     * Single currency wallet content state
     *
     * @property onBackClick            lambda be invoked when back button is clicked
     * @property topBarConfig           top bar config
     * @property walletsListConfig      wallets list config
     * @property pullToRefreshConfig    pull to refresh config
     * @property notifications          notifications
     * @property buttons                manage buttons
     * @property marketPriceBlockState  market price block state
     * @property txHistoryState         transactions history state
     */
    data class SingleCurrencyContent(
        override val onBackClick: () -> Unit,
        override val topBarConfig: WalletTopBarConfig,
        override val walletsListConfig: WalletsListConfig,
        override val pullToRefreshConfig: WalletPullToRefreshConfig,
        override val notifications: ImmutableList<WalletNotification>,
        override val bottomSheet: WalletBottomSheetConfig? = null,
        val buttons: ImmutableList<ActionButtonConfig>,
        val marketPriceBlockState: MarketPriceBlockState,
        val txHistoryState: WalletTxHistoryState,
    ) : WalletStateHolder(
        onBackClick = onBackClick,
        topBarConfig = topBarConfig,
        walletsListConfig = walletsListConfig,
        pullToRefreshConfig = pullToRefreshConfig,
        notifications = notifications,
        bottomSheet = bottomSheet,
    )

    /**
     * Unlock wallet content state
     *
     * @property onBackClick                      lambda be invoked when back button is clicked
     * @property topBarConfig                     top bar config
     * @property walletsListConfig                wallets list config
     * @property pullToRefreshConfig              pull to refresh config
     * @property lockedContentState               locked content state
     * @property onUnlockWalletsNotificationClick lambda be invoked when unlock wallets notification is clicked
     * @property onBottomSheetDismissRequest      lambda be invoked when bottom sheet is dismissed
     * @property onUnlockClick                    lambda be invoked when unlock button is clicked
     * @property onScanClick                      lambda be invoked when scan card button is clicked
     */
    data class UnlockWalletContent(
        override val onBackClick: () -> Unit,
        override val topBarConfig: WalletTopBarConfig,
        override val walletsListConfig: WalletsListConfig,
        override val pullToRefreshConfig: WalletPullToRefreshConfig,
        val lockedContentState: WalletLockedContentState,
        val onUnlockWalletsNotificationClick: () -> Unit,
        val onBottomSheetDismissRequest: () -> Unit,
        val onUnlockClick: () -> Unit,
        val onScanClick: () -> Unit,
    ) : WalletStateHolder(
        onBackClick = onBackClick,
        topBarConfig = topBarConfig,
        walletsListConfig = walletsListConfig,
        pullToRefreshConfig = pullToRefreshConfig,
        notifications = persistentListOf(WalletNotification.UnlockWallets(onUnlockWalletsNotificationClick)),
        bottomSheet = WalletBottomSheetConfig(
            isShow = false,
            onDismissRequest = onBottomSheetDismissRequest,
            content = WalletBottomSheetConfig.BottomSheetContentConfig.UnlockWallets(
                onUnlockClick = onUnlockClick,
                onScanClick = onScanClick,
            ),
        ),
    )

    /**
     * Loading state
     *
     * @property onBackClick lambda be invoked when back button is clicked
     */
    data class Loading(override val onBackClick: () -> Unit) : WalletStateHolder(
        onBackClick = onBackClick,
        topBarConfig = WalletTopBarConfig(onScanCardClick = {}, onMoreClick = {}),
        walletsListConfig = WalletsListConfig(
            selectedWalletIndex = 0,
            wallets = persistentListOf(
                WalletCardState.Loading(
                    id = UserWalletId(stringValue = ""),
                    title = "",
                    additionalInfo = "",
                    imageResId = null,
                ),
            ),
            onWalletChange = {},
        ),
        pullToRefreshConfig = WalletPullToRefreshConfig(isRefreshing = false, onRefresh = {}),
        notifications = persistentListOf(),
        bottomSheet = null,
    )
}
