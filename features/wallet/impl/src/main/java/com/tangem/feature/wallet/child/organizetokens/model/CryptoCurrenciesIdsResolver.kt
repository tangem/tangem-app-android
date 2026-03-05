package com.tangem.feature.wallet.child.organizetokens.model

import com.tangem.domain.account.models.AccountStatusList
import com.tangem.domain.account.status.model.AccountCryptoCurrencies
import com.tangem.domain.models.account.filterCryptoPortfolio
import com.tangem.domain.models.tokenlist.TokenList
import com.tangem.feature.wallet.child.organizetokens.entity.DraggableItem
import com.tangem.feature.wallet.child.organizetokens.entity.OrganizeTokensListUM

internal class CryptoCurrenciesIdsResolver {

    fun resolveLegacy(
        tokensListUM: OrganizeTokensListUM,
        accountStatusList: AccountStatusList?,
    ): AccountCryptoCurrencies {
        if (accountStatusList == null) return emptyMap()

        val draggableTokens = when (tokensListUM) {
            OrganizeTokensListUM.EmptyList -> return emptyMap()
            is OrganizeTokensListUM.AccountList,
            is OrganizeTokensListUM.TokensList,
            -> tokensListUM.items.filterIsInstance<DraggableItem.Token>()
        }

        return accountStatusList.accountStatuses
            .filterCryptoPortfolio()
            .filter { it.tokenList != TokenList.Empty }
            .associate { accountStatus ->
                val currenciesById = accountStatus.flattenCurrencies().associateBy { it.currency.id.value }

                accountStatus.account to draggableTokens
                    .asSequence()
                    .filter { it.accountId == accountStatus.account.accountId.value }
                    .mapNotNull { token -> currenciesById[token.id]?.currency }
                    .toList()
            }
    }
}