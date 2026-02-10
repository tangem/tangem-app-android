package com.tangem.features.feed.model.earn.state

import com.tangem.common.R
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.extensions.TextReference
import com.tangem.features.feed.model.earn.state.transformers.EarnUMTransformer
import com.tangem.features.feed.ui.earn.state.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@ModelScoped
internal class EarnStateController @Inject constructor() {

    private val mutableUiState: MutableStateFlow<EarnUM> = MutableStateFlow(value = getInitialState())

    val uiState: StateFlow<EarnUM> get() = mutableUiState.asStateFlow()

    val value: EarnUM get() = uiState.value

    fun update(function: (EarnUM) -> EarnUM) {
        mutableUiState.update(function = function)
    }

    fun update(transformer: EarnUMTransformer) {
        mutableUiState.update(function = transformer::transform)
    }

    private fun getInitialState(): EarnUM {
        return EarnUM(
            mostlyUsed = EarnListUM.Loading,
            bestOpportunities = EarnBestOpportunitiesUM.Loading,
            selectedTypeFilter = EarnFilterTypeUM.All,
            selectedNetworkFilter = EarnFilterNetworkUM.AllNetworks(
                text = TextReference.Res(R.string.earn_filter_all_networks),
                isSelected = true,
            ),
            filterByTypeBottomSheet = TangemBottomSheetConfig.Empty,
            filterByNetworkBottomSheet = TangemBottomSheetConfig.Empty,
            onBackClick = {},
            onNetworkFilterClick = {},
            onTypeFilterClick = {},
        )
    }
}