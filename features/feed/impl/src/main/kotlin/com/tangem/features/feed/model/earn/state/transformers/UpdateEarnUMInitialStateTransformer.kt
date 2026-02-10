package com.tangem.features.feed.model.earn.state.transformers

import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.features.feed.ui.earn.state.EarnUM

internal class UpdateEarnUMInitialStateTransformer(
    private val onBackClick: () -> Unit,
    private val filterByTypeBottomSheetConfig: TangemBottomSheetConfig,
    private val filterByNetworkBottomSheetConfig: TangemBottomSheetConfig,
    private val onNetworkFilterClick: () -> Unit,
    private val onTypeFilterClick: () -> Unit,
) : EarnUMTransformer {

    override fun transform(prevState: EarnUM): EarnUM {
        return prevState.copy(
            onBackClick = onBackClick,
            filterByNetworkBottomSheet = filterByNetworkBottomSheetConfig,
            filterByTypeBottomSheet = filterByTypeBottomSheetConfig,
            onNetworkFilterClick = onNetworkFilterClick,
            onTypeFilterClick = onTypeFilterClick,
        )
    }
}