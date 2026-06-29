package com.tangem.features.addressbook.selectnetworks.model

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchainsdk.utils.toNetworkId
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.features.addressbook.common.SupportedNetworksMatcher
import com.tangem.features.addressbook.selectnetworks.DefaultSelectNetworksComponent
import com.tangem.features.addressbook.selectnetworks.state.SelectNetworksStateController
import com.tangem.features.addressbook.selectnetworks.state.transformers.UpdateNetworksContentTransformer
import com.tangem.features.addressbook.selectnetworks.state.transformers.UpdateSelectNetworksInitialStateTransformer
import com.tangem.features.addressbook.selectnetworks.state.transformers.UpdateSelectNetworksSearchBarTransformer
import com.tangem.features.addressbook.selectnetworks.ui.state.SelectNetworksUM
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.*
import javax.inject.Inject

@Suppress("NamedArguments")
@ModelScoped
internal class SelectNetworksModel @Inject constructor(
    paramsContainer: ParamsContainer,
    supportedNetworksMatcher: SupportedNetworksMatcher,
    override val dispatchers: CoroutineDispatcherProvider,
    private val stateController: SelectNetworksStateController,
) : Model() {

    private val params: DefaultSelectNetworksComponent.Params = paramsContainer.require()
    private val query = MutableStateFlow("")
    private val isSearchActive = MutableStateFlow(false)

    private val matchedBlockchains: List<Blockchain> = supportedNetworksMatcher.match(params.address)

    private val selectedNetworks = MutableStateFlow(
        params.selectedNetworkIds.toSet().intersect(
            matchedBlockchains.map { blockchain -> blockchain.toNetworkId() }.toSet(),
        ),
    )

    val state: StateFlow<SelectNetworksUM> get() = stateController.uiState

    init {
        updateInitialState()
        subscribeToContent()
    }

    private fun updateInitialState() {
        stateController.update(
            UpdateSelectNetworksInitialStateTransformer(
                onQueryChange = ::onQueryChange,
                onActiveChange = ::onActiveChange,
                onBackClick = params.onBackClick,
                onDoneClick = ::onDoneClick,
            ),
        )
    }

    private fun subscribeToContent() {
        combine(query, selectedNetworks) { query, selection ->
            UpdateNetworksContentTransformer(
                matchedBlockchains = matchedBlockchains,
                query = query,
                selectedNetworkIds = selection,
                onToggle = ::onToggle,
            )
        }
            .onEach(stateController::update)
            .flowOn(dispatchers.default)
            .launchIn(modelScope)
    }

    private fun onQueryChange(value: String) {
        query.value = value
        updateSearchBar(query = value, isActive = isSearchActive.value)
    }

    private fun onActiveChange(isActive: Boolean) {
        isSearchActive.value = isActive
        updateSearchBar(query = query.value, isActive = isActive)
    }

    /** Reflects the search field immediately on the caller (main) thread, decoupled from the content recomputation. */
    private fun updateSearchBar(query: String, isActive: Boolean) {
        stateController.update(UpdateSelectNetworksSearchBarTransformer(query = query, isActive = isActive))
    }

    private fun onToggle(networkId: String) {
        val current = selectedNetworks.value
        selectedNetworks.value = if (networkId in current) current - networkId else current + networkId
    }

    private fun onDoneClick() {
        val selected = selectedNetworks.value
        if (selected.isEmpty()) return
        params.onDone(selected)
    }
}