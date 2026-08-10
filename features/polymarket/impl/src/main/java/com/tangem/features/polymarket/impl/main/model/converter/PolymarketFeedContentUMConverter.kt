package com.tangem.features.polymarket.impl.main.model.converter

import com.tangem.domain.polymarket.model.PolymarketEventsBatchListState
import com.tangem.features.polymarket.impl.main.ui.state.PolymarketMainUM
import com.tangem.pagination.PaginationStatus
import com.tangem.utils.converter.Converter
import kotlinx.collections.immutable.toImmutableList

/**
 * Converts the pagination state of the Discovery feed into the events area of the screen.
 *
 * Pages accumulate: every loaded batch stays on the screen, so a status carrying pages renders them all and only
 * the footer loader tells whether one more is on its way. A first page that never arrived is the one failure the
 * user sees — the design shows the same reload prompt for a failed request and for an empty category alike.
 *
 * @property eventUMConverter converts the events of the loaded pages into cards
 * @property onReloadClick retries the feed from its first page
 */
internal class PolymarketFeedContentUMConverter(
    private val eventUMConverter: PolymarketEventUMConverter,
    private val onReloadClick: () -> Unit,
) : Converter<PolymarketEventsBatchListState, PolymarketMainUM.ContentUM> {

    override fun convert(value: PolymarketEventsBatchListState): PolymarketMainUM.ContentUM {
        return when (value.status) {
            is PaginationStatus.None,
            is PaginationStatus.InitialLoading,
            -> PolymarketMainUM.ContentUM.Loading
            is PaginationStatus.InitialLoadingError -> PolymarketMainUM.ContentUM.Error(onReloadClick = onReloadClick)
            is PaginationStatus.NextBatchLoading -> value.toContent(isLoadingNextPage = true)
            is PaginationStatus.Paginating,
            is PaginationStatus.EndOfPagination,
            -> value.toContent(isLoadingNextPage = false)
        }
    }

    private fun PolymarketEventsBatchListState.toContent(
        isLoadingNextPage: Boolean,
    ): PolymarketMainUM.ContentUM.Content {
        val events = data.flatMap { batch -> batch.data }
        return PolymarketMainUM.ContentUM.Content(
            events = eventUMConverter.convertList(events).toImmutableList(),
            isLoadingNextPage = isLoadingNextPage,
        )
    }
}