package com.tangem.features.collectibles.impl.machinedetails.model.transformer

import com.tangem.core.ui.extensions.stringReference
import com.tangem.domain.collectibles.CollectibleGachaMachineNftsBatchListState
import com.tangem.domain.collectibles.models.CollectibleGachaMachineNft
import com.tangem.features.collectibles.impl.machinedetails.model.converter.CollectiblesPrizeConverter
import com.tangem.features.collectibles.impl.machinedetails.ui.state.CollectiblesMachineDetailsUM
import com.tangem.pagination.PaginationStatus
import com.tangem.utils.transformer.Transformer
import kotlinx.collections.immutable.toImmutableList

/**
 * Applies the pagination state of the prize pool; every other part of the screen stays as it is, and a state
 * that is not yet [CollectiblesMachineDetailsUM.Content] is left untouched — the machine has to arrive first.
 *
 * Pages accumulate: every loaded page stays on screen and only the footer loader says another is on its way.
 *
 * @property batchListState pagination state to render
 * @property onReloadClick retries the pool from its first page
 */
internal class CollectiblesPrizePoolTransformer(
    private val batchListState: CollectibleGachaMachineNftsBatchListState,
    private val onReloadClick: () -> Unit,
) : Transformer<CollectiblesMachineDetailsUM> {

    override fun transform(prevState: CollectiblesMachineDetailsUM): CollectiblesMachineDetailsUM {
        if (prevState !is CollectiblesMachineDetailsUM.Content) return prevState

        return prevState.copy(prizePool = transformPrizePool())
    }

    private fun transformPrizePool(): CollectiblesMachineDetailsUM.PrizePoolUM {
        return when (batchListState.status) {
            is PaginationStatus.None,
            is PaginationStatus.InitialLoading,
            -> CollectiblesMachineDetailsUM.PrizePoolUM.Loading
            is PaginationStatus.InitialLoadingError -> CollectiblesMachineDetailsUM.PrizePoolUM.Error(
                message = stringReference(ERROR_MESSAGE),
                onReloadClick = onReloadClick,
            )
            is PaginationStatus.NextBatchLoading -> toContent(isLoadingNextPage = true)
            is PaginationStatus.Paginating,
            is PaginationStatus.EndOfPagination,
            -> toContent(isLoadingNextPage = false)
        }
    }

    private fun toContent(isLoadingNextPage: Boolean): CollectiblesMachineDetailsUM.PrizePoolUM {
        // The pool shifts as packs are opened, so a collectible that moved between two page requests arrives
        // twice; the copies would collide as lazy-list keys and crash the list.
        val nfts = batchListState.data
            .flatMap { batch -> batch.data }
            .distinctBy(CollectibleGachaMachineNft::id)

        if (nfts.isEmpty()) {
            return CollectiblesMachineDetailsUM.PrizePoolUM.Empty(message = stringReference(EMPTY_MESSAGE))
        }

        val rows = nfts
            .map(CollectiblesPrizeConverter::convert)
            .chunked(PRIZES_PER_ROW) { chunk ->
                CollectiblesMachineDetailsUM.PrizeRowUM(id = chunk.first().id, prizes = chunk.toImmutableList())
            }
            .toImmutableList()

        return CollectiblesMachineDetailsUM.PrizePoolUM.Content(rows = rows, isLoadingNextPage = isLoadingNextPage)
    }

    private companion object {

        const val PRIZES_PER_ROW = 2
        const val ERROR_MESSAGE = "Failed to load the prize pool.\nTap to reload" // TODO design not ready
        const val EMPTY_MESSAGE = "This pack has no cards in its prize pool yet" // TODO design not ready
    }
}