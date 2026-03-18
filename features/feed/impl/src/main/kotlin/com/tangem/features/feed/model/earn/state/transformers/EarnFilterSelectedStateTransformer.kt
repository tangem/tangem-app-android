package com.tangem.features.feed.model.earn.state.transformers

import com.tangem.domain.models.earn.EarnNetworks
import com.tangem.features.feed.ui.earn.state.EarnFilterNetworkUM
import com.tangem.features.feed.ui.earn.state.EarnFilterTypeUM
import com.tangem.features.feed.ui.earn.state.EarnUM

internal class EarnFilterSelectedStateTransformer(
    private val earnNetworks: EarnNetworks,
    private val filterType: EarnFilterTypeUM,
    private val filterNetwork: EarnFilterNetworkUM,
) : EarnUMTransformer {

    override fun transform(prevState: EarnUM): EarnUM {
        return prevState.copy(
            earnFilterUM = prevState.earnFilterUM.copy(
                selectedTypeFilter = filterType,
                selectedNetworkFilter = filterNetwork,
                isNetworkFilterEnabled = earnNetworks.isRight { networks -> networks.isNotEmpty() },
                isTypeFilterEnabled = true,
            ),
        )
    }
}