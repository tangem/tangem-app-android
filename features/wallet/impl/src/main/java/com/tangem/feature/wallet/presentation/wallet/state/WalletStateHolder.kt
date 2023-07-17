package com.tangem.feature.wallet.presentation.wallet.state

import com.tangem.core.ui.components.buttons.actions.ActionButtonConfig
import com.tangem.core.ui.components.marketprice.MarketPriceBlockState
import kotlinx.collections.immutable.ImmutableList

/**
 * Wallet screen state holder
 *
 * @property onBackClick       lambda be invoked when back button is clicked
 * @property topBarConfig      top bar config
 * @property walletsListConfig wallets list config
 * @property contentItems      content items
 * @property notifications     notifications
 *
[REDACTED_AUTHOR]
 */
internal sealed class WalletStateHolder(
    open val onBackClick: () -> Unit,
    open val topBarConfig: WalletTopBarConfig,
    open val walletsListConfig: WalletsListConfig,
    open val contentItems: ImmutableList<WalletContentItemState>,
    open val notifications: ImmutableList<WalletNotification>,
) {

    /**
     * Multi currency wallet content state
     *
     * @property onBackClick           lambda be invoked when back button is clicked
     * @property topBarConfig          top bar config
     * @property walletsListConfig     wallets list config
     * @property contentItems          content items
     * @property notifications         notifications
     * @property onOrganizeTokensClick lambda be invoked when organize tokens button is clicked
     */
    data class MultiCurrencyContent(
        override val onBackClick: () -> Unit,
        override val topBarConfig: WalletTopBarConfig,
        override val walletsListConfig: WalletsListConfig,
        override val contentItems: ImmutableList<WalletContentItemState.MultiCurrencyItem>,
        override val notifications: ImmutableList<WalletNotification>,
        val onOrganizeTokensClick: () -> Unit,
    ) : WalletStateHolder(onBackClick, topBarConfig, walletsListConfig, contentItems, notifications)

    /**
     * Single currency wallet content state
     *
     * @property onBackClick           lambda be invoked when back button is clicked
     * @property topBarConfig          top bar config
     * @property walletsListConfig     wallets list config
     * @property contentItems          content items
     * @property notifications         notifications
     * @property buttons               manage buttons
     * @property marketPriceBlockState market price block state
     */
    data class SingleCurrencyContent(
        override val onBackClick: () -> Unit,
        override val topBarConfig: WalletTopBarConfig,
        override val walletsListConfig: WalletsListConfig,
        override val contentItems: ImmutableList<WalletContentItemState.SingleCurrencyItem>,
        override val notifications: ImmutableList<WalletNotification>,
        val buttons: ImmutableList<ActionButtonConfig>,
        val marketPriceBlockState: MarketPriceBlockState,
    ) : WalletStateHolder(onBackClick, topBarConfig, walletsListConfig, contentItems, notifications)
}