package com.tangem.feature.tokendetails.presentation.tokendetails.state.transformer

import com.tangem.domain.tokens.model.ScenarioUnavailabilityReason
import com.tangem.domain.tokens.model.TokenActionsState
import com.tangem.domain.tokens.model.isLoading
import com.tangem.feature.tokendetails.presentation.tokendetails.model.TokenDetailsClickIntents
import com.tangem.feature.tokendetails.presentation.tokendetails.state.AddFundsUM
import com.tangem.feature.tokendetails.presentation.tokendetails.state.TokenDetailsUM
import com.tangem.utils.transformer.Transformer

internal class UpdateAddFundsTransformer(
    private val actions: List<TokenActionsState.ActionState>,
    private val clickIntents: TokenDetailsClickIntents,
    private val onActionDispatched: () -> Unit,
) : Transformer<TokenDetailsUM> {

    override fun transform(prevState: TokenDetailsUM): TokenDetailsUM {
        val buyAction = actions.firstNotNullOfOrNull { it as? TokenActionsState.ActionState.Buy }
        val swapAction = actions.firstNotNullOfOrNull { it as? TokenActionsState.ActionState.Swap }
        val receiveAction = actions.firstNotNullOfOrNull { it as? TokenActionsState.ActionState.Receive }

        if (buyAction == null && swapAction == null && receiveAction == null) return prevState

        val buyRow = buyAction?.let { action ->
            AddFundsUM.Row(
                isLoading = action.unavailabilityReason.isLoading,
                isEnabled = action.unavailabilityReason == ScenarioUnavailabilityReason.None,
                onClick = {
                    onActionDispatched()
                    clickIntents.onBuyClick(action.unavailabilityReason)
                },
            )
        }
        val swapRow = swapAction?.let { action ->
            AddFundsUM.Row(
                isLoading = action.unavailabilityReason.isLoading,
                isEnabled = action.unavailabilityReason == ScenarioUnavailabilityReason.None,
                onClick = {
                    onActionDispatched()
                    clickIntents.onSwapToClick(action.unavailabilityReason)
                },
            )
        }
        val receiveRow = receiveAction?.let { action ->
            AddFundsUM.Row(
                isLoading = action.unavailabilityReason.isLoading,
                isEnabled = action.unavailabilityReason == ScenarioUnavailabilityReason.None,
                onClick = {
                    onActionDispatched()
                    clickIntents.onReceiveClick(action.unavailabilityReason)
                },
                onLongClick = {
                    onActionDispatched()
                    clickIntents.onCopyAddress()
                },
            )
        }

        return prevState.copy(
            addFundsUM = AddFundsUM.Content(buy = buyRow, swap = swapRow, receive = receiveRow),
        )
    }
}