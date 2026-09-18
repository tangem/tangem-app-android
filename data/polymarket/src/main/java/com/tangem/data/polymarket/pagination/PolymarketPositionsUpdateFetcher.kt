package com.tangem.data.polymarket.pagination

import arrow.core.Either
import arrow.core.getOrElse
import com.tangem.domain.polymarket.model.PolymarketPosition
import com.tangem.domain.polymarket.model.PolymarketPositionsError
import com.tangem.domain.polymarket.model.PolymarketPositionsListConfig
import com.tangem.domain.polymarket.model.PolymarketPositionsPage
import com.tangem.domain.polymarket.model.PolymarketPositionsUpdateRequest
import com.tangem.pagination.Batch
import com.tangem.pagination.BatchUpdateFetcher
import com.tangem.pagination.BatchUpdateResult

/**
 * Requests a loaded page again and replaces its rows: a holding sold or claimed since must leave the list, and a
 * row that slid over from the next page is deduplicated by the session.
 *
 * Pages are keyed in load order and every page is requested with the same limit, so page `k` lives at offset
 * `k * batchSize`.
 *
 * A failed request is left to propagate — the session keeps the rows it has.
 */
internal class PolymarketPositionsUpdateFetcher(
    private val batchSize: Int,
    private val fetchPage: suspend (config: PolymarketPositionsListConfig, limit: Int, offset: Int) ->
    Either<PolymarketPositionsError, PolymarketPositionsPage<PolymarketPosition>>,
) : BatchUpdateFetcher<Int, List<PolymarketPosition>, PolymarketPositionsUpdateRequest> {

    override suspend fun BatchUpdateFetcher.UpdateContext<Int, List<PolymarketPosition>>.fetchUpdateAsync(
        toUpdate: List<Batch<Int, List<PolymarketPosition>>>,
        updateRequest: PolymarketPositionsUpdateRequest,
    ) {
        toUpdate.forEach { batch ->
            val page = fetchPage(updateRequest.config, batchSize, batch.key * batchSize)
                .getOrElse { throw PolymarketPositionsReadException(it) }
            update {
                BatchUpdateResult.Success(
                    data = mapNotNull { current ->
                        if (current.key == batch.key) current.copy(data = page.positions) else null
                    },
                )
            }
        }
    }
}