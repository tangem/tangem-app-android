package com.tangem.feature.wallet.presentation.organizetokens.utils

import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.tokenlist.TokenList
import com.tangem.feature.wallet.presentation.organizetokens.model.DraggableItem
import com.tangem.feature.wallet.presentation.organizetokens.model.OrganizeTokensListState

internal class CryptoCurrenciesIdsResolver {

    fun resolve(listState: OrganizeTokensListState, tokenList: TokenList?): List<CryptoCurrency.ID> {
        val draggableTokens = when (listState) {
            is OrganizeTokensListState.Empty -> return emptyList()
            is OrganizeTokensListState.GroupedByNetwork -> listState.items.filterIsInstance<DraggableItem.Token>()
            is OrganizeTokensListState.Ungrouped -> listState.items.filterIsInstance<DraggableItem.Token>()
        }
        val currenciesStatuses = when (tokenList) {
            is TokenList.GroupedByNetwork -> tokenList.groups.flatMap { it.currencies }
            is TokenList.Ungrouped -> tokenList.currencies
            is TokenList.Empty,
            null,
            -> return emptyList()
        }

        return draggableTokens.mapNotNull { draggableToken ->
            val currencyStatus = currenciesStatuses.firstOrNull {
                it.currency.id.value == draggableToken.id
            }

            currencyStatus?.currency?.id
        }
    }
}