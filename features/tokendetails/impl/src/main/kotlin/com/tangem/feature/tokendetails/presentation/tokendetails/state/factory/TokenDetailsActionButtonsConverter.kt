package com.tangem.feature.tokendetails.presentation.tokendetails.state.factory

import com.tangem.common.Provider
import com.tangem.domain.tokens.model.TokenActionsState
import com.tangem.feature.tokendetails.presentation.tokendetails.state.TokenDetailsState
import com.tangem.feature.tokendetails.presentation.tokendetails.state.components.TokenDetailsActionButton
import com.tangem.feature.tokendetails.presentation.tokendetails.viewmodels.TokenDetailsClickIntents
import com.tangem.utils.converter.Converter
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

internal class TokenDetailsActionButtonsConverter(
    private val currentStateProvider: Provider<TokenDetailsState>,
    private val clickIntents: TokenDetailsClickIntents,
) : Converter<List<TokenActionsState.ActionState>, TokenDetailsState> {

    override fun convert(value: List<TokenActionsState.ActionState>): TokenDetailsState {
        val state = currentStateProvider()
        return state.copy(
            tokenBalanceBlockState = state.tokenBalanceBlockState.copyActionButtons(value.mapToManageButtons()),
        )
    }

    private fun List<TokenActionsState.ActionState>.mapToManageButtons(): ImmutableList<TokenDetailsActionButton> {
        return this
            .map { action ->
                when (action) {
                    is TokenActionsState.ActionState.Buy -> {
                        TokenDetailsActionButton.Buy(enabled = action.enabled, onClick = clickIntents::onBuyClick)
                    }
                    is TokenActionsState.ActionState.Receive -> {
                        TokenDetailsActionButton.Receive(onClick = clickIntents::onReceiveClick)
                    }
                    is TokenActionsState.ActionState.Sell -> {
                        TokenDetailsActionButton.Sell(enabled = action.enabled, onClick = clickIntents::onSellClick)
                    }
                    is TokenActionsState.ActionState.Send -> {
                        TokenDetailsActionButton.Send(enabled = action.enabled, onClick = clickIntents::onSendClick)
                    }
                    is TokenActionsState.ActionState.Swap -> {
                        TokenDetailsActionButton.Swap(enabled = action.enabled, onClick = clickIntents::onSwapClick)
                    }
                }
            }
            .toImmutableList()
    }
}