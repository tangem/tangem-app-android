package com.tangem.tap.features.tokens.ui.compose.currencyItem

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import com.tangem.blockchain.common.Blockchain
import com.tangem.tap.domain.tokens.Currency
import com.tangem.tap.features.tokens.redux.ContractAddress
import com.tangem.tap.features.tokens.redux.TokenWithBlockchain

@Suppress("LongParameterList")
@Composable
fun CurrencyItem(
    currency: Currency,
    addedTokens: List<TokenWithBlockchain>,
    addedBlockchains: List<Blockchain>,
    allowToAdd: Boolean,
    isExpanded: Boolean,
    onCurrencyClick: () -> Unit,
    onAddCurrencyToggle: (Currency, TokenWithBlockchain?) -> Unit,
    onNetworkItemClick: (ContractAddress) -> Unit,
) {
    Column {
        CurrencyItemHeader(
            currency = currency,
            addedTokens = addedTokens,
            addedBlockchains = addedBlockchains,
            isExpanded = isExpanded,
            onCurrencyClick = onCurrencyClick,
        )
        CurrencyExpandedContent(
            currency = currency,
            addedTokens = addedTokens,
            addedBlockchains = addedBlockchains,
            allowToAdd = allowToAdd,
            isExpanded = isExpanded,
            onAddCurrencyToggle = onAddCurrencyToggle,
            onNetworkItemClick = onNetworkItemClick,
        )
    }
}
