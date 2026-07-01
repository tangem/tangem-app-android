package com.tangem.feature.swap.ui

import com.tangem.common.ui.amountScreen.AmountScreenClickIntents
import com.tangem.feature.swap.models.UiActions

/**
 * Adapter that exposes legacy swap [UiActions] through the shared [AmountScreenClickIntents] contract
 * so the from-card amount field can be constructed by the common `AmountFieldConverter`.
 *
 * Only the callbacks that the swap amount field actually wires are mapped to real actions; the rest
 * are no-ops, because swap-v1 overrides the corresponding [com.tangem.common.ui.amountScreen.models.AmountFieldModel]
 * fields (keyboardActions / onValuePastedTriggerDismiss) after conversion to preserve its existing behaviour.
 */
internal class SwapAmountScreenClickIntents(
    private val actions: UiActions,
) : AmountScreenClickIntents {

    override fun onAmountValueChange(value: String) = actions.onAmountChanged(value)

    override fun onAmountPasteTriggerDismiss() = Unit

    override fun onMaxValueClick() = actions.onMaxAmountSelected()

    override fun onCurrencyChangeClick(isFiat: Boolean) = actions.onCurrencyChange(isFiat)

    override fun onAmountNext() = Unit
}