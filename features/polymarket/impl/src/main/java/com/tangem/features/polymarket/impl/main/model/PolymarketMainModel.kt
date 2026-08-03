package com.tangem.features.polymarket.impl.main.model

import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.domain.polymarket.model.PolymarketAccessMode
import com.tangem.domain.polymarket.usecase.GetPolymarketEventsUseCase
import com.tangem.features.polymarket.impl.main.model.converter.PolymarketEventUMConverter
import com.tangem.features.polymarket.impl.main.ui.state.PolymarketMainUM
import com.tangem.features.polymarket.impl.navigation.PolymarketRoute
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.coroutines.JobHolder
import com.tangem.utils.coroutines.saveIn
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
 * The events are served from fixtures while the feature is built UI-first, see `MockPolymarketRepository`.
 *
 * User-visible captions of the event card are English literals for now — the Lokalise keys
 * (`predictions_event_total_volume`, `predictions_event_probability`, `predictions_event_more_outcomes`)
 * do not exist yet, and the feature stays behind a disabled toggle until they do.
 */
@ModelScoped
internal class PolymarketMainModel @Inject constructor(
    paramsContainer: ParamsContainer,
    private val router: Router,
    override val dispatchers: CoroutineDispatcherProvider,
    private val getPolymarketEventsUseCase: GetPolymarketEventsUseCase,
) : Model() {

    private val accessMode = paramsContainer.require<PolymarketAccessMode>()

    val uiState: StateFlow<PolymarketMainUM>
        field = MutableStateFlow(
            PolymarketMainUM(accessMode = accessMode, content = PolymarketMainUM.ContentUM.Loading),
        )

    private val converter = PolymarketEventUMConverter(
        onEventClick = ::onEventClick,
        onOutcomeClick = ::onOutcomeClick,
    )

    private val loadEventsJob = JobHolder()

    init {
        loadEvents()
    }

    private fun loadEvents() {
        modelScope.launch {
            uiState.update { it.copy(content = PolymarketMainUM.ContentUM.Loading) }

            val newContent = withContext(dispatchers.default) {
                getPolymarketEventsUseCase().fold(
                    ifLeft = { PolymarketMainUM.ContentUM.Error(onRetryClick = ::loadEvents) },
                    ifRight = { events ->
                        if (events.isEmpty()) {
                            PolymarketMainUM.ContentUM.Empty
                        } else {
                            PolymarketMainUM.ContentUM.Content(
                                events = converter.convertList(events).toImmutableList(),
                            )
                        }
                    },
                )
            }

            uiState.update { it.copy(content = newContent) }
        }.saveIn(loadEventsJob)
    }

    private fun onEventClick(eventId: String) {
        router.push(PolymarketRoute.EventDetails(eventId = eventId, accessMode = accessMode))
    }

    private fun onOutcomeClick(eventId: String, marketId: String, assetId: String) {
        router.push(
            PolymarketRoute.EventDetails(
                eventId = eventId,
                marketId = marketId,
                assetId = assetId,
                accessMode = accessMode,
            ),
        )
    }
}