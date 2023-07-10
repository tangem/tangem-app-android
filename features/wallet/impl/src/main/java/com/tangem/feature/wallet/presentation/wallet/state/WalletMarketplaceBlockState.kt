package com.tangem.feature.wallet.presentation.wallet.state

import com.tangem.feature.wallet.presentation.common.state.PriceChangeConfig

/**
 * Wallet marketplace component state
 *
 * @property currencyName currency name
 *
[REDACTED_AUTHOR]
 */
internal sealed class WalletMarketplaceBlockState(open val currencyName: String) {

    /**
     * Loading state
     *
     * @property currencyName currency name
     */
    data class Loading(override val currencyName: String) : WalletMarketplaceBlockState(currencyName = currencyName)

    /**
     * Content state
     *
     * @property currencyName      currency name
     * @property price             price
     * @property priceChangeConfig price change config
     */
    data class Content(
        override val currencyName: String,
        val price: String,
        val priceChangeConfig: PriceChangeConfig,
    ) : WalletMarketplaceBlockState(currencyName = currencyName)
}