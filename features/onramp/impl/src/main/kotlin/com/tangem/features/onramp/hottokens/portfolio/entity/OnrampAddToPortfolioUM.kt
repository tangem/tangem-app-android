package com.tangem.features.onramp.hottokens.portfolio.entity

import com.tangem.core.ui.components.currency.icon.CurrencyIconState
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.features.onramp.impl.R

/**
 * Onramp add to portfolio UM
 *
 * @property currencyName      currency name
 * @property networkName       network name
 * @property currencyIconState currency icon state
 * @property onAddClick        lambda be invoked when add button is clicked
 *
 * @author Andrew Khokhlov on 18/01/2025
 */
data class OnrampAddToPortfolioUM(
    val currencyName: String,
    val networkName: String,
    val currencyIconState: CurrencyIconState,
    val onAddClick: () -> Unit,
) {

    val bsTitle: TextReference = resourceReference(id = R.string.common_add_token)

    val subtitle: TextReference = resourceReference(
        id = R.string.hot_crypto_token_network,
        formatArgs = wrappedList(networkName),
    )

    val addButtonText: TextReference = resourceReference(R.string.common_add_to_portfolio)
}
