package com.tangem.features.foryou.impl.tokensummary.model.converter

import com.tangem.common.ui.markets.tokenselector.TokenSelectorEntry
import com.tangem.core.ui.R
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.features.foryou.impl.tokensummary.entity.BottomButtonUM
import com.tangem.features.foryou.impl.tokensummary.model.SwapHoldingsState
import com.tangem.utils.converter.Converter

/**
 * Turns the resolved holdings into the bottom button, binding each outcome to what tapping it does:
 * [onAddFundsClick] when the token is held but empty, [onSwapClick] with the swappable holdings when there are any.
 */
internal class BottomButtonUMConverter(
    private val onAddFundsClick: () -> Unit,
    private val onSwapClick: (List<TokenSelectorEntry>) -> Unit,
) : Converter<SwapHoldingsState, BottomButtonUM> {

    override fun convert(value: SwapHoldingsState): BottomButtonUM = when (value) {
        SwapHoldingsState.Loading -> BottomButtonUM.Loading
        SwapHoldingsState.ZeroBalance -> BottomButtonUM.Content(
            text = resourceReference(R.string.common_add_funds),
            isEnabled = true,
            onClick = onAddFundsClick,
        )
        is SwapHoldingsState.Available -> BottomButtonUM.Content(
            text = resourceReference(R.string.token_summary_go_to_swap_button),
            isEnabled = true,
            onClick = { onSwapClick(value.entries) },
        )
        SwapHoldingsState.Unavailable -> BottomButtonUM.Content(
            text = resourceReference(R.string.token_summary_go_to_swap_button),
            isEnabled = false,
            onClick = {},
        )
    }
}