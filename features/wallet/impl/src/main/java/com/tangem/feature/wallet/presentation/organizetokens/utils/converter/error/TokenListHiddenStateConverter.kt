package com.tangem.feature.wallet.presentation.organizetokens.utils.converter.error

import com.tangem.feature.wallet.presentation.common.state.TokenItemState
import com.tangem.feature.wallet.presentation.organizetokens.model.DraggableItem
import com.tangem.feature.wallet.presentation.organizetokens.model.OrganizeTokensListState
import com.tangem.utils.converter.Converter
import kotlinx.collections.immutable.toPersistentList

internal class TokenListHiddenStateConverter :
    Converter<Pair<OrganizeTokensListState, Boolean>, OrganizeTokensListState> {

    override fun convert(input: Pair<OrganizeTokensListState, Boolean>): OrganizeTokensListState {
        val currentState = input.first
        val isBalanceHidden = input.second

        return currentState.copySealed(
            currentState.items.map { draggableItem ->
                if (draggableItem is DraggableItem.Token) {
                    if (draggableItem.tokenItemState.info is TokenItemState.DraggableItemInfo.Balance) {
                        draggableItem.copy(
                            tokenItemState = draggableItem.tokenItemState.copy(
                                info = draggableItem.tokenItemState.info.copy(
                                    balance = draggableItem.tokenItemState.info.balance,
                                    isBalanceHidden = isBalanceHidden,
                                ),
                            ),
                        )
                    } else {
                        draggableItem
                    }
                } else {
                    draggableItem
                }
            }.toPersistentList(),
        )
    }
}
