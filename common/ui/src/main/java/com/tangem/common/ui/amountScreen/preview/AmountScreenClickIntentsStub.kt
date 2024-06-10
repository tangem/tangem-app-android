package com.tangem.common.ui.amountScreen.preview

import com.tangem.common.ui.amountScreen.AmountScreenClickIntents

object AmountScreenClickIntentsStub : AmountScreenClickIntents {

    override fun onAmountValueChange(value: String) {}

    override fun onCurrencyChangeClick(isFiat: Boolean) {}

    override fun onMaxValueClick() {}

    override fun onAmountPasteTriggerDismiss() {}

    override fun onAmountNext() {}
}
