package com.tangem.features.send.v2.feeselector.component.token

import androidx.compose.runtime.Stable
import com.tangem.features.send.v2.feeselector.model.FeeSelectorIntents
import com.tangem.features.send.v2.feeselector.model.StubFeeSelectorIntents

@Stable
internal interface FeeTokenSelectorIntents : FeeSelectorIntents {
    fun onLearnMoreClick()
}

@Stable
internal class StubFeeTokenSelectorIntents : FeeTokenSelectorIntents, FeeSelectorIntents by StubFeeSelectorIntents() {
    override fun onLearnMoreClick() {}
}