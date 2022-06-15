package com.tangem.tap.features.tokens.ui.compose

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tangem.blockchain.common.Blockchain
import com.tangem.tap.common.compose.extensions.HideKeyboardOnScroll
import com.tangem.tap.common.compose.extensions.OnBottomReached
import com.tangem.tap.domain.tokens.Currency
import com.tangem.tap.features.tokens.redux.ContractAddress
import com.tangem.tap.features.tokens.redux.TokenWithBlockchain


@Composable
fun ListOfCurrencies(
    header: @Composable () -> Unit,
    currencies: List<Currency>,
    nonRemovableTokens: List<ContractAddress>,
    nonRemovableBlockchains: List<Blockchain>,
    addedTokens: List<TokenWithBlockchain>,
    addedBlockchains: List<Blockchain>,
    allowToAdd: Boolean,
    onAddCurrencyToggled: (Currency, TokenWithBlockchain?) -> Unit,
    onNetworkItemClicked: (ContractAddress) -> Unit,
    onLoadMore: () -> Unit
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

    val listState = rememberLazyListState()
    listState.HideKeyboardOnScroll()
    listState.OnBottomReached(loadMoreThreshold = 40) { onLoadMore() }

    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize(),
        contentPadding = PaddingValues(bottom = 90.dp)
    ) {
        item { header() }
        itemsIndexed(currencies) { index, currency ->
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

val Currency.fullName: String
    get() = "${this.name} (${this.symbol})"
