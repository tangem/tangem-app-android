package com.tangem.features.feed.model.earn.filters

import androidx.compose.runtime.Stable
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.features.feed.components.earn.EarnNetworkFilterComponent
import com.tangem.features.feed.model.earn.analytics.EarnAnalyticsEvent
import com.tangem.features.feed.model.earn.analytics.FilterNetworkAnalytic
import com.tangem.features.feed.model.earn.filters.state.EarnFilterNetworkConverter
import com.tangem.features.feed.model.earn.filters.state.EarnFilterNetworkUMConverter
import com.tangem.features.feed.ui.earn.state.EarnFilterByNetworkBottomSheetContentUM
import com.tangem.features.feed.ui.earn.state.EarnFilterNetworkUM
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@Stable
@ModelScoped
internal class EarnNetworkFilterModel @Inject constructor(
    override val dispatchers: CoroutineDispatcherProvider,
    private val analyticsEventHandler: AnalyticsEventHandler,
    paramsContainer: ParamsContainer,
) : Model() {

    private val params = paramsContainer.require<EarnNetworkFilterComponent.Params>()

    private val _state = MutableStateFlow(configureInitialState())
    val state = _state.asStateFlow()

    private fun configureInitialState(): EarnFilterByNetworkBottomSheetContentUM {
        return EarnFilterByNetworkBottomSheetContentUM(
            networks = params.allFilters
                .map { EarnFilterNetworkConverter().convert(it) }
                .toPersistentList(),
            onOptionClick = ::handleOnOptionClick,
        )
    }

    private fun handleOnOptionClick(filter: EarnFilterNetworkUM) {
        params.onFilterSelected(EarnFilterNetworkUMConverter().convert(filter))
        val (networkId, filterType) = when (filter) {
            is EarnFilterNetworkUM.AllNetworks -> "" to FilterNetworkAnalytic.ALL_NETWORKS
            is EarnFilterNetworkUM.MyNetworks -> "" to FilterNetworkAnalytic.MY_NETWORKS
            is EarnFilterNetworkUM.Network -> filter.id to FilterNetworkAnalytic.SPECIFIC
        }
        analyticsEventHandler.send(
            EarnAnalyticsEvent.BestOpportunitiesFilterNetworkApplied(
                networkId = networkId,
                filterType = filterType,
            ),
        )
    }
}