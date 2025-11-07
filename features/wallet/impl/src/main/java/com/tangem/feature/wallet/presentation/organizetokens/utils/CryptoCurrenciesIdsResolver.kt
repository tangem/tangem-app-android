package com.tangem.feature.wallet.presentation.organizetokens.utils

import com.tangem.domain.account.models.AccountStatusList
import com.tangem.domain.account.status.model.AccountCryptoCurrencies
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.tokenlist.TokenList
import com.tangem.feature.wallet.presentation.organizetokens.model.DraggableItem
import com.tangem.feature.wallet.presentation.organizetokens.model.OrganizeTokensListState
import com.tangem.feature.wallet.presentation.organizetokens.model.OrganizeTokensListUM

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

    fun resolveV2(tokensListUM: OrganizeTokensListUM, accountStatusList: AccountStatusList?): AccountCryptoCurrencies {
        val draggableTokens = when (tokensListUM) {
            OrganizeTokensListUM.EmptyList -> return emptyMap()
            is OrganizeTokensListUM.AccountList,
            is OrganizeTokensListUM.TokensList,
            -> tokensListUM.items.filterIsInstance<DraggableItem.Token>()
        }

        return accountStatusList?.accountStatuses
            ?.filter { it.getCryptoTokenList() != TokenList.Empty }
            ?.associate { accountStatus ->
                val currencies = accountStatus.flattenCurrencies()
                accountStatus.account as Account.CryptoPortfolio to draggableTokens
                    .asSequence()
                    .filter { it.accountId == accountStatus.account.accountId.value }
                    .mapNotNull { sortedToken ->
                        currencies.firstOrNull { it.currency.id.value == sortedToken.id }?.currency
                    }
                    .toList()
            } ?: emptyMap()
    }
}