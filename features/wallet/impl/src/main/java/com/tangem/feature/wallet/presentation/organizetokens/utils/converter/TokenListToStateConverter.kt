package com.tangem.feature.wallet.presentation.organizetokens.utils.converter

import com.tangem.common.Provider
import com.tangem.domain.tokens.model.TokenList
import com.tangem.feature.wallet.presentation.organizetokens.OrganizeTokensListState
import com.tangem.feature.wallet.presentation.organizetokens.OrganizeTokensState
import com.tangem.feature.wallet.presentation.organizetokens.utils.converter.items.TokenListToListStateConverter
import com.tangem.utils.converter.Converter

internal class TokenListToStateConverter(
    private val currentState: Provider<OrganizeTokensState>,
    private val itemsConverter: TokenListToListStateConverter,
) : Converter<TokenList, OrganizeTokensState> {

    override fun convert(value: TokenList): OrganizeTokensState {
        val state = currentState()
        val itemsState = itemsConverter.convert(value)

        return state.copy(
            itemsState = itemsState,
            header = state.header.copy(
                isEnabled = itemsState !is OrganizeTokensListState.Empty,
                isSortedByBalance = value.sortedBy == TokenList.SortType.BALANCE,
                isGrouped = value is TokenList.GroupedByNetwork,
            ),
            actions = state.actions.copy(
                canApply = itemsState !is OrganizeTokensListState.Empty,
            ),
        )
    }
}
