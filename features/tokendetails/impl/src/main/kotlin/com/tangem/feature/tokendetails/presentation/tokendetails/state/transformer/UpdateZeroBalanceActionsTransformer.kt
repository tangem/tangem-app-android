package com.tangem.feature.tokendetails.presentation.tokendetails.state.transformer

import com.tangem.domain.tokens.model.ScenarioUnavailabilityReason
import com.tangem.domain.tokens.model.TokenActionsState
import com.tangem.domain.tokens.model.isLoading
import com.tangem.feature.tokendetails.presentation.tokendetails.model.TokenDetailsClickIntents
import com.tangem.feature.tokendetails.presentation.tokendetails.state.TokenDetailsUM
import com.tangem.feature.tokendetails.presentation.tokendetails.state.ZeroBalanceActionsUM
import com.tangem.utils.transformer.Transformer

internal class UpdateZeroBalanceActionsTransformer(
    private val actions: List<TokenActionsState.ActionState>,
    private val clickIntents: TokenDetailsClickIntents,
) : Transformer<TokenDetailsUM> {

    override fun transform(prevState: TokenDetailsUM): TokenDetailsUM {
        val buyAction = actions.firstNotNullOfOrNull { it as? TokenActionsState.ActionState.Buy }
        val swapAction = actions.firstNotNullOfOrNull { it as? TokenActionsState.ActionState.Swap }
        val receiveAction = actions.firstNotNullOfOrNull { it as? TokenActionsState.ActionState.Receive }

        if (buyAction == null && swapAction == null && receiveAction == null) return prevState

        return prevState.copy(
            zeroBalanceActionsUM = ZeroBalanceActionsUM.Content(
                buy = buyAction?.toRow(onClick = clickIntents::onBuyClick),
                swap = swapAction?.toRow(onClick = clickIntents::onSwapToClick),
                receive = receiveAction?.toRow(
                    onClick = clickIntents::onReceiveClick,
                    onLongClick = { clickIntents.onCopyAddress() },
                ),
            ),
        )
    }

    private fun TokenActionsState.ActionState.toRow(
        onClick: (ScenarioUnavailabilityReason) -> Unit,
        onLongClick: (() -> Unit)? = null,
    ): ZeroBalanceActionsUM.Row {
        val reason = unavailabilityReason
        return ZeroBalanceActionsUM.Row(
            isLoading = reason.isLoading,
            isEnabled = reason == ScenarioUnavailabilityReason.None,
            onClick = { onClick(reason) },
            onLongClick = onLongClick,
        )
    }
}