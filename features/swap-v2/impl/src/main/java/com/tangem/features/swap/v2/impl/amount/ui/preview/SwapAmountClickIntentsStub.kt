package com.tangem.features.swap.v2.impl.amount.ui.preview

import com.tangem.features.swap.v2.impl.amount.entity.SwapAmountType
import com.tangem.features.swap.v2.impl.amount.model.SwapAmountClickIntents

internal object SwapAmountClickIntentsStub : SwapAmountClickIntents {
    override fun onExpandEditField(selectedAmountType: SwapAmountType) {}

    override fun onSelectTokenClick() {}

    override fun onAmountValueChange(value: String) {}

    override fun onAmountPasteTriggerDismiss() {}

    override fun onMaxValueClick() {}

    override fun onCurrencyChangeClick(isFiat: Boolean) {}

    override fun onAmountNext() {}
}