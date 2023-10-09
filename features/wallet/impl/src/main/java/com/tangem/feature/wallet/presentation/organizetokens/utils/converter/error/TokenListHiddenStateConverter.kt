package com.tangem.feature.wallet.presentation.organizetokens.utils.converter.error

import com.tangem.common.Provider
import com.tangem.feature.wallet.presentation.organizetokens.model.DraggableItem
import com.tangem.feature.wallet.presentation.organizetokens.model.OrganizeTokensListState
import com.tangem.utils.converter.Converter
import kotlinx.collections.immutable.toPersistentList

internal class TokenListHiddenStateConverter(
    private val currentStateProvider: Provider<OrganizeTokensListState>,
) : Converter<Boolean, OrganizeTokensListState> {

    override fun convert(input: Boolean): OrganizeTokensListState {
        val currentState = currentStateProvider()
        val isBalanceHidden = input

        return currentState.copySealed(
            currentState.items.map { draggableItem ->
                if (draggableItem is DraggableItem.Token) {
                    draggableItem.copy(
                        tokenItemState = draggableItem.tokenItemState.copy(
                            isBalanceHidden = isBalanceHidden,
                        ),
                    )
                } else {
                    draggableItem
                }
            }.toPersistentList(),
        )
    }
}