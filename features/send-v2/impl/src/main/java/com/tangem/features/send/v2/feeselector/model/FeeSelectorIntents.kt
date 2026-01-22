package com.tangem.features.send.v2.feeselector.model

import androidx.compose.runtime.Stable
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.features.send.v2.api.entity.FeeItem

@Stable
internal interface FeeSelectorIntents {
    fun onFeeItemSelected(feeItem: FeeItem)
    fun onTokenSelected(status: CryptoCurrencyStatus)
    fun onCustomFeeValueChange(index: Int, value: String)
    fun onNonceChange(value: String)
    fun onDoneClick()
}

internal class StubFeeSelectorIntents : FeeSelectorIntents {
    override fun onFeeItemSelected(feeItem: FeeItem) {}
    override fun onTokenSelected(status: CryptoCurrencyStatus) {}
    override fun onCustomFeeValueChange(index: Int, value: String) {}
    override fun onNonceChange(value: String) {}
    override fun onDoneClick() {}
}