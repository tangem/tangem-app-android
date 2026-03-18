package com.tangem.feature.wallet.child.organizetokens.model.converter.items

import com.tangem.domain.account.status.model.AccountCryptoCurrencyStatus
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.account.AccountId
import com.tangem.domain.models.tokenlist.TokenList.GroupedByNetwork.NetworkGroup
import com.tangem.feature.wallet.child.organizetokens.entity.DraggableItem
import com.tangem.feature.wallet.child.organizetokens.model.common.getGroupHeaderId
import com.tangem.feature.wallet.child.organizetokens.model.common.getGroupPlaceholderLegacy
import com.tangem.utils.converter.Converter

internal class NetworkGroupToDraggableItemsConverter(
    private val itemConverter: CryptoCurrencyToDraggableItemConverter,
) : Converter<Pair<Account.CryptoPortfolio, NetworkGroup>, List<DraggableItem>> {

    override fun convert(value: Pair<Account.CryptoPortfolio, NetworkGroup>): List<DraggableItem> {
        val (account, networkGroup) = value
        return buildList {
            add(createGroupHeader(account.accountId, networkGroup))
            addAll(createTokens(account, networkGroup))
        }
    }

    override fun convertList(
        input: Collection<Pair<Account.CryptoPortfolio, NetworkGroup>>,
    ): List<List<DraggableItem>> {
        return input.mapIndexed { index, pair ->
            convert(pair).toMutableList()
                .also { mutableGroup ->
                    mutableGroup.add(
                        getGroupPlaceholderLegacy(accountId = pair.first.accountId.value, index = index),
                    )
                }
        }
    }

    private fun createGroupHeader(accountId: AccountId, group: NetworkGroup) = DraggableItem.GroupHeader(
        id = getGroupHeaderId(group.network),
        accountId = accountId.value,
        networkName = group.network.name,
    )

    private fun createTokens(account: Account.CryptoPortfolio, group: NetworkGroup): List<DraggableItem.Token> {
        return itemConverter.convertList(
            group.currencies.map { currencyStatus ->
                AccountCryptoCurrencyStatus(
                    account = account,
                    status = currencyStatus,
                )
            },
        )
    }
}