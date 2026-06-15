package com.tangem.features.send.subcomponents.amount.model

import com.tangem.common.ui.amountScreen.AmountScreenClickIntents

interface SendAmountClickIntents : AmountScreenClickIntents {

    fun onConvertToAnotherToken()
}