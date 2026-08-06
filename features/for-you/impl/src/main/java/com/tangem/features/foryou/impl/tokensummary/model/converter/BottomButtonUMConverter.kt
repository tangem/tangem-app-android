package com.tangem.features.foryou.impl.tokensummary.model.converter

import com.tangem.core.ui.R
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.features.foryou.impl.tokensummary.entity.BottomButtonUM
import com.tangem.features.foryou.impl.tokensummary.model.SwapHolding
import com.tangem.features.foryou.impl.tokensummary.model.SwapHoldingsState
import com.tangem.utils.converter.Converter

/**
 * Turns the resolved holdings into the bottom button, binding each outcome to what tapping it does:
 * [onAddToPortfolioClick] when the token is not held yet, [onAddFundsClick] when it is held but empty, and
 * [onSwapClick] with the holdings once there are any with funds.
 *
 * The add-to-portfolio offer needs a token the app knows how to add, so when [isAddToPortfolioAvailable] is `false` an
 * unheld token falls back to the disabled Swap button — there is nothing else the summary can offer for it.
 */
internal class BottomButtonUMConverter(
    private val isAddToPortfolioAvailable: Boolean,
    private val onAddToPortfolioClick: () -> Unit,
    private val onAddFundsClick: () -> Unit,
    private val onSwapClick: (List<SwapHolding>) -> Unit,
) : Converter<SwapHoldingsState, BottomButtonUM> {

    override fun convert(value: SwapHoldingsState): BottomButtonUM = when (value) {
        SwapHoldingsState.Loading -> BottomButtonUM.Loading
        SwapHoldingsState.NotHeld -> if (isAddToPortfolioAvailable) {
            addFundsButton(onClick = onAddToPortfolioClick)
        } else {
            disabledSwapButton()
        }
        SwapHoldingsState.ZeroBalance -> addFundsButton(onClick = onAddFundsClick)
        is SwapHoldingsState.Resolved -> BottomButtonUM.Content(
            text = resourceReference(R.string.token_summary_go_to_swap_button),
            isEnabled = true,
            onClick = { onSwapClick(value.holdings) },
        )
        SwapHoldingsState.Unavailable -> disabledSwapButton()
    }

    private fun addFundsButton(onClick: () -> Unit) = BottomButtonUM.Content(
        text = resourceReference(R.string.common_add_funds),
        isEnabled = true,
        onClick = onClick,
    )

    private fun disabledSwapButton() = BottomButtonUM.Content(
        text = resourceReference(R.string.token_summary_go_to_swap_button),
        isEnabled = false,
        onClick = {},
    )
}