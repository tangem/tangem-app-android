package com.tangem.feature.tokendetails.presentation.tokendetails.state.factory

import com.tangem.domain.tokens.model.TokenActionsState
import com.tangem.feature.tokendetails.presentation.tokendetails.state.TokenDetailsState
import com.tangem.feature.tokendetails.presentation.tokendetails.state.components.TokenDetailsActionButton
import com.tangem.feature.tokendetails.presentation.tokendetails.viewmodels.TokenDetailsClickIntents
import com.tangem.utils.Provider
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
            .mapNotNull { action ->
                when (action) {
                    is TokenActionsState.ActionState.Buy -> {
                        TokenDetailsActionButton.Buy(onClick = { clickIntents.onBuyClick(action.unavailabilityReason) })
                    }
                    is TokenActionsState.ActionState.Receive -> {
                        TokenDetailsActionButton.Receive(onClick = { clickIntents.onReceiveClick(action.unavailabilityReason) })
                    }
                    is TokenActionsState.ActionState.Sell -> {
                        TokenDetailsActionButton.Sell(onClick = { clickIntents.onSellClick(action.unavailabilityReason) })
                    }
                    is TokenActionsState.ActionState.Send -> {
                        TokenDetailsActionButton.Send(onClick = { clickIntents.onSendClick(action.unavailabilityReason) })
                    }
                    is TokenActionsState.ActionState.Swap -> {
                        TokenDetailsActionButton.Swap(onClick = { clickIntents.onSwapClick(action.unavailabilityReason) })
                    }
                    else -> {
                        null
                    }
                }
            }
            .toImmutableList()
    }
}
