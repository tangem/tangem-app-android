package com.tangem.features.polymarket.impl.main.model

import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.domain.polymarket.model.PolymarketCategory
import com.tangem.domain.polymarket.usecase.GetPolymarketCategoriesUseCase
import com.tangem.domain.polymarket.usecase.GetPolymarketEventsUseCase
import com.tangem.features.polymarket.impl.main.model.converter.PolymarketEventUMConverter
import com.tangem.features.polymarket.impl.main.ui.state.PolymarketCategoryTabUM
import com.tangem.features.polymarket.impl.main.ui.state.PolymarketMainUM
import com.tangem.features.polymarket.impl.navigation.PolymarketRoute
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.coroutines.JobHolder
import com.tangem.utils.coroutines.saveIn
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Model of the Discovery feed screen.
 *
 * Loads the category tabs once, then (re)loads the events feed for the selected category.
 */
@ModelScoped
internal class PolymarketMainModel @Inject constructor(
    paramsContainer: ParamsContainer,
    private val router: Router,
    override val dispatchers: CoroutineDispatcherProvider,
    private val getPolymarketEventsUseCase: GetPolymarketEventsUseCase,
    private val getPolymarketCategoriesUseCase: GetPolymarketCategoriesUseCase,
) : Model() {

    private val params = paramsContainer.require<PolymarketMainParams>()

    val uiState: StateFlow<PolymarketMainUM>
        field = MutableStateFlow(
            PolymarketMainUM(accessMode = params.accessMode, content = PolymarketMainUM.ContentUM.Loading),
        )

    private val converter = PolymarketEventUMConverter(
        onEventClick = ::onEventClick,
        onOutcomeClick = ::onOutcomeClick,
    )

    private var categories: List<PolymarketCategory> = emptyList()
    private var selectedCategoryId: Int? = null

    private val loadJob = JobHolder()

    init {
        load()
    }

    fun onBackClick() {
        router.pop()
    }

    /** Initial load: fetch the category tabs, then the events of the first category. */
    private fun load() {
        modelScope.launch {
            uiState.update { it.copy(content = PolymarketMainUM.ContentUM.Loading) }

            val result = withContext(dispatchers.default) { getPolymarketCategoriesUseCase() }
            result.fold(
                ifLeft = {
                    uiState.update { current ->
                        current.copy(content = PolymarketMainUM.ContentUM.Error(onRetryClick = ::load))
                    }
                },
                ifRight = { loadedCategories ->
                    categories = loadedCategories
                    selectedCategoryId = loadedCategories.firstOrNull()?.id
                    loadEvents(selectedCategoryId)
                },
            )
        }.saveIn(loadJob)
    }

    private fun onCategorySelected(categoryId: Int) {
        if (categoryId == selectedCategoryId) return
        selectedCategoryId = categoryId
        // Highlight the tapped tab immediately; the feed refreshes once its events arrive.
        uiState.update { current ->
            val content = current.content
            if (content is PolymarketMainUM.ContentUM.Content) {
                current.copy(content = content.copy(categories = buildTabs()))
            } else {
                current
            }
        }
        loadEvents(categoryId)
    }

    private fun loadEvents(category: Int?) {
        modelScope.launch {
            val newContent = withContext(dispatchers.default) {
                getPolymarketEventsUseCase(category = category).fold(
                    ifLeft = { PolymarketMainUM.ContentUM.Error(onRetryClick = ::load) },
                    ifRight = { events ->
                        if (events.isEmpty()) {
                            PolymarketMainUM.ContentUM.Empty
                        } else {
                            PolymarketMainUM.ContentUM.Content(
                                categories = buildTabs(),
                                events = converter.convertList(events).toImmutableList(),
                            )
                        }
                    },
                )
            }
            uiState.update { it.copy(content = newContent) }
        }.saveIn(loadJob)
    }

    private fun buildTabs(): ImmutableList<PolymarketCategoryTabUM> = categories
        .map { category ->
            PolymarketCategoryTabUM(
                id = category.id,
                label = category.label,
                isSelected = category.id == selectedCategoryId,
                onClick = { onCategorySelected(category.id) },
            )
        }
        .toImmutableList()

    private fun onEventClick(eventId: String) {
        router.push(PolymarketRoute.EventDetails(eventId = eventId))
    }

    private fun onOutcomeClick(eventId: String, marketId: String, assetId: String) {
        router.push(PolymarketRoute.EventDetails(eventId = eventId, marketId = marketId, assetId = assetId))
    }
}