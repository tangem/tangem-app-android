package com.tangem.features.feed.model.earn.state.transformers

import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.features.feed.ui.earn.state.EarnFilterNetworkUM
import com.tangem.features.feed.ui.earn.state.EarnUM

internal class EarnNetworkFilterSelectedStateTransformer(
    private val filter: EarnFilterNetworkUM,
    private val filterByNetworkBottomSheetConfig: (EarnFilterNetworkUM, Boolean) -> TangemBottomSheetConfig,
) : EarnUMTransformer {

    override fun transform(prevState: EarnUM): EarnUM {
        return prevState.copy(
            selectedNetworkFilter = filter,
            filterByNetworkBottomSheet = filterByNetworkBottomSheetConfig(filter, false),
        )
    }
}