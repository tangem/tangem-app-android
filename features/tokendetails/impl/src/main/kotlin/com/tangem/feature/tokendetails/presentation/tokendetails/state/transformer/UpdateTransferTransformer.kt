package com.tangem.feature.tokendetails.presentation.tokendetails.state.transformer

import com.tangem.common.ui.tokens.rendersActionAsEnabled
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.analytics.models.event.TransferAnalyticsEvent
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
    private val analyticsEventHandler: AnalyticsEventHandler,
    private val onActionDispatched: () -> Unit,
    private val isJointAccount: Boolean = false,
) : Transformer<TokenDetailsUM> {

    override fun transform(prevState: TokenDetailsUM): TokenDetailsUM {
        val sendAction = actions.firstNotNullOfOrNull { it as? TokenActionsState.ActionState.Send }
        val swapAction = actions.firstNotNullOfOrNull { it as? TokenActionsState.ActionState.Swap }
        val sellAction = actions.firstNotNullOfOrNull { it as? TokenActionsState.ActionState.Sell }

        if (sendAction == null && swapAction == null && sellAction == null) return prevState

        val sendRow = sendAction?.let { action ->
            TransferUM.Row(
                isLoading = action.unavailabilityReason.isOutdatedLoading(),
                isEnabled = action.unavailabilityReason.rendersActionAsEnabled(),
                onClick = {
                    analyticsEventHandler.send(TransferAnalyticsEvent.ButtonSend())
                    onActionDispatched()
                    clickIntents.onSendClick(action.unavailabilityReason)
                },
            )
        }
        val swapRow = swapAction?.let { action ->
            TransferUM.Row(
                isLoading = action.unavailabilityReason.isLoading,
                isEnabled = action.unavailabilityReason.rendersActionAsEnabled(),
                onClick = {
                    analyticsEventHandler.send(TransferAnalyticsEvent.ButtonSwap())
                    onActionDispatched()
                    clickIntents.onSwapClick(action.unavailabilityReason)
                },
            )
        }
        // Send&Swap is only meaningful while Swap itself keeps the enabled look, so it is shown only in that case
        // (mirrors the main-screen Transfer quick actions, which synthesize Send&Swap the same way).
        // This prevents a disabled Send&Swap row for cards that cannot swap at all (e.g. S2C single-currency cards);
        // a joint account's row stays, and the tap explains the reason.
        val swapAndSendRow = swapAction
            ?.takeIf { it.unavailabilityReason.rendersActionAsEnabled() }
            ?.let { action ->
                val clickReason = if (isJointAccount) {
                    ScenarioUnavailabilityReason.JointAccount
                } else {
                    action.unavailabilityReason
                }
                TransferUM.Row(
                    isLoading = false,
                    isEnabled = true,
                    onClick = {
                        analyticsEventHandler.send(TransferAnalyticsEvent.ButtonSwapAndSend())
                        onActionDispatched()
                        clickIntents.onSwapAndSendClick(clickReason)
                    },
                )
            }
        val sellRow = sellAction?.let { action ->
            TransferUM.Row(
                isLoading = action.unavailabilityReason.isOutdatedLoading(),
                isEnabled = action.unavailabilityReason.rendersActionAsEnabled(),
                onClick = {
                    analyticsEventHandler.send(TransferAnalyticsEvent.ButtonSell())
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