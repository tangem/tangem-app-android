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
 * Model of the "Filter by network" sheet: the scope tabs and the list selection form a draft that
 * only reaches the tab (and the backend query) on Apply, so the Reset/Apply footer shows exactly
 * while the draft differs from the applied filter.
 */
@Stable
@ModelScoped
internal class EarnNetworkFilterModel @Inject constructor(
    override val dispatchers: CoroutineDispatcherProvider,
    private val analyticsEventHandler: AnalyticsEventHandler,
    paramsContainer: ParamsContainer,
) : Model() {

    private val params = paramsContainer.require<EarnNetworkFilterComponent.Params>()

    private val appliedSelection = Selection.from(filter = params.selectedFilter, networks = params.networks)

    // Round-tripped so the comparison ignores what the draft can't express: a specific network is
    // reachable from either scope, so switching tabs alone must not count as a change.
    private val appliedFilter = appliedSelection.toFilter(networks = params.networks)

    private var draftSelection = appliedSelection

    val state: StateFlow<EarnFilterByNetworkBottomSheetContentUM>
        field = MutableStateFlow(buildState())

    private fun onScopeClick(scope: EarnNetworkScope) {
        val networkId = draftSelection.networkId?.takeIf { id -> networksOf(scope).any { it.networkId == id } }
        draftSelection = Selection(scope = scope, networkId = networkId)
        state.update { buildState() }
    }

    private fun onOptionClick(networkId: String?) {
        draftSelection = draftSelection.copy(networkId = networkId)
        state.update { buildState() }
    }

    private fun onReset() {
        draftSelection = DEFAULT_SELECTION
        state.update { buildState() }
    }

    private fun onApply() {
        val filter = draftSelection.toFilter(networks = params.networks)
        val (networkId, filterType) = when (filter) {
            is EarnFilterNetwork.AllNetworks -> "" to FilterNetworkAnalytic.ALL_NETWORKS
            is EarnFilterNetwork.MyNetworks -> "" to FilterNetworkAnalytic.MY_NETWORKS
            is EarnFilterNetwork.Specific -> filter.id to FilterNetworkAnalytic.SPECIFIC
        }
        analyticsEventHandler.send(
            EarnAnalyticsEvent.BestOpportunitiesFilterNetworkApplied(
                networkId = networkId,
                filterType = filterType,
            ),
        )
        params.onFilterSelected(filter)
    }

    private fun buildState(): EarnFilterByNetworkBottomSheetContentUM {
        val scope = draftSelection.scope
        val allOption = EarnFilterNetworkUM.All(
            isSelected = draftSelection.networkId == null,
            onClick = { onOptionClick(networkId = null) },
        )
        val networkOptions = EarnNetworkToFilterUMConverter(
            selectedNetworkId = draftSelection.networkId,
            onClick = { network -> onOptionClick(networkId = network.networkId) },
        ).convertList(networksOf(scope))

        return EarnFilterByNetworkBottomSheetContentUM(
            scopeTabs = buildScopeTabs(selected = scope),
            networks = (listOf(allOption) + networkOptions).toImmutableList(),
            footer = EarnFilterFooterUM(onReset = ::onReset, onApply = ::onApply)
                .takeIf { draftSelection.toFilter(networks = params.networks) != appliedFilter },
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

    private companion object {
        val DEFAULT_SELECTION = Selection(scope = EarnNetworkScope.AllNetworks, networkId = null)
    }
}