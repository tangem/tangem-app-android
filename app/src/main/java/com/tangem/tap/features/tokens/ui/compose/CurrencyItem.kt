package com.tangem.tap.features.tokens.ui.compose

import androidx.compose.runtime.Composable
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.Token
import com.tangem.tap.domain.tokens.Currency
import com.tangem.tap.features.tokens.redux.ContractAddress

@Composable
fun CurrencyItem(
    currency: Currency,
    nonRemovableTokens: List<ContractAddress>,
    nonRemovableBlockchains: List<Blockchain>,
    addedTokens: List<Token>,
    addedBlockchains: List<Blockchain>,
    allowToAdd: Boolean,
    expanded: Boolean,
    onCurrencyClick: (String) -> Unit,
    onAddCurrencyToggled: (Currency, Token?) -> Unit
) {
    if (expanded) {
        ExpandedCurrencyItem(
            currency = currency,
            nonRemovableTokens = nonRemovableTokens,
            nonRemovableBlockchains = nonRemovableBlockchains,
            addedTokens = addedTokens,
            addedBlockchains = addedBlockchains,
            allowToAdd = allowToAdd,
            onCurrencyClick = onCurrencyClick, onAddCurrencyToggled = onAddCurrencyToggled
        )
    } else {
        CollapsedCurrencyItem(
            currency = currency, addedTokens = addedTokens,
            addedBlockchains = addedBlockchains, onCurrencyClick
        )
    }
}