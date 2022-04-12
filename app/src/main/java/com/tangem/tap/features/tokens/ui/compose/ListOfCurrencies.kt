package com.tangem.tap.features.tokens.ui.compose

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.tangem.blockchain.common.Blockchain
import com.tangem.tap.domain.tokens.Currency
import com.tangem.tap.features.tokens.redux.ContractAddress
import com.tangem.tap.features.tokens.redux.TokenWithBlockchain


@Composable
fun ListOfCurrencies(
    currencies: List<Currency>,
    nonRemovableTokens: List<ContractAddress>,
    nonRemovableBlockchains: List<Blockchain>,
    addedTokens: List<TokenWithBlockchain>,
    addedBlockchains: List<Blockchain>,
    searchInput: String,
    allowToAdd: Boolean,
    onAddCurrencyToggled: (Currency, TokenWithBlockchain?) -> Unit,
    onNetworkItemClicked: (ContractAddress) -> Unit
) {

    val expandedCurrencies = remember { mutableStateOf(listOf("")) }

    val onCurrencyClick = { currencyId: String ->
        val mutableList = expandedCurrencies.value.toMutableList()
        if (mutableList.contains(currencyId)) {
            mutableList.remove(currencyId)
        } else {
            mutableList.add(currencyId)
        }
        expandedCurrencies.value = mutableList
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
    ) {

        val filteredCurrencies = if (searchInput.isBlank()) {
            currencies
        } else {
            currencies.asSequence().filter {
                it.name.lowercase().contains(searchInput) || it.symbol.lowercase()
                    .contains(searchInput)
            }.toList()
        }
        items(filteredCurrencies) { currency ->
            CurrencyItem(
                currency = currency,
                nonRemovableTokens = nonRemovableTokens,
                nonRemovableBlockchains = nonRemovableBlockchains,
                addedTokens = addedTokens,
                addedBlockchains = addedBlockchains,
                allowToAdd = allowToAdd,
                expanded = expandedCurrencies.value.contains(currency.id),
                onCurrencyClick = onCurrencyClick,
                onAddCurrencyToggled = onAddCurrencyToggled,
                onNetworkItemClicked = onNetworkItemClicked
            )
        }

    }
}