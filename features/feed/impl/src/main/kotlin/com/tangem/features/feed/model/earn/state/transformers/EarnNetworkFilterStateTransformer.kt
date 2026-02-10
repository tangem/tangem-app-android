package com.tangem.features.feed.model.earn.state.transformers

import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.features.feed.ui.earn.state.EarnFilterNetworkUM
import com.tangem.features.feed.ui.earn.state.EarnUM

internal class EarnNetworkFilterStateTransformer(
    private val shouldShow: Boolean,
    private val filterByNetworkBottomSheetConfig: (EarnFilterNetworkUM, Boolean) -> TangemBottomSheetConfig,
) : EarnUMTransformer {

    override fun transform(prevState: EarnUM): EarnUM {
        return if (shouldShow) {
            prevState.copy(
                filterByNetworkBottomSheet = filterByNetworkBottomSheetConfig(prevState.selectedNetworkFilter, true),
            )
        } else {
            prevState.copy(filterByNetworkBottomSheet = prevState.filterByNetworkBottomSheet.copy(isShown = false))
        }
    }
}