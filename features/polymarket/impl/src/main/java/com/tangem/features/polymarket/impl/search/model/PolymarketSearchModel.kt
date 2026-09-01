package com.tangem.features.polymarket.impl.search.model

import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.domain.polymarket.model.PolymarketSearchBatchingContext
import com.tangem.domain.polymarket.model.PolymarketSearchConfig
import com.tangem.domain.polymarket.usecase.SearchPolymarketEventsUseCase
import com.tangem.features.polymarket.impl.main.model.converter.PolymarketEventUMConverter
import com.tangem.features.polymarket.impl.navigation.PolymarketRoute
import com.tangem.features.polymarket.impl.search.PolymarketSearchComponent
import com.tangem.features.polymarket.impl.search.model.transformer.PolymarketSearchContentTransformer
import com.tangem.features.polymarket.impl.search.ui.state.PolymarketSearchUM
import com.tangem.pagination.BatchAction
import com.tangem.pagination.BatchFetchResult
import com.tangem.pagination.PaginationStatus
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Model of the events search screen.
 *
 * Search-as-you-type: every settled query — debounced and at least [MIN_QUERY_LENGTH] characters long —
 * restarts the paginated search; a shorter one resets it back to the "start typing" prompt.
 */
@ModelScoped
internal class PolymarketSearchModel @Inject constructor(
    paramsContainer: ParamsContainer,
    private val router: Router,
    override val dispatchers: CoroutineDispatcherProvider,
    searchPolymarketEventsUseCase: SearchPolymarketEventsUseCase,
) : Model() {

    private val params = paramsContainer.require<PolymarketSearchComponent.Params>()

    val uiState: StateFlow<PolymarketSearchUM>
        field = MutableStateFlow(
            PolymarketSearchUM(
                query = "",
                onQueryChange = ::onQueryChange,
                onCloseClick = ::onCloseClick,
                content = PolymarketSearchUM.ContentUM.Initial,
            ),
        )

    private val eventUMConverter = PolymarketEventUMConverter(
        onEventClick = ::onEventClick,
        onOutcomeClick = ::onOutcomeClick,
    )

    // Replays the latest action: a Reload can be dispatched while the pagination is still subscribing,
    // and a shared flow without a replay cache drops what it cannot deliver yet.
    private val actionsFlow = MutableSharedFlow<BatchAction<Int, PolymarketSearchConfig, Nothing>>(replay = 1)

    private val searchBatchFlow = searchPolymarketEventsUseCase(
        context = PolymarketSearchBatchingContext(actionsFlow = actionsFlow, coroutineScope = modelScope),
    )

    init {
        observeQuery()
        observeResults()
    }

    /**
     * Requests the next page once the results are scrolled close enough to their end. Ignored while a page
     * is already on its way, and while the results are empty or broken — those restart via the query.
     */
    fun onLoadMore() {
        modelScope.launch {
            when (val status = searchBatchFlow.state.value.status) {
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

    private fun onQueryChange(query: String) {
        uiState.update { it.copy(query = query) }
    }

    @OptIn(FlowPreview::class)
    private fun observeQuery() {
        uiState
            .map { it.query.trim() }
            .distinctUntilChanged()
            .debounce(QUERY_DEBOUNCE_MILLIS)
            .onEach { query ->
                if (query.length >= MIN_QUERY_LENGTH) {
                    actionsFlow.emit(BatchAction.Reload(requestParams = PolymarketSearchConfig(query = query)))
                } else {
                    // The reset alone is not enough: a state that is already empty re-emits nothing,
                    // so the prompt is set directly as well.
                    actionsFlow.emit(BatchAction.Reset)
                    uiState.update { it.copy(content = PolymarketSearchUM.ContentUM.Initial) }
                }
            }
            .launchIn(modelScope)
    }

    private fun observeResults() {
        searchBatchFlow.state
            .onEach { batchState ->
                val transformer = PolymarketSearchContentTransformer(
                    batchListState = batchState,
                    isQueryActive = uiState.value.query.trim().length >= MIN_QUERY_LENGTH,
                    eventUMConverter = eventUMConverter,
                    onReloadClick = ::reloadCurrentQuery,
                )
                uiState.update(transformer::transform)
            }
            // Building the whole results UM is list-sized work; keep it off the main thread.
            .flowOn(dispatchers.default)
            .launchIn(modelScope)
    }

    private fun reloadCurrentQuery() {
        modelScope.launch {
            val query = uiState.value.query.trim()
            if (query.length >= MIN_QUERY_LENGTH) {
                actionsFlow.emit(BatchAction.Reload(requestParams = PolymarketSearchConfig(query = query)))
            }
        }
    }

    private fun onCloseClick() {
        router.pop()
    }

    private fun onEventClick(eventId: String) {
        router.push(PolymarketRoute.EventDetails(eventId = eventId, userWalletId = params.userWalletId))
    }

    private fun onOutcomeClick(eventId: String, marketId: String, assetId: String) {
        router.push(
            PolymarketRoute.EventDetails(
                eventId = eventId,
                userWalletId = params.userWalletId,
                marketId = marketId,
                assetId = assetId,
            ),
        )
    }

    private companion object {
        const val QUERY_DEBOUNCE_MILLIS = 500L

        // The BFF consistently rejects queries shorter than 3 characters.
        const val MIN_QUERY_LENGTH = 3
    }
}