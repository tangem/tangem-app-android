package com.tangem.feature.tokendetails.presentation.tokendetails.state.transformer

import com.tangem.domain.models.StatusSource
import com.tangem.domain.tokens.model.ScenarioUnavailabilityReason
import com.tangem.domain.tokens.model.TokenActionsState
import com.tangem.domain.tokens.model.isLoading
import com.tangem.feature.tokendetails.presentation.tokendetails.model.TokenDetailsClickIntents
import com.tangem.feature.tokendetails.presentation.tokendetails.state.TokenDetailsUM
import com.tangem.feature.tokendetails.presentation.tokendetails.state.TransferUM
import com.tangem.utils.transformer.Transformer

internal class UpdateTransferTransformer(
    private val actions: List<TokenActionsState.ActionState>,
    private val networkSource: StatusSource,
    private val clickIntents: TokenDetailsClickIntents,
    private val onActionDispatched: () -> Unit,
) : Transformer<TokenDetailsUM> {

    override fun transform(prevState: TokenDetailsUM): TokenDetailsUM {
        val sendAction = actions.firstNotNullOfOrNull { it as? TokenActionsState.ActionState.Send }
        val swapAction = actions.firstNotNullOfOrNull { it as? TokenActionsState.ActionState.Swap }
        val sellAction = actions.firstNotNullOfOrNull { it as? TokenActionsState.ActionState.Sell }

        if (sendAction == null && swapAction == null && sellAction == null) return prevState

        val sendRow = sendAction?.let { action ->
            TransferUM.Row(
                isLoading = action.unavailabilityReason.isOutdatedLoading(),
                isEnabled = action.unavailabilityReason == ScenarioUnavailabilityReason.None,
                onClick = {
                    onActionDispatched()
                    clickIntents.onSendClick(action.unavailabilityReason)
                },
            )
        }
        val swapRow = swapAction?.let { action ->
            TransferUM.Row(
                isLoading = action.unavailabilityReason.isLoading,
                isEnabled = action.unavailabilityReason == ScenarioUnavailabilityReason.None,
                onClick = {
                    onActionDispatched()
                    clickIntents.onSwapFromClick(action.unavailabilityReason)
                },
            )
        }
        val swapAndSendRow = swapAction?.let { action ->
            TransferUM.Row(
                isLoading = action.unavailabilityReason.isLoading,
                isEnabled = action.unavailabilityReason == ScenarioUnavailabilityReason.None,
                onClick = {
                    onActionDispatched()
                    clickIntents.onSwapAndSendClick(action.unavailabilityReason)
                },
            )
        }
        val sellRow = sellAction?.let { action ->
            TransferUM.Row(
                isLoading = action.unavailabilityReason.isOutdatedLoading(),
                isEnabled = action.unavailabilityReason == ScenarioUnavailabilityReason.None,
                onClick = {
                    onActionDispatched()
                    clickIntents.onSellClick(action.unavailabilityReason)
                },
            )
        }

        return prevState.copy(
            transferUM = TransferUM.Content(
                send = sendRow,
                swap = swapRow,
                swapAndSend = swapAndSendRow,
                sell = sellRow,
            ),
        )
    }

    private fun ScenarioUnavailabilityReason.isOutdatedLoading(): Boolean =
        isLoading || this == ScenarioUnavailabilityReason.UsedOutdatedData && networkSource == StatusSource.CACHE
}