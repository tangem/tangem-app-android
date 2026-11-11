package com.tangem.features.feed.earn.model.filters

import androidx.compose.runtime.Stable
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.ui.ds2.tabnavigation.TangemTabItemUM
import com.tangem.domain.earn.model.EarnFilterNetwork
import com.tangem.domain.models.earn.EarnNetwork
import com.tangem.features.feed.earn.components.EarnNetworkFilterComponent
import com.tangem.features.feed.earn.model.analytics.EarnAnalyticsEvent
import com.tangem.features.feed.earn.model.analytics.FilterNetworkAnalytic
import com.tangem.features.feed.earn.model.filters.state.EarnNetworkToFilterUMConverter
import com.tangem.features.feed.earn.ui.state.EarnFilterByNetworkBottomSheetContentUM
import com.tangem.features.feed.earn.ui.state.EarnFilterFooterUM
import com.tangem.features.feed.earn.ui.state.EarnFilterNetworkUM
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

/**
 * Model of the "Filter by network" sheet: a tap on an option applies it and the tab closes the sheet,
 * so "All" doubles as reset. Switching to the other scope tab is a choice of scope: it checks that tab's
 * "All" and offers Apply; switching back to the tab the sheet opened on restores what it opened with.
 */
@Stable
@ModelScoped
internal class EarnNetworkFilterModel @Inject constructor(
    override val dispatchers: CoroutineDispatcherProvider,
    private val analyticsEventHandler: AnalyticsEventHandler,
    paramsContainer: ParamsContainer,
) : Model() {

    private val params = paramsContainer.require<EarnNetworkFilterComponent.Params>()

    private val openedSelection = Selection.from(filter = params.selectedFilter, networks = params.networks)

    private var viewedSelection = openedSelection

    val state: StateFlow<EarnFilterByNetworkBottomSheetContentUM>
        field = MutableStateFlow(buildState())

    private fun onScopeClick(scope: EarnNetworkScope) {
        viewedSelection = if (scope == openedSelection.scope) {
            openedSelection
        } else {
            Selection(scope = scope, networkId = null)
        }
        state.update { buildState() }
    }

    private fun onOptionClick(networkId: String?) {
        applyFilter(viewedSelection.copy(networkId = networkId).toFilter(networks = params.networks))
    }

    private fun onApply() {
        applyFilter(viewedSelection.toFilter(networks = params.networks))
    }

    private fun applyFilter(filter: EarnFilterNetwork) {
        val (analyticsNetworkId, filterType) = when (filter) {
            is EarnFilterNetwork.AllNetworks -> "" to FilterNetworkAnalytic.ALL_NETWORKS
            is EarnFilterNetwork.MyNetworks -> "" to FilterNetworkAnalytic.MY_NETWORKS
            is EarnFilterNetwork.Specific -> filter.id to FilterNetworkAnalytic.SPECIFIC
        }
        analyticsEventHandler.send(
            EarnAnalyticsEvent.BestOpportunitiesFilterNetworkApplied(
                networkId = analyticsNetworkId,
                filterType = filterType,
            ),
        )
        params.onFilterSelected(filter)
    }

    private fun buildState(): EarnFilterByNetworkBottomSheetContentUM {
        val scope = viewedSelection.scope
        val allOption = EarnFilterNetworkUM.All(
            isSelected = viewedSelection.networkId == null,
            onClick = { onOptionClick(networkId = null) },
        )
        val networkOptions = EarnNetworkToFilterUMConverter(
            selectedNetworkId = viewedSelection.networkId,
            onClick = { network -> onOptionClick(networkId = network.networkId) },
        ).convertList(networksOf(scope))

        return EarnFilterByNetworkBottomSheetContentUM(
            scopeTabs = buildScopeTabs(selected = scope),
            networks = (listOf(allOption) + networkOptions).toImmutableList(),
            footer = EarnFilterFooterUM(onApply = ::onApply)
                .takeIf { viewedSelection != openedSelection },
        )
    }

    private fun buildScopeTabs(selected: EarnNetworkScope) = EarnNetworkScope.entries
        .map<EarnNetworkScope, TangemTabItemUM> { scope ->
            TangemTabItemUM.Content(
                id = scope.name,
                label = scope.title,
                isSelected = scope == selected,
                onClick = { onScopeClick(scope) },
            )
        }
        .toImmutableList()

    private fun networksOf(scope: EarnNetworkScope): List<EarnNetwork> = when (scope) {
        EarnNetworkScope.AllNetworks -> params.networks
        EarnNetworkScope.MyNetworks -> params.networks.filter(EarnNetwork::isAdded)
    }

    /** @property networkId `null` means the scope's "All" option is selected. */
    private data class Selection(
        val scope: EarnNetworkScope,
        val networkId: String?,
    ) {

        fun toFilter(networks: List<EarnNetwork>): EarnFilterNetwork {
            val network = networkId?.let { id -> networks.firstOrNull { it.networkId == id } }
            return when {
                network != null -> EarnFilterNetwork.Specific(
                    isSelected = true,
                    id = network.networkId,
                    symbol = network.symbol,
                    fullName = network.fullName,
                )
                scope == EarnNetworkScope.MyNetworks -> EarnFilterNetwork.MyNetworks(isSelected = true)
                else -> EarnFilterNetwork.AllNetworks(isSelected = true)
            }
        }

        companion object {

            fun from(filter: EarnFilterNetwork, networks: List<EarnNetwork>): Selection = when (filter) {
                is EarnFilterNetwork.AllNetworks -> Selection(EarnNetworkScope.AllNetworks, networkId = null)
                is EarnFilterNetwork.MyNetworks -> Selection(EarnNetworkScope.MyNetworks, networkId = null)
                is EarnFilterNetwork.Specific -> Selection(
                    // A specific network is scope-agnostic, so the sheet opens on the narrowest tab holding it.
                    scope = if (networks.any { it.networkId == filter.id && it.isAdded }) {
                        EarnNetworkScope.MyNetworks
                    } else {
                        EarnNetworkScope.AllNetworks
                    },
                    networkId = filter.id,
                )
            }
        }
    }
}