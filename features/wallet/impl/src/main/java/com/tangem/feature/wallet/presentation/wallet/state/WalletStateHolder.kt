package com.tangem.feature.wallet.presentation.wallet.state

import kotlinx.collections.immutable.ImmutableList

/**
 * Wallet state holder
 *
 * @property onBackClick    lambda be invoked when back button is clicked
 * @property topBarConfig   top bar config
 * @property selectedWallet selected wallet
 * @property wallets        list of wallets states
 * @property contentItems   content items
 *
[REDACTED_AUTHOR]
 */
internal data class WalletStateHolder(
    val onBackClick: () -> Unit,
    val topBarConfig: TopBarConfig,
    val selectedWallet: WalletCardState,
    val wallets: ImmutableList<WalletCardState>,
    val contentItems: ImmutableList<WalletContentItemState>,
    val onOrganizeTokensClick: () -> Unit,
) {

    /**
     * Top bar config
     *
     * @property onScanCardClick lambda be invoked when scan card button is clicked
     * @property onMoreClick     lambda be invoked when more button is clicked
     */
    data class TopBarConfig(val onScanCardClick: () -> Unit, val onMoreClick: () -> Unit)
}