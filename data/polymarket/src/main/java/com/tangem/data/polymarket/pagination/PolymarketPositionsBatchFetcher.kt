package com.tangem.data.polymarket.pagination

import arrow.core.Either
import com.tangem.domain.polymarket.model.PolymarketPosition
import com.tangem.domain.polymarket.model.PolymarketPositionsError
import com.tangem.domain.polymarket.model.PolymarketPositionsListConfig
import com.tangem.domain.polymarket.model.PolymarketPositionsPage
import com.tangem.pagination.BatchFetchResult
import com.tangem.pagination.fetcher.BatchFetcher
import com.tangem.pagination.fetcher.LimitOffsetBatchFetcher

internal class PolymarketPositionsBatchFetcher(
    batchSize: Int,
    fetchPage: suspend (config: PolymarketPositionsListConfig, limit: Int, offset: Int) ->
    Either<PolymarketPositionsError, PolymarketPositionsPage<PolymarketPosition>>,
) : BatchFetcher<PolymarketPositionsListConfig, List<PolymarketPosition>> by LimitOffsetBatchFetcher(
    prefetchDistance = batchSize,
    batchSize = batchSize,
    subFetcher = { request, _, _ ->
        fetchPage(request.params, request.limit, request.offset).fold(
            ifLeft = { BatchFetchResult.Error(PolymarketPositionsReadException(it)) },
            ifRight = { page ->
                BatchFetchResult.Success(
                    data = page.positions,
                    empty = page.positions.isEmpty(),
                    last = !page.hasNext || request.offset + request.limit > MAX_OFFSET,
                )
            },
        )
    },
) {

    init {
        require(batchSize in 1..MAX_PAGE_SIZE)
    }

    private companion object {
        const val MAX_PAGE_SIZE = 30
        const val MAX_OFFSET = 10_000
    }
}