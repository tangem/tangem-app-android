package com.tangem.feature.wallet.presentation.wallet.state

import com.tangem.core.ui.components.buttons.actions.ActionButtonConfig
import com.tangem.core.ui.components.marketprice.MarketPriceBlockState
import kotlinx.collections.immutable.ImmutableList

/**
 * Wallet screen state holder
 *
 * @property onBackClick            lambda be invoked when back button is clicked
 * @property topBarConfig           top bar config
 * @property walletsListConfig      wallets list config
 * @property pullToRefreshConfig    pull to refresh config
 * @property contentItems           content items
 * @property notifications          notifications
 *
[REDACTED_AUTHOR]
 */
internal sealed class WalletStateHolder(
    open val onBackClick: () -> Unit,
    open val topBarConfig: WalletTopBarConfig,
    open val walletsListConfig: WalletsListConfig,
    open val pullToRefreshConfig: WalletPullToRefreshConfig,
    open val contentItems: ImmutableList<WalletContentItemState>,
    open val notifications: ImmutableList<WalletNotification>,
    open val bottomSheet: WalletBottomSheetConfig? = null,
) {

    /**
     * Multi currency wallet content state
     *
     * @property onBackClick            lambda be invoked when back button is clicked
     * @property topBarConfig           top bar config
     * @property walletsListConfig      wallets list config
     * @property pullToRefreshConfig    pull to refresh config
     * @property contentItems           content items
     * @property notifications          notifications
     * @property onOrganizeTokensClick  lambda be invoked when organize tokens button is clicked
     */
    data class MultiCurrencyContent(
        override val onBackClick: () -> Unit,
        override val topBarConfig: WalletTopBarConfig,
        override val walletsListConfig: WalletsListConfig,
        override val pullToRefreshConfig: WalletPullToRefreshConfig,
        override val contentItems: ImmutableList<WalletContentItemState.MultiCurrencyItem>,
        override val notifications: ImmutableList<WalletNotification>,
        override val bottomSheet: WalletBottomSheetConfig,
        val onOrganizeTokensClick: () -> Unit,
    ) : WalletStateHolder(
        onBackClick = onBackClick,
        topBarConfig = topBarConfig,
        walletsListConfig = walletsListConfig,
        pullToRefreshConfig = pullToRefreshConfig,
        contentItems = contentItems,
        bottomSheet = bottomSheet,
        notifications = notifications,
    )

    /**
     * Single currency wallet content state
     *
     * @property onBackClick            lambda be invoked when back button is clicked
     * @property topBarConfig           top bar config
     * @property walletsListConfig      wallets list config
     * @property pullToRefreshConfig    pull to refresh config
     * @property contentItems           content items
     * @property notifications          notifications
     * @property buttons                manage buttons
     * @property marketPriceBlockState  market price block state
     */
    data class SingleCurrencyContent(
        override val onBackClick: () -> Unit,
        override val topBarConfig: WalletTopBarConfig,
        override val walletsListConfig: WalletsListConfig,
        override val pullToRefreshConfig: WalletPullToRefreshConfig,
        override val contentItems: ImmutableList<WalletContentItemState.SingleCurrencyItem>,
        override val notifications: ImmutableList<WalletNotification>,
        override val bottomSheet: WalletBottomSheetConfig,
        val buttons: ImmutableList<ActionButtonConfig>,
        val marketPriceBlockState: MarketPriceBlockState,
    ) : WalletStateHolder(
        onBackClick = onBackClick,
        topBarConfig = topBarConfig,
        walletsListConfig = walletsListConfig,
        pullToRefreshConfig = pullToRefreshConfig,
        contentItems = contentItems,
        notifications = notifications,
        bottomSheet = bottomSheet,
    )
}