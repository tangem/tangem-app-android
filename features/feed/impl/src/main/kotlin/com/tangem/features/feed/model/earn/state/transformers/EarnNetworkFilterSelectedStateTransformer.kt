package com.tangem.features.feed.model.earn.state.transformers

import com.tangem.features.feed.ui.earn.state.EarnFilterNetworkUM
import com.tangem.features.feed.ui.earn.state.EarnUM

internal class EarnNetworkFilterSelectedStateTransformer(
    private val filter: EarnFilterNetworkUM,
) : EarnUMTransformer {

    override fun transform(prevState: EarnUM): EarnUM {
        return prevState.copy(selectedNetworkFilter = filter)
    }
}