package com.tangem.features.send.v2.feeselector.model

import com.tangem.features.send.v2.api.entity.FeeItem

internal interface FeeSelectorIntents {
    fun onFeeItemSelected(feeItem: FeeItem)
    fun onCustomFeeValueChange(index: Int, value: String)
    fun onCustomFeeNextClick()
    fun onDoneClick()
}

internal class StubFeeSelectorIntents : FeeSelectorIntents {
    override fun onFeeItemSelected(feeItem: FeeItem) {}
    override fun onCustomFeeValueChange(index: Int, value: String) {}
    override fun onCustomFeeNextClick() {}
    override fun onDoneClick() {}
}