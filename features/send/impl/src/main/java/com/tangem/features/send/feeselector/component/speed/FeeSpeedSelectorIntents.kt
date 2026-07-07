package com.tangem.features.send.feeselector.component.speed

import androidx.compose.runtime.Stable
import com.tangem.features.send.feeselector.model.FeeSelectorIntents
import com.tangem.features.send.feeselector.model.StubFeeSelectorIntents

@Stable
internal interface FeeSpeedSelectorIntents : FeeSelectorIntents {
    fun onLearnMoreClick()
}

@Stable
internal class StubFeeSpeedSelectorIntents : FeeSpeedSelectorIntents, FeeSelectorIntents by StubFeeSelectorIntents() {
    override fun onLearnMoreClick() {}
}