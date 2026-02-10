package com.tangem.features.feed.model.earn.state.transformers

import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.features.feed.ui.earn.state.EarnFilterTypeUM
import com.tangem.features.feed.ui.earn.state.EarnUM

internal class EarnTypeFilterStateTransformer(
    private val shouldShow: Boolean,
    private val filterByTypeBottomSheetConfig: (EarnFilterTypeUM, Boolean) -> TangemBottomSheetConfig,
) : EarnUMTransformer {

    override fun transform(prevState: EarnUM): EarnUM {
        return if (shouldShow) {
            prevState.copy(
                filterByTypeBottomSheet = filterByTypeBottomSheetConfig(prevState.selectedTypeFilter, true),
            )
        } else {
            prevState.copy(filterByTypeBottomSheet = prevState.filterByTypeBottomSheet.copy(isShown = false))
        }
    }
}