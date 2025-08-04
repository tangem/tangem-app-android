package com.tangem.feature.wallet.presentation.organizetokens.utils.converter.items

import com.tangem.domain.models.tokenlist.TokenList
import com.tangem.feature.wallet.presentation.organizetokens.model.DraggableItem
import com.tangem.feature.wallet.presentation.organizetokens.model.OrganizeTokensListState
import com.tangem.feature.wallet.presentation.organizetokens.utils.common.uniteItems
import com.tangem.utils.converter.Converter
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.toPersistentList

internal class TokenListToListStateConverter(
    private val groupsConverter: NetworkGroupToDraggableItemsConverter,
    private val tokensConverter: CryptoCurrencyToDraggableItemConverter,
) : Converter<TokenList, OrganizeTokensListState> {

    override fun convert(value: TokenList): OrganizeTokensListState {
        return when (value) {
            is TokenList.GroupedByNetwork -> createListState(value)
            is TokenList.Ungrouped -> createListState(value)
            is TokenList.Empty -> createEmptyListState()
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

    @Suppress("UNCHECKED_CAST") // Erased type
    private fun createListState(tokenList: TokenList.Ungrouped): OrganizeTokensListState.Ungrouped {
        return OrganizeTokensListState.Ungrouped(
            items = tokensConverter.convertList(tokenList.currencies)
                .uniteItems()
                .toPersistentList() as PersistentList<DraggableItem.Token>,
        )
    }

    private fun createEmptyListState(): OrganizeTokensListState.Empty {
        return OrganizeTokensListState.Empty
    }
}