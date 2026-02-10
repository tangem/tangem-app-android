package com.tangem.features.feed.model.earn.state.transformers

import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.features.feed.ui.earn.state.EarnFilterTypeUM
import com.tangem.features.feed.ui.earn.state.EarnUM

internal class EarnTypeFilterSelectedStateTransformer(
    private val filter: EarnFilterTypeUM,
    private val filterByTypeBottomSheetConfig: (EarnFilterTypeUM, Boolean) -> TangemBottomSheetConfig,
) : EarnUMTransformer {

    override fun transform(prevState: EarnUM): EarnUM {
        return prevState.copy(
            selectedTypeFilter = filter,
            filterByTypeBottomSheet = filterByTypeBottomSheetConfig(filter, false),
        )
    }
}