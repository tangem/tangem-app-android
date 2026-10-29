package com.tangem.data.polymarket.pagination

import com.tangem.domain.polymarket.model.PolymarketEventsBatch
import com.tangem.domain.polymarket.model.PolymarketEventsListConfig
import com.tangem.domain.polymarket.model.PolymarketEventsPage
import com.tangem.domain.polymarket.model.PolymarketEventsUpdateRequest
import com.tangem.pagination.Batch
import com.tangem.pagination.BatchUpdateFetcher
import com.tangem.pagination.BatchUpdateResult

/**
 * Refreshes a loaded page with its original request cursor, preserving the server's current order and membership.
 *
 * A failed request is left to propagate — the poller counts it and the next tick asks again.
 */
internal class PolymarketEventsUpdateFetcher(
    private val batchSize: Int,
    private val fetchPage: suspend (config: PolymarketEventsListConfig, cursor: String?, limit: Int) ->
    PolymarketEventsPage,
) : BatchUpdateFetcher<Int, PolymarketEventsBatch, PolymarketEventsUpdateRequest> {

    override suspend fun BatchUpdateFetcher.UpdateContext<Int, PolymarketEventsBatch>.fetchUpdateAsync(
        toUpdate: List<Batch<Int, PolymarketEventsBatch>>,
        updateRequest: PolymarketEventsUpdateRequest,
    ) {
        toUpdate.forEach { batch ->
            val refreshed = fetchPage(updateRequest.config, batch.data.requestCursor, batchSize)
                .events

            update {
                val merged = mapNotNull { current ->
                    if (current.key != batch.key) return@mapNotNull null

                    current.copy(
                        data = current.data.copy(events = refreshed),
                    )
                }

                BatchUpdateResult.Success(merged)
            }
        }
    }
}