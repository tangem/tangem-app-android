package com.tangem.feature.swap.component.chooseprovider.model

import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.analytics.models.event.SwapAnalyticsEvent
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.domain.express.models.ProviderFilterType
import com.tangem.feature.swap.component.chooseprovider.ChooseProviderComponent
import com.tangem.feature.swap.domain.models.domain.ExchangeProviderType
import com.tangem.feature.swap.models.states.ChooseProviderBottomSheetConfig
import com.tangem.feature.swap.models.states.ChooseProviderSheetSource
import com.tangem.feature.swap.models.states.ProviderState
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@ModelScoped
internal class ChooseProviderModel @Inject constructor(
    override val dispatchers: CoroutineDispatcherProvider,
    private val analyticsEventHandler: AnalyticsEventHandler,
    paramsContainer: ParamsContainer,
) : Model() {

    private val params: ChooseProviderComponent.Params = paramsContainer.require()

    private val selectedFilter = MutableStateFlow(ProviderFilterType.ALL)

    val uiState: StateFlow<ChooseProviderBottomSheetConfig> = combine(
        params.source,
        selectedFilter,
        ::createUiState,
    ).stateIn(
        scope = modelScope,
        started = SharingStarted.Eagerly,
        initialValue = createUiState(params.source.value, selectedFilter.value),
    )

    fun onFilterSelect(filterType: ProviderFilterType) {
        analyticsEventHandler.send(
            SwapAnalyticsEvent.FilterProvider(
                filterType = when (filterType) {
                    ProviderFilterType.ALL -> "All"
                    ProviderFilterType.CEX -> "CEX"
                    ProviderFilterType.DEX -> "DEX"
                },
            ),
        )
        selectedFilter.value = filterType
    }

    private fun createUiState(
        source: ChooseProviderSheetSource,
        filterType: ProviderFilterType,
    ): ChooseProviderBottomSheetConfig {
        return ChooseProviderBottomSheetConfig(
            selectedProviderId = source.selectedProviderId,
            providers = source.allProviders.filter { matchesTypeFilter(it, filterType) }.toImmutableList(),
            allProviders = source.allProviders,
            notification = source.notification,
            selectedFilter = filterType,
            availableFilters = source.availableFilters,
            onFilterSelect = ::onFilterSelect,
        )
    }

    private fun matchesTypeFilter(state: ProviderState, filterType: ProviderFilterType): Boolean {
        val typeStr = when (state) {
            is ProviderState.Content -> state.type
            is ProviderState.Unavailable -> state.type
            else -> null
        } ?: return filterType == ProviderFilterType.ALL
        return when (filterType) {
            ProviderFilterType.ALL -> true
            ProviderFilterType.CEX -> typeStr == ExchangeProviderType.CEX.providerName
            ProviderFilterType.DEX -> typeStr == ExchangeProviderType.DEX.providerName ||
                typeStr == ExchangeProviderType.DEX_BRIDGE.providerName
        }
    }
}