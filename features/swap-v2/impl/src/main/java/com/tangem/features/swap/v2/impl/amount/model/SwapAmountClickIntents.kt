package com.tangem.features.swap.v2.impl.amount.model

import com.tangem.common.ui.amountScreen.AmountScreenClickIntents
import com.tangem.features.swap.v2.impl.amount.entity.SwapAmountType

internal interface SwapAmountClickIntents : AmountScreenClickIntents {

    fun onExpandEditField(selectedAmountType: SwapAmountType)
    fun onInfoClick()
    fun onSelectTokenClick()
    fun onSeparatorClick()
    fun onProviderClick()
}