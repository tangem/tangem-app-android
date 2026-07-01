package com.tangem.features.feed.model.earn.state

import com.tangem.core.decompose.di.ModelScoped
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

    fun update(transformer: EarnUMTransformer) {
        mutableUiState.update(function = transformer::transform)
    }

    private fun getInitialState(): EarnUM {
        return EarnUM(
            mostlyUsed = EarnListUM.Loading,
            bestOpportunities = EarnBestOpportunitiesUM.Loading,
            earnFilterUM = EarnFilterUM(
                selectedTypeFilter = EarnFilterTypeUM.All,
                selectedNetworkFilter = EarnFilterNetworkUM.AllNetworks(isSelected = true),
            ),
            onBackClick = {},
            onNetworkFilterClick = {},
            onTypeFilterClick = {},
            onSliderScroll = {},
        )
    }
}