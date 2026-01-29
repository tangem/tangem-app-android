package com.tangem.feature.wallet.presentation.organizetokens.utils.converter.items

import com.tangem.domain.account.status.model.AccountCryptoCurrencyStatus
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.account.AccountId
import com.tangem.domain.models.tokenlist.TokenList.GroupedByNetwork.NetworkGroup
import com.tangem.feature.wallet.presentation.organizetokens.model.DraggableItem
import com.tangem.feature.wallet.presentation.organizetokens.utils.common.getGroupHeaderId
import com.tangem.feature.wallet.presentation.organizetokens.utils.common.getGroupPlaceholder
import com.tangem.utils.converter.Converter

internal class NetworkGroupToDraggableItemsConverterV2(
    private val itemConverter: CryptoCurrencyToDraggableItemConverterV2,
) : Converter<Pair<Account.Crypto.Portfolio, NetworkGroup>, List<DraggableItem>> {

    override fun convert(value: Pair<Account.Crypto.Portfolio, NetworkGroup>): List<DraggableItem> {
        val (account, networkGroup) = value
        return buildList {
            add(createGroupHeader(account.accountId, networkGroup))
            addAll(createTokens(account, networkGroup))
        }
    }

    override fun convertList(
        input: Collection<Pair<Account.Crypto.Portfolio, NetworkGroup>>,
    ): List<List<DraggableItem>> {
        return input.mapIndexed { index, pair ->
            convert(pair).toMutableList()
                .also { mutableGroup ->
                    mutableGroup.add(
                        getGroupPlaceholder(accountId = pair.first.accountId.value, index = index),
                    )
                }
        }
    }

    private fun createGroupHeader(accountId: AccountId, group: NetworkGroup) = DraggableItem.GroupHeader(
        id = getGroupHeaderId(group.network),
        accountId = accountId.value,
        networkName = group.network.name,
    )

    private fun createTokens(account: Account.Crypto.Portfolio, group: NetworkGroup): List<DraggableItem.Token> {
        return itemConverter.convertList(
            group.currencies.map {
                AccountCryptoCurrencyStatus(
                    account = account,
                    status = it,
                )
            },
        )
    }
}