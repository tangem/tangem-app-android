package com.tangem.features.polymarket.impl.search.model.transformer

import com.tangem.domain.polymarket.model.PolymarketEvent
import com.tangem.domain.polymarket.model.PolymarketSearchBatchListState
import com.tangem.features.polymarket.impl.main.model.converter.PolymarketEventUMConverter
import com.tangem.features.polymarket.impl.main.ui.state.PolymarketFeedFooterUM
import com.tangem.features.polymarket.impl.search.ui.state.PolymarketSearchUM
import com.tangem.pagination.BatchFetchResult
import com.tangem.pagination.PaginationStatus
import com.tangem.utils.transformer.Transformer
import kotlinx.collections.immutable.toImmutableList

/**
 * Applies the pagination state of a search query to the results area of the screen; the query field of the
 * previous state stays as it is.
 *
 * While the query is too short to search, the area shows the "start typing" prompt whatever the pagination
 * holds — stale pages of an abandoned query must not survive its clearing.
 *
 * @property batchListState pagination state to render
 * @property isQueryActive whether the current query is long enough to be searched
 * @property eventUMConverter converts the events of the loaded pages into cards
 * @property onReloadClick retries the current query from its first page
 * @property onNextPageRetryClick retries the failed page while keeping the loaded events
 */
internal class PolymarketSearchContentTransformer(
    private val batchListState: PolymarketSearchBatchListState,
    private val isQueryActive: Boolean,
    private val eventUMConverter: PolymarketEventUMConverter,
    private val onReloadClick: () -> Unit,
    private val onNextPageRetryClick: () -> Unit,
) : Transformer<PolymarketSearchUM> {

    override fun transform(prevState: PolymarketSearchUM): PolymarketSearchUM {
        return prevState.copy(content = transformContent())
    }

    private fun transformContent(): PolymarketSearchUM.ContentUM {
        if (!isQueryActive) return PolymarketSearchUM.ContentUM.Initial

        return when (val status = batchListState.status) {
            is PaginationStatus.None,
            is PaginationStatus.InitialLoading,
            -> PolymarketSearchUM.ContentUM.Loading
            is PaginationStatus.InitialLoadingError ->
                PolymarketSearchUM.ContentUM.Error(onReloadClick = onReloadClick)
            is PaginationStatus.NextBatchLoading -> toResults(footer = PolymarketFeedFooterUM.Loading)
            is PaginationStatus.Paginating -> toResults(
                footer = if (status.lastResult is BatchFetchResult.Error) {
                    PolymarketFeedFooterUM.Error(onRetryClick = onNextPageRetryClick)
                } else {
                    PolymarketFeedFooterUM.None
                },
            )
            is PaginationStatus.EndOfPagination -> toResults(footer = PolymarketFeedFooterUM.None)
        }
    }

    private fun toResults(footer: PolymarketFeedFooterUM): PolymarketSearchUM.ContentUM {
        // A result that slides between two page requests arrives on both pages; the copies would collide
        // as LazyColumn keys and crash the list — the same drift the feed deduplicates.
        val events = batchListState.data
            .flatMap { batch -> batch.data }
            .distinctBy(PolymarketEvent::id)
        if (events.isEmpty()) return PolymarketSearchUM.ContentUM.NothingFound

        return PolymarketSearchUM.ContentUM.Results(
            events = eventUMConverter.convertList(events).toImmutableList(),
            footer = footer,
        )
    }
}