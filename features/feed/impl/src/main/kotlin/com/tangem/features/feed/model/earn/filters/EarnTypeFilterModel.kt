package com.tangem.features.feed.model.earn.filters

import androidx.compose.runtime.Stable
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.features.earn.api.EarnFeatureToggles
import com.tangem.features.feed.components.earn.EarnTypeFilterComponent
import com.tangem.features.feed.model.earn.analytics.EarnAnalyticsEvent
import com.tangem.features.feed.model.earn.analytics.FilterTypeAnalytic
import com.tangem.features.feed.model.earn.filters.state.EarnFilterTypeConverter
import com.tangem.features.feed.model.earn.filters.state.EarnFilterTypeUMConverter
import com.tangem.features.feed.ui.earn.state.EarnFilterByTypeBottomSheetContentUM
import com.tangem.features.feed.ui.earn.state.EarnFilterTypeUM
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@Stable
@ModelScoped
internal class EarnTypeFilterModel @Inject constructor(
    override val dispatchers: CoroutineDispatcherProvider,
    private val analyticsEventHandler: AnalyticsEventHandler,
    private val earnFeatureToggles: EarnFeatureToggles,
    paramsContainer: ParamsContainer,
) : Model() {

    private val params = paramsContainer.require<EarnTypeFilterComponent.Params>()

    private val _state = MutableStateFlow(configureInitialState())
    val state = _state.asStateFlow()

    private fun configureInitialState(): EarnFilterByTypeBottomSheetContentUM {
        return EarnFilterByTypeBottomSheetContentUM(
            options = EarnFilterTypeUM.entries
                .filter { it != EarnFilterTypeUM.Vaults || earnFeatureToggles.isEarnEnabled }
                .toImmutableList(),
            selectedOption = EarnFilterTypeConverter().convert(params.selectedFilter),
            onOptionClick = ::handleOnOptionClick,
        )
    }

    private fun handleOnOptionClick(filter: EarnFilterTypeUM) {
        params.onFilterSelected(EarnFilterTypeUMConverter().convert(filter))
        analyticsEventHandler.send(
            EarnAnalyticsEvent.BestOpportunitiesFilterTypeApplied(
                filterTypeAnalytic = when (filter) {
                    EarnFilterTypeUM.All -> FilterTypeAnalytic.ALL_TYPES
                    EarnFilterTypeUM.Staking -> FilterTypeAnalytic.STAKING
                    EarnFilterTypeUM.YieldMode -> FilterTypeAnalytic.YIELD
                    EarnFilterTypeUM.Vaults -> FilterTypeAnalytic.VAULTS
                },
            ),
        )
    }
}