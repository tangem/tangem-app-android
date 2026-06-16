package com.tangem.feature.tokendetails.presentation.tokendetails.state.transformer

import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.res.TangemTheme
import com.tangem.domain.tokens.model.ScenarioUnavailabilityReason
import com.tangem.domain.tokens.model.TokenActionsState
import com.tangem.feature.tokendetails.presentation.tokendetails.model.TokenDetailsClickIntents
import com.tangem.feature.tokendetails.presentation.tokendetails.state.TokenDetailsUM
import com.tangem.utils.transformer.Transformer

internal class UpdateActionButtonsTransformer(
    private val actions: List<TokenActionsState.ActionState>,
    private val clickIntents: TokenDetailsClickIntents,
) : Transformer<TokenDetailsUM> {

    override fun transform(prevState: TokenDetailsUM): TokenDetailsUM {
        val swapAction = actions.firstNotNullOfOrNull { it as? TokenActionsState.ActionState.Swap }
            ?: return prevState

        val prev = prevState.balanceBlockUM
        val isSwapEnabled = swapAction.unavailabilityReason == ScenarioUnavailabilityReason.None

        val updated = prev.swapButton.copy(
            isEnabled = isSwapEnabled,
            tangemIconUM = (prev.swapButton.tangemIconUM as? TangemIconUM.Icon)?.copy(
                tint = {
                    if (isSwapEnabled) {
                        TangemTheme.colors2.graphic.neutral.primary
                    } else {
                        TangemTheme.colors2.graphic.neutral.quaternary
                    }
                },
            ) ?: prev.swapButton.tangemIconUM,
            onClick = { clickIntents.onSwapFromClick(swapAction.unavailabilityReason) },
        )

        return prevState.copy(balanceBlockUM = prev.copyButtons(swapButton = updated))
    }
}