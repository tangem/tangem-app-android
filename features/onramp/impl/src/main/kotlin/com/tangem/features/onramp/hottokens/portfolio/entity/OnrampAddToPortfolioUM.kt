package com.tangem.features.onramp.hottokens.portfolio.entity

import com.tangem.core.ui.components.currency.icon.CurrencyIconState
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.features.onramp.impl.R

/**
 * Onramp add to portfolio UM
 *
 * @property walletName        wallet name
 * @property currencyName      currency name
 * @property networkName       network name
 * @property currencyIconState currency icon state
 * @property addButtonUM       add button UM
 *
[REDACTED_AUTHOR]
 */
data class OnrampAddToPortfolioUM(
    val walletName: String,
    val currencyName: String,
    val networkName: String,
    val currencyIconState: CurrencyIconState,
    val addButtonUM: AddButtonUM,
) {

    val bsTitle: TextReference = resourceReference(id = R.string.common_add_token)

    val bsSubtitle: TextReference = resourceReference(
        id = R.string.hot_crypto_add_token_subtitle,
        formatArgs = wrappedList(walletName),
    )

    val subtitle: TextReference = resourceReference(
        id = R.string.hot_crypto_token_network,
        formatArgs = wrappedList(networkName),
    )

    data class AddButtonUM(val isProgress: Boolean, val onClick: () -> Unit) {

        val text: TextReference = resourceReference(R.string.common_add_to_portfolio)
    }
}