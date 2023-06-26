package com.tangem.feature.wallet.presentation.wallet.state

import kotlinx.collections.immutable.ImmutableList

/**
 * Wallet screen state holder
 *
 * @property onBackClick    lambda be invoked when back button is clicked
 * @property topBarConfig   top bar config
 * @property selectedWallet selected wallet
 * @property wallets        list of wallets states
 * @property contentItems   content items
 *
[REDACTED_AUTHOR]
 */
internal sealed class WalletStateHolder(
    open val onBackClick: () -> Unit,
    open val topBarConfig: WalletTopBarConfig,
    open val selectedWallet: WalletCardState,
    open val wallets: ImmutableList<WalletCardState>,
    open val contentItems: ImmutableList<WalletContentItemState>,
) {

    /**
     * Multi currency wallet content state
     *
     * @property onBackClick    lambda be invoked when back button is clicked
     * @property topBarConfig   top bar config
     * @property selectedWallet selected wallet
     * @property wallets        list of wallets states
     * @property contentItems   content items
     */
    data class MultiCurrencyContent(
        override val onBackClick: () -> Unit,
        override val topBarConfig: WalletTopBarConfig,
        override val selectedWallet: WalletCardState,
        override val wallets: ImmutableList<WalletCardState>,
        override val contentItems: ImmutableList<WalletContentItemState.MultiCurrencyItem>,
        val onOrganizeTokensClick: () -> Unit,
    ) : WalletStateHolder(onBackClick, topBarConfig, selectedWallet, wallets, contentItems)

    /**
     * Single currency wallet content state
     *
     * @property onBackClick    lambda be invoked when back button is clicked
     * @property topBarConfig   top bar config
     * @property selectedWallet selected wallet
     * @property wallets        list of wallets states
     * @property contentItems   content items
     */
    data class SingleCurrencyContent(
        override val onBackClick: () -> Unit,
        override val topBarConfig: WalletTopBarConfig,
        override val selectedWallet: WalletCardState,
        override val wallets: ImmutableList<WalletCardState>,
        override val contentItems: ImmutableList<WalletContentItemState.SingleCurrencyItem>,
    ) : WalletStateHolder(onBackClick, topBarConfig, selectedWallet, wallets, contentItems)
}