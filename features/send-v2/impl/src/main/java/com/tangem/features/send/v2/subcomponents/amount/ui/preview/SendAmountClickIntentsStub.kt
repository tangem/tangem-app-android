package com.tangem.features.send.v2.subcomponents.amount.ui.preview

import com.tangem.features.send.v2.subcomponents.amount.model.SendAmountClickIntents

internal object SendAmountClickIntentsStub : SendAmountClickIntents {
    override fun onConvertToAnotherToken() {}

    override fun onAmountValueChange(value: String) {}

    override fun onAmountPasteTriggerDismiss() {}

    override fun onMaxValueClick() {}

    override fun onCurrencyChangeClick(isFiat: Boolean) {}

    override fun onAmountNext() {}
}