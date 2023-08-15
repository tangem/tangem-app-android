package com.tangem.feature.wallet.presentation.wallet.state.components

/**
 * Wallet screen top bar config
 *
 * @property onScanCardClick lambda be invoked when scan card button is clicked
 * @property onMoreClick     lambda be invoked when more button is clicked
 */
data class WalletTopBarConfig(val onScanCardClick: () -> Unit, val onMoreClick: () -> Unit)
