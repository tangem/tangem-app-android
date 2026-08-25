package com.tangem.features.polymarket.impl.main.model.transformer

import com.tangem.domain.polymarket.model.PolymarketEvent
import com.tangem.domain.polymarket.model.PolymarketEventsBatchListState
import com.tangem.features.polymarket.impl.main.model.converter.PolymarketEventUMConverter
import com.tangem.features.polymarket.impl.main.ui.state.PolymarketMainUM
import com.tangem.pagination.PaginationStatus
import com.tangem.utils.transformer.Transformer
import kotlinx.collections.immutable.toImmutableList

/**
 * Applies the pagination state of the Discovery feed to the events area of the screen; the tabs and the access
 * mode of the previous state stay as they are.
 *
 * Pages accumulate: every loaded batch stays on the screen, so a status carrying pages renders them all and only
 * the footer loader tells whether one more is on its way. A first page that never arrived is the one failure the
 * user sees — the design shows the same reload prompt for a failed request and for an empty category alike.
 *
 * @property batchListState pagination state to render
 * @property eventUMConverter converts the events of the loaded pages into cards
 * @property onReloadClick retries the feed from its first page
 */
internal class PolymarketFeedContentTransformer(
    private val batchListState: PolymarketEventsBatchListState,
    private val eventUMConverter: PolymarketEventUMConverter,
    private val onReloadClick: () -> Unit,
) : Transformer<PolymarketMainUM> {

    override fun transform(prevState: PolymarketMainUM): PolymarketMainUM {
        return prevState.copy(content = transformContent())
    }

    private fun transformContent(): PolymarketMainUM.ContentUM {
        return when (batchListState.status) {
            is PaginationStatus.None,
            is PaginationStatus.InitialLoading,
            -> PolymarketMainUM.ContentUM.Loading
            is PaginationStatus.InitialLoadingError -> PolymarketMainUM.ContentUM.Error(onReloadClick = onReloadClick)
            is PaginationStatus.NextBatchLoading -> toContent(isLoadingNextPage = true)
            is PaginationStatus.Paginating,
            is PaginationStatus.EndOfPagination,
            -> toContent(isLoadingNextPage = false)
        }
    }

    private fun toContent(isLoadingNextPage: Boolean): PolymarketMainUM.ContentUM.Content {
        // The BFF pages with a keyset cursor over a shifting order, so an event that moved between two
        // page requests arrives twice; the copies would collide as LazyColumn keys and crash the feed.
        val events = batchListState.data
            .flatMap { batch -> batch.data.events }
            .distinctBy(PolymarketEvent::id)
        return PolymarketMainUM.ContentUM.Content(
            events = eventUMConverter.convertList(events).toImmutableList(),
            isLoadingNextPage = isLoadingNextPage,
        )
    }
}