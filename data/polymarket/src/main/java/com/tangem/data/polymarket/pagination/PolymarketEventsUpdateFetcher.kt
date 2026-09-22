package com.tangem.data.polymarket.pagination

import com.tangem.domain.polymarket.model.PolymarketEvent
import com.tangem.domain.polymarket.model.PolymarketEventsBatch
import com.tangem.domain.polymarket.model.PolymarketEventsListConfig
import com.tangem.domain.polymarket.model.PolymarketEventsPage
import com.tangem.domain.polymarket.model.PolymarketEventsUpdateRequest
import com.tangem.pagination.Batch
import com.tangem.pagination.BatchUpdateFetcher
import com.tangem.pagination.BatchUpdateResult

/**
 * Requests a loaded page again with the cursor it was loaded by and merges the response into it **by event id**.
 *
 * The merge never changes what the page holds or in which order: the feed's live sort would otherwise reshuffle
 * cards under the user's finger, and a keyset cursor over a shifting order would duplicate them.
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
                .associateBy(PolymarketEvent::id)

            update {
                val merged = mapNotNull { current ->
                    if (current.key != batch.key) return@mapNotNull null

                    current.copy(
                        data = current.data.copy(
                            events = current.data.events.map { event -> refreshed[event.id] ?: event },
                        ),
                    )
                }

                BatchUpdateResult.Success(merged)
            }
        }
    }
}