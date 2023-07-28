package com.tangem.feature.wallet.presentation.organizetokens.utils.converter.items

import com.tangem.domain.tokens.model.TokenList
import com.tangem.feature.wallet.presentation.organizetokens.OrganizeTokensListState
import com.tangem.feature.wallet.presentation.organizetokens.utils.common.uniteItems
import com.tangem.utils.converter.Converter
import kotlinx.collections.immutable.toPersistentList

internal class TokenListToListStateConverter(
    private val groupsConverter: NetworkGroupToDraggableItemsConverter,
    private val tokensConverter: CryptoCurrencyToDraggableItemConverter,
) : Converter<TokenList, OrganizeTokensListState> {

    override fun convert(value: TokenList): OrganizeTokensListState {
        return when (value) {
            is TokenList.GroupedByNetwork -> createListState(value)
            is TokenList.Ungrouped -> createListState(value)
            is TokenList.NotInitialized -> createEmptyListState()
        }
    }

    private fun createListState(tokenList: TokenList.GroupedByNetwork): OrganizeTokensListState.GroupedByNetwork {
        return OrganizeTokensListState.GroupedByNetwork(
            items = groupsConverter.convertList(tokenList.groups)
                .flatten()
                .uniteItems()
                .toPersistentList(),
        )
    }

    private fun createListState(tokenList: TokenList.Ungrouped): OrganizeTokensListState.Ungrouped {
        return OrganizeTokensListState.Ungrouped(
            items = tokensConverter.convertList(tokenList.currencies)
                .uniteItems()
                .toPersistentList(),
        )
    }

    private fun createEmptyListState(): OrganizeTokensListState.Empty {
        return OrganizeTokensListState.Empty
    }
}
