package com.tangem.tap.features.tokens.ui.compose

import androidx.compose.runtime.Composable
import com.tangem.blockchain.common.Blockchain
import com.tangem.tap.domain.tokens.Currency
import com.tangem.tap.features.tokens.redux.ContractAddress
import com.tangem.tap.features.tokens.redux.TokenWithBlockchain

@Composable
fun CurrencyItem(
    currency: Currency,
    addedTokens: List<TokenWithBlockchain>,
    addedBlockchains: List<Blockchain>,
    allowToAdd: Boolean,
    expanded: Boolean,
    onCurrencyClick: (String) -> Unit,
    onAddCurrencyToggled: (Currency, TokenWithBlockchain?) -> Unit,
    onNetworkItemClicked: (ContractAddress) -> Unit,
) {
    if (expanded) {
        ExpandedCurrencyItem(
            currency = currency,
            addedTokens = addedTokens,
            addedBlockchains = addedBlockchains,
            allowToAdd = allowToAdd,
            onCurrencyClick = onCurrencyClick,
            onAddCurrencyToggled = onAddCurrencyToggled,
            onNetworkItemClicked = onNetworkItemClicked,
        )
    } else {
        CollapsedCurrencyItem(
            currency = currency,
            addedTokens = addedTokens,
            addedBlockchains = addedBlockchains,
            onCurrencyClick,
        )
    }
}
