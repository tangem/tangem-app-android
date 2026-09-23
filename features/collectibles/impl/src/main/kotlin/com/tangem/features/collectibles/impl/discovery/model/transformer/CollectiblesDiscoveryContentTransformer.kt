package com.tangem.features.collectibles.impl.discovery.model.transformer

import com.tangem.core.ui.extensions.TextReference
import com.tangem.domain.collectibles.CollectibleGachaMachinesBatchListState
import com.tangem.domain.collectibles.models.CollectibleGachaMachine
import com.tangem.features.collectibles.impl.R
import com.tangem.features.collectibles.impl.discovery.model.converter.CollectiblesDiscoveryPackConverter
import com.tangem.features.collectibles.impl.discovery.ui.state.CollectiblesDiscoveryUM
import com.tangem.pagination.PaginationStatus
import com.tangem.utils.transformer.Transformer
import kotlinx.collections.immutable.toImmutableList

/**
 * Applies the pagination state of the catalogue to the content area; the tabs and the search of the previous
 * state stay as they are.
 *
 * Pages accumulate: every loaded batch stays on the screen, so a status carrying pages renders them all and only
 * the footer loader tells whether one more is on its way. A first page that never arrived is the one failure the
 * user sees — the design shows the same reload prompt for a failed request and for an empty category alike.
 *
 * @property batchListState pagination state to render
 * @property packConverter converts the machines of the loaded pages into pack cards
 * @property onReloadClick retries the catalogue from its first page
 */
internal class CollectiblesDiscoveryContentTransformer(
    private val batchListState: CollectibleGachaMachinesBatchListState,
    private val packConverter: CollectiblesDiscoveryPackConverter,
    private val onReloadClick: () -> Unit,
) : Transformer<CollectiblesDiscoveryUM> {

    override fun transform(prevState: CollectiblesDiscoveryUM): CollectiblesDiscoveryUM {
        return prevState.copy(content = transformContent(prev = prevState.content))
    }

    private fun transformContent(prev: CollectiblesDiscoveryUM.ContentUM): CollectiblesDiscoveryUM.ContentUM {
        return when (batchListState.status) {
            is PaginationStatus.None,
            is PaginationStatus.InitialLoading,
            -> if (prev is CollectiblesDiscoveryUM.ContentUM.Content) {
                prev.copy(isReloading = true)
            } else {
                CollectiblesDiscoveryUM.ContentUM.Loading
            }
            is PaginationStatus.InitialLoadingError -> CollectiblesDiscoveryUM.ContentUM.Error(
                message = TextReference.Res(R.string.collectibles_main_search_load_error),
                onReloadClick = onReloadClick,
            )
            is PaginationStatus.NextBatchLoading -> toContent(isLoadingNextPage = true)
            is PaginationStatus.Paginating,
            is PaginationStatus.EndOfPagination,
            -> toContent(isLoadingNextPage = false)
        }
    }

    private fun toContent(isLoadingNextPage: Boolean): CollectiblesDiscoveryUM.ContentUM {
        // The backend pages with a keyset cursor over a shifting order, so a machine that moved between two
        // page requests arrives twice; the copies would collide as lazy-list keys and crash the list.
        val machines = batchListState.data
            .flatMap { batch -> batch.data.collectibleGachaMachines }
            .distinctBy(CollectibleGachaMachine::id)

        if (machines.isEmpty()) {
            return CollectiblesDiscoveryUM.ContentUM.Empty(TextReference.Res(R.string.collectibles_main_search_empty))
        }

        val rows = machines
            .map(packConverter::convert)
            .chunked(PACKS_PER_ROW) { chunk ->
                CollectiblesDiscoveryUM.PackRowUM(id = chunk.first().id, packs = chunk.toImmutableList())
            }
            .toImmutableList()

        return CollectiblesDiscoveryUM.ContentUM.Content(
            rows = rows,
            isLoadingNextPage = isLoadingNextPage,
            isReloading = false,
        )
    }

    private companion object {

        const val PACKS_PER_ROW = 2
    }
}