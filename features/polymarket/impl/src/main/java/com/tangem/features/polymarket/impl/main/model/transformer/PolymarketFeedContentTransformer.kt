package com.tangem.features.polymarket.impl.main.model.transformer

import com.tangem.domain.polymarket.model.PolymarketEvent
import com.tangem.domain.polymarket.model.PolymarketEventsBatchListState
import com.tangem.features.polymarket.impl.main.model.converter.PolymarketEventUMConverter
import com.tangem.features.polymarket.impl.main.ui.state.PolymarketFeedFooterUM
import com.tangem.features.polymarket.impl.main.ui.state.PolymarketMainUM
import com.tangem.pagination.BatchFetchResult
import com.tangem.pagination.PaginationStatus
import com.tangem.utils.transformer.Transformer
import kotlinx.collections.immutable.toImmutableList

/**
 * Applies the pagination state of the Discovery feed to the events area of the screen; the tabs and the access
 * mode of the previous state stay as they are.
 *
 * Pages accumulate: every loaded batch stays on the screen, so a status carrying pages renders them all and only
 * the footer tells about the next one — on its way, or failed. A first page that never arrived takes the whole
 * events area instead — the design shows the same reload prompt for a failed request and for an empty category
 * alike.
 *
 * @property batchListState pagination state to render
 * @property eventUMConverter converts the events of the loaded pages into cards
 * @property onReloadClick retries the feed from its first page
 * @property onNextPageRetryClick asks for the next page again after it failed
 */
internal class PolymarketFeedContentTransformer(
    private val batchListState: PolymarketEventsBatchListState,
    private val eventUMConverter: PolymarketEventUMConverter,
    private val onReloadClick: () -> Unit,
    private val onNextPageRetryClick: () -> Unit,
) : Transformer<PolymarketMainUM> {

    override fun transform(prevState: PolymarketMainUM): PolymarketMainUM {
        return prevState.copy(content = transformContent())
    }

    private fun transformContent(): PolymarketMainUM.ContentUM {
        return when (val status = batchListState.status) {
            is PaginationStatus.None,
            is PaginationStatus.InitialLoading,
            -> PolymarketMainUM.ContentUM.Loading
            is PaginationStatus.InitialLoadingError -> PolymarketMainUM.ContentUM.Error(onReloadClick = onReloadClick)
            is PaginationStatus.NextBatchLoading -> toContent(footer = PolymarketFeedFooterUM.Loading)
            is PaginationStatus.Paginating -> toContent(
                footer = if (status.lastResult is BatchFetchResult.Error) {
                    PolymarketFeedFooterUM.Error(onRetryClick = onNextPageRetryClick)
                } else {
                    PolymarketFeedFooterUM.None
                },
            )
            is PaginationStatus.EndOfPagination -> toContent(footer = PolymarketFeedFooterUM.None)
        }
    }

    private fun toContent(footer: PolymarketFeedFooterUM): PolymarketMainUM.ContentUM.Content {
        // The BFF pages with a keyset cursor over a shifting order, so an event that moved between two
        // page requests arrives twice; the copies would collide as LazyColumn keys and crash the feed.
        val events = batchListState.data
            .flatMap { batch -> batch.data.events }
            .distinctBy(PolymarketEvent::id)
        return PolymarketMainUM.ContentUM.Content(
            events = eventUMConverter.convertList(events).toImmutableList(),
            footer = footer,
        )
    }
}