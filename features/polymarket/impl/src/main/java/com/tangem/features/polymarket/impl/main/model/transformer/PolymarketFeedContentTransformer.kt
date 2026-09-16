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

/** Renders the pagination state of the Discovery feed into the events area of the screen. */
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
        // The keyset cursor can serve an event twice; duplicate keys would crash the list.
        val events = batchListState.data
            .flatMap { batch -> batch.data.events }
            .distinctBy(PolymarketEvent::id)
        return PolymarketMainUM.ContentUM.Content(
            events = eventUMConverter.convertList(events).toImmutableList(),
            footer = footer,
        )
    }
}