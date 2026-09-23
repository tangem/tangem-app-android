package com.tangem.features.collectibles.impl.discovery.model

import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.ui.ds2.search.TangemSearch
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.domain.collectibles.CollectibleGachaMachinesBatchAction
import com.tangem.domain.collectibles.CollectibleGachaMachinesBatchingContext
import com.tangem.domain.collectibles.models.CollectibleGachaMachineCategories
import com.tangem.domain.collectibles.models.CollectibleGachaMachinesListConfig
import com.tangem.domain.collectibles.usecase.GetCollectibleGachaMachineCategoriesUseCase
import com.tangem.domain.collectibles.usecase.GetCollectibleGachaMachinesBatchFlowUseCase
import com.tangem.features.collectibles.impl.CollectiblesRoute
import com.tangem.features.collectibles.impl.R
import com.tangem.features.collectibles.impl.discovery.model.converter.CollectiblesDiscoveryPackConverter
import com.tangem.features.collectibles.impl.discovery.model.converter.CollectiblesDiscoveryTabsConverter
import com.tangem.features.collectibles.impl.discovery.model.transformer.CollectiblesDiscoveryContentTransformer
import com.tangem.features.collectibles.impl.discovery.ui.state.CollectiblesDiscoveryUM
import com.tangem.pagination.BatchAction
import com.tangem.pagination.BatchFetchResult
import com.tangem.pagination.PaginationStatus
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.coroutines.JobHolder
import com.tangem.utils.coroutines.saveIn
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@ModelScoped
internal class CollectiblesDiscoveryModel @Inject constructor(
    override val dispatchers: CoroutineDispatcherProvider,
    private val getCategoriesUseCase: GetCollectibleGachaMachineCategoriesUseCase,
    private val router: Router,
    getMachinesBatchFlowUseCase: GetCollectibleGachaMachinesBatchFlowUseCase,
) : Model() {

    val uiState: StateFlow<CollectiblesDiscoveryUM>
        field = MutableStateFlow(buildInitialState())

    private val packConverter = CollectiblesDiscoveryPackConverter(onPackClick = ::onPackClick)
    private val tabsConverter = CollectiblesDiscoveryTabsConverter(onCategoryClick = ::onCategoryClick)

    // Replays the latest action: the first Reload is dispatched while the pagination is still subscribing, and a
    // shared flow without a replay cache drops what it cannot deliver yet.
    private val actionsFlow = MutableSharedFlow<CollectibleGachaMachinesBatchAction>(replay = 1)

    private val machinesBatchFlow = getMachinesBatchFlowUseCase(
        context = CollectibleGachaMachinesBatchingContext(actionsFlow = actionsFlow, coroutineScope = modelScope),
    )

    private var categories: CollectibleGachaMachineCategories? = null
    private var selectedCategoryId: String? = null

    private val categoriesJob = JobHolder()

    init {
        observeMachines()
        observeQuery()
        loadCategories()
    }

    fun onLoadMore() {
        modelScope.launch {
            when (val status = machinesBatchFlow.state.value.status) {
                is PaginationStatus.Paginating -> {
                    val lastResult = status.lastResult
                    // A failed page is worth another attempt as the user keeps scrolling; a last one is not.
                    val canLoadMore = lastResult !is BatchFetchResult.Success || !lastResult.last
                    if (canLoadMore) actionsFlow.emit(BatchAction.LoadMore())
                }
                else -> Unit
            }
        }
    }

    fun onRefresh() = reload()

    private fun buildInitialState(): CollectiblesDiscoveryUM {
        return CollectiblesDiscoveryUM(
            tabs = CollectiblesDiscoveryUM.TabsUM.Loading,
            content = CollectiblesDiscoveryUM.ContentUM.Loading,
            search = TangemSearch.State(
                placeholderText = resourceReference(R.string.common_search),
                query = "",
                onQueryChange = ::onQueryChange,
                isActive = false,
                onActiveChange = ::onSearchActiveChange,
                onClearClick = ::onClearQueryClick,
                onCloseClick = ::onCloseSearchClick,
            ),
        )
    }

    private fun loadCategories() {
        modelScope.launch {
            uiState.update { it.copy(tabs = CollectiblesDiscoveryUM.TabsUM.Loading) }

            getCategoriesUseCase()
                .onRight { loaded ->
                    categories = loaded
                    uiState.update { state ->
                        state.copy(
                            tabs = buildTabs(),
                            hasSearchableCatalogue = loaded.totalMachinesQuantity > 0,
                        )
                    }
                    reloadMachines()
                }
                .onLeft {
                    uiState.update { state ->
                        state.copy(
                            tabs = CollectiblesDiscoveryUM.TabsUM.Hidden,
                            content = CollectiblesDiscoveryUM.ContentUM.Error(
                                message = resourceReference(R.string.collectibles_main_search_load_error),
                                onReloadClick = ::reload,
                            ),
                        )
                    }
                }
        }.saveIn(categoriesJob)
    }

    private fun onCategoryClick(categoryId: String?) {
        if (categoryId == selectedCategoryId) return
        selectedCategoryId = categoryId
        // Highlight the tapped tab immediately; the list catches up once its first page arrives.
        uiState.update { it.copy(tabs = buildTabs()) }
        modelScope.launch { reloadMachines() }
    }

    private fun reload() {
        if (categories == null) {
            loadCategories()
        } else {
            modelScope.launch { reloadMachines() }
        }
    }

    private suspend fun reloadMachines() {
        val config = CollectibleGachaMachinesListConfig(
            categoryId = selectedCategoryId,
            search = uiState.value.search.query.trim()
                .takeIf(String::isNotEmpty),
        )
        actionsFlow.emit(BatchAction.Reload(requestParams = config))
    }

    private fun observeMachines() {
        machinesBatchFlow.state
            .onEach { batchState ->
                val transformer = CollectiblesDiscoveryContentTransformer(
                    batchListState = batchState,
                    packConverter = packConverter,
                    onReloadClick = ::reload,
                )
                uiState.update(transformer::transform)
            }
            // Building the whole rows list is list-sized work; keep it off the main thread.
            .flowOn(dispatchers.default)
            .launchIn(modelScope)
    }

    @OptIn(FlowPreview::class)
    private fun observeQuery() {
        uiState
            .map { it.search.query.trim() }
            .distinctUntilChanged()
            // The initial "" is not a query — the first load is driven by the categories.
            .drop(count = 1)
            .debounce(QUERY_DEBOUNCE_MILLIS)
            .onEach { reloadMachines() }
            .launchIn(modelScope)
    }

    private fun buildTabs(): CollectiblesDiscoveryUM.TabsUM {
        val loaded = categories ?: return CollectiblesDiscoveryUM.TabsUM.Hidden

        return CollectiblesDiscoveryUM.TabsUM.Content(
            tabs = tabsConverter.convert(
                value = CollectiblesDiscoveryTabsConverter.Input(
                    categories = loaded,
                    selectedCategoryId = selectedCategoryId,
                ),
            ),
        )
    }

    private fun onQueryChange(query: String) {
        uiState.update { it.copy(search = it.search.copy(query = query)) }
    }

    private fun onSearchActiveChange(isActive: Boolean) {
        uiState.update { it.copy(search = it.search.copy(isActive = isActive)) }
    }

    private fun onClearQueryClick() = onQueryChange(query = "")

    private fun onCloseSearchClick() {
        uiState.update { it.copy(search = it.search.copy(query = "", isActive = false)) }
    }

    private fun onPackClick(externalId: String) {
        router.push(CollectiblesRoute.MachineDetails(externalId = externalId))
    }

    private companion object {

        const val QUERY_DEBOUNCE_MILLIS = 500L
    }
}