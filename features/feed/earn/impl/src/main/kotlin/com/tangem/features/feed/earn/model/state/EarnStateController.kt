package com.tangem.features.feed.earn.model.state

import com.tangem.core.decompose.di.ModelScoped
import com.tangem.features.feed.earn.ui.state.EarnBestOpportunitiesUM
import com.tangem.features.feed.earn.ui.state.EarnFeedTabUM
import com.tangem.features.feed.earn.ui.state.EarnListUM
import com.tangem.utils.transformer.Transformer
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@ModelScoped
internal class EarnStateController @Inject constructor() {

    val uiState: StateFlow<EarnFeedTabUM>
        field = MutableStateFlow(value = getInitialState())

    fun update(transformer: Transformer<EarnFeedTabUM>) {
        uiState.update(function = transformer::transform)
    }

    private fun getInitialState(): EarnFeedTabUM {
        return EarnFeedTabUM(
            mostlyUsed = EarnListUM.Loading,
            bestOpportunities = EarnBestOpportunitiesUM.Loading,
            filters = persistentListOf(),
            onSliderScroll = {},
        )
    }
}