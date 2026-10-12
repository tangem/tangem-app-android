package com.tangem.data.collectibles.pagination

import com.tangem.domain.collectibles.models.CollectibleGachaMachineNft
import com.tangem.domain.collectibles.models.CollectibleGachaMachineNftsListConfig
import com.tangem.domain.collectibles.models.CollectibleGachaMachineNftsPage
import com.tangem.pagination.BatchFetchResult
import com.tangem.pagination.exception.EndOfPaginationException
import com.tangem.pagination.fetcher.BatchFetcher
import com.tangem.utils.coroutines.runSuspendCatching

/**
 * Pages a machine's prize pool by 1-based page number.
 *
 * Unlike the machines catalogue next door, this endpoint has no cursor — it reports `hasMore` and echoes the
 * page it served.
 *
 * @param fetchPage loads a single page: which machine, the page to read and the page size.
 */
internal class CollectibleGachaMachineNftsBatchFetcher(
    private val batchSize: Int,
    private val fetchPage: suspend (
        config: CollectibleGachaMachineNftsListConfig,
        page: Int,
        limit: Int,
    ) -> CollectibleGachaMachineNftsPage,
) : BatchFetcher<CollectibleGachaMachineNftsListConfig, List<CollectibleGachaMachineNft>> {

    private var lastConfig: CollectibleGachaMachineNftsListConfig? = null
    private var nextPage: Int = FIRST_PAGE

    override suspend fun fetchFirst(
        requestParams: CollectibleGachaMachineNftsListConfig,
    ): BatchFetchResult<List<CollectibleGachaMachineNft>> {
        lastConfig = requestParams
        nextPage = FIRST_PAGE
        return runFetch(config = requestParams, page = FIRST_PAGE)
    }

    override suspend fun fetchNext(
        overrideRequestParams: CollectibleGachaMachineNftsListConfig?,
        lastResult: BatchFetchResult<List<CollectibleGachaMachineNft>>,
    ): BatchFetchResult<List<CollectibleGachaMachineNft>> {
        val config = overrideRequestParams
            ?: lastConfig
            ?: error("fetchFirst() must be called before fetchNext()")

        if (lastResult is BatchFetchResult.Success && lastResult.last && overrideRequestParams == null) {
            return BatchFetchResult.Error(EndOfPaginationException())
        }

        lastConfig = config
        return runFetch(config = config, page = nextPage)
    }

    private suspend fun runFetch(
        config: CollectibleGachaMachineNftsListConfig,
        page: Int,
    ): BatchFetchResult<List<CollectibleGachaMachineNft>> = runSuspendCatching { fetchPage(config, page, batchSize) }
        .fold(
            onSuccess = { fetched ->
                // Advanced from the requested page, not the echoed one: a backend that repeats a page number
                // would otherwise pin the pagination and refetch the same page forever.
                nextPage = page + 1

                BatchFetchResult.Success(
                    data = fetched.nfts,
                    empty = fetched.nfts.isEmpty(),
                    last = !fetched.hasMore,
                )
            },
            // A failed page keeps [nextPage] where it is, so a retry re-requests the page that failed.
            onFailure = { throwable -> BatchFetchResult.Error(throwable) },
        )

    private companion object {
        const val FIRST_PAGE = 1
    }
}