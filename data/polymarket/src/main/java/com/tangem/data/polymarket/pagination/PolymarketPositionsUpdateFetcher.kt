package com.tangem.data.polymarket.pagination

import arrow.core.Either
import arrow.core.getOrElse
import com.tangem.domain.polymarket.error.PolymarketPositionsReadException
import com.tangem.domain.polymarket.model.PolymarketPosition
import com.tangem.domain.polymarket.model.PolymarketPositionsError
import com.tangem.domain.polymarket.model.PolymarketPositionsListConfig
import com.tangem.domain.polymarket.model.PolymarketPositionsPage
import com.tangem.domain.polymarket.model.PolymarketPositionsPageRequest
import com.tangem.domain.polymarket.model.PolymarketPositionsUpdateRequest
import com.tangem.pagination.Batch
import com.tangem.pagination.BatchUpdateFetcher
import com.tangem.pagination.BatchUpdateResult

/**
 * Reads the loaded pages again from the first one and replaces their rows: a holding sold or claimed since must
 * leave the list, and a row that slid over from the next page is deduplicated by the session.
 *
 * A cursor only continues the page it came with, so the pages are walked in key order, each through the cursor
 * the fresh page before it returned. A list that has shrunk leaves the pages past its end empty.
 *
 * A failed request is left to propagate — the session keeps the rows it has.
 */
internal class PolymarketPositionsUpdateFetcher(
    private val batchSize: Int,
    private val fetchPage: suspend (config: PolymarketPositionsListConfig, request: PolymarketPositionsPageRequest) ->
    Either<PolymarketPositionsError, PolymarketPositionsPage<PolymarketPosition>>,
) : BatchUpdateFetcher<Int, List<PolymarketPosition>, PolymarketPositionsUpdateRequest> {

    override suspend fun BatchUpdateFetcher.UpdateContext<Int, List<PolymarketPosition>>.fetchUpdateAsync(
        toUpdate: List<Batch<Int, List<PolymarketPosition>>>,
        updateRequest: PolymarketPositionsUpdateRequest,
    ) {
        var request: PolymarketPositionsPageRequest? = PolymarketPositionsPageRequest.First(batchSize)
        toUpdate.sortedBy { it.key }.forEach { batch ->
            val page = request?.let { pageRequest ->
                fetchPage(updateRequest.config, pageRequest).getOrElse { throw PolymarketPositionsReadException(it) }
            }
            request = page?.nextCursor?.let(PolymarketPositionsPageRequest::Next)
            update {
                BatchUpdateResult.Success(
                    data = mapNotNull { current ->
                        if (current.key == batch.key) current.copy(data = page?.positions.orEmpty()) else null
                    },
                )
            }
        }
    }
}