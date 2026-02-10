package com.tangem.features.feed.model.earn.state.transformers

import com.tangem.common.R
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.extensions.TextReference
import com.tangem.features.feed.ui.earn.state.EarnFilterNetworkUM
import com.tangem.features.feed.ui.earn.state.EarnFilterTypeUM
import com.tangem.features.feed.ui.earn.state.EarnUM

internal class ClearEarnUMFilterStateTransformer(
    private val filterByTypeBottomSheetConfig: (EarnFilterTypeUM, Boolean) -> TangemBottomSheetConfig,
    private val filterByNetworkBottomSheetConfig: (EarnFilterNetworkUM, Boolean) -> TangemBottomSheetConfig,
) : EarnUMTransformer {

    override fun transform(prevState: EarnUM): EarnUM {
        val defaultNetworksFilter = EarnFilterNetworkUM.AllNetworks(
            text = TextReference.Res(R.string.earn_filter_all_networks),
            isSelected = true,
        )

        return prevState.copy(
            selectedTypeFilter = EarnFilterTypeUM.All,
            selectedNetworkFilter = defaultNetworksFilter,
            filterByTypeBottomSheet = filterByTypeBottomSheetConfig(EarnFilterTypeUM.All, false),
            filterByNetworkBottomSheet = filterByNetworkBottomSheetConfig(defaultNetworksFilter, false),
        )
    }
}