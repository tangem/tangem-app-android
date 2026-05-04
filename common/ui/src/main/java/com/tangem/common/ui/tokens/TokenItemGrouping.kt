package com.tangem.common.ui.tokens

import com.tangem.common.ui.R
import com.tangem.core.ui.components.tokenlist.state.TokensListItemUM
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.tokenlist.TokenList
import com.tangem.domain.models.tokenlist.TokenList.GroupedByNetwork.NetworkGroup

object TokenItemGrouping {

    fun TokenList.GroupedByNetwork.toGroupedItems(tokenConverter: TokenItemStateConverter): List<TokensListItemUM> {
        return groups.fold(initial = mutableListOf()) { acc, group ->
            acc.addGroup(tokenConverter, group)
        }
    }

    fun TokenList.Ungrouped.toUngroupedItems(tokenConverter: TokenItemStateConverter): List<TokensListItemUM> {
        return currencies.fold(initial = mutableListOf()) { acc, token ->
            acc.addToken(tokenConverter, token)
        }
    }

    fun MutableList<TokensListItemUM>.addGroup(
        tokenConverter: TokenItemStateConverter,
        group: NetworkGroup,
    ): MutableList<TokensListItemUM> {
        val groupTitle = TokensListItemUM.GroupTitle(
            id = group.network.hashCode(),
            text = resourceReference(
                id = R.string.wallet_network_group_title,
                formatArgs = wrappedList(group.network.name),
            ),
        )

        add(groupTitle)
        group.currencies.forEach { token -> addToken(tokenConverter, token) }

        return this
    }

    fun MutableList<TokensListItemUM>.addToken(
        tokenConverter: TokenItemStateConverter,
        token: CryptoCurrencyStatus,
    ): MutableList<TokensListItemUM> {
        val tokenItemState = tokenConverter.convert(token)

        add(TokensListItemUM.Token(tokenItemState))

        return this
    }
}