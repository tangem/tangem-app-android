package com.tangem.feature.wallet.presentation.state

/**
 * Wallet state holder
 *
 * @property onBackClick  lambda be invoked when back button is clicked
 * @property headerConfig header config
 *
[REDACTED_AUTHOR]
 */
internal data class WalletStateHolder(
    val onBackClick: () -> Unit,
    val headerConfig: HeaderConfig,
) {

    /**
     * Header config
     *
     * @property wallets         list of wallets states
     * @property onScanCardClick lambda be invoked when scan card button is clicked
     * @property onMoreClick     lambda be invoked when more button is clicked
     */
    data class HeaderConfig(
        val wallets: List<WalletCardState>,
        val onScanCardClick: () -> Unit,
        val onMoreClick: () -> Unit,
    )
}