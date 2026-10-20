package com.tangem.domain.polymarket.session

import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.polymarket.PolymarketPositionsRepository
import com.tangem.domain.polymarket.flow.DefaultPredictionPositionsUpdater
import com.tangem.domain.polymarket.model.PolymarketAddresses
import com.tangem.domain.polymarket.model.PolymarketPosition
import com.tangem.domain.polymarket.model.PolymarketPositionId
import com.tangem.domain.polymarket.model.PolymarketPositionResult
import com.tangem.domain.polymarket.model.PolymarketResolutionStatus
import com.tangem.domain.polymarket.model.PolymarketPositionsBatchingContext
import com.tangem.domain.polymarket.model.PolymarketPositionsListConfig
import com.tangem.domain.polymarket.model.PolymarketPositionsUpdateRequest
import com.tangem.domain.polymarket.usecase.CreatePolymarketPositionsSessionUseCase
import com.tangem.domain.polymarket.usecase.DerivePolymarketAddressesUseCase
import com.tangem.pagination.Batch
import com.tangem.pagination.BatchFetchResult
import com.tangem.pagination.BatchUpdateFetcher
import com.tangem.pagination.BatchUpdateResult
import com.tangem.pagination.BatchListSource
import com.tangem.pagination.fetcher.BatchFetcher
import com.tangem.pagination.toBatchFlow
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.TestDispatcher

@OptIn(ExperimentalCoroutinesApi::class)
internal abstract class PolymarketPositionsSessionTestBase {

    protected val repository: PolymarketPositionsRepository = mockk()
    protected val deriveAddresses: DerivePolymarketAddressesUseCase = mockk()
    protected val wallet = UserWalletId("011")
    protected val addresses: PolymarketAddresses = mockk()
    protected val positionsUpdater = DefaultPredictionPositionsUpdater()
    /** Every page read, as the list identity and the zero-based page index. */
    protected val requests = mutableListOf<Pair<PolymarketPositionsListConfig, Int>>()
    protected var fetch: suspend (index: Int) -> BatchFetchResult<List<PolymarketPosition>> = { page() }

    protected fun TestScope.createFactory(
        dispatcher: TestDispatcher = StandardTestDispatcher(testScheduler),
        fetchDispatcher: TestDispatcher = dispatcher,
    ): CreatePolymarketPositionsSessionUseCase {
        coEvery { deriveAddresses.stored(wallet) } returns addresses
        every { repository.getPositionsBatchFlow(any(), any()) } answers {
            val context = firstArg<PolymarketPositionsBatchingContext>()
            val size = secondArg<Int>()
            BatchListSource(
                fetchDispatcher = fetchDispatcher,
                context = context,
                generateNewKey = { keys -> keys.size },
                batchFetcher = PagesFetcher(),
                updateFetcher = refreshByWalking(),
            ).toBatchFlow()
        }
        return CreatePolymarketPositionsSessionUseCase(
            repository,
            deriveAddresses,
            positionsUpdater,
            TestingCoroutineDispatcherProvider(dispatcher, dispatcher, dispatcher, dispatcher, dispatcher),
        )
    }

    protected fun position(
        assetId: String = "asset",
        conditionId: String = "condition",
        result: PolymarketPositionResult? = null,
        resolution: PolymarketResolutionStatus = PolymarketResolutionStatus.OPEN,
    ): PolymarketPosition.Open = mockk {
        every { id } returns PolymarketPositionId(conditionId, assetId)
        every { this@mockk.result } returns result
        every { resolutionStatus } returns resolution
    }

    protected fun page(
        positions: List<PolymarketPosition> = listOf(position()),
        last: Boolean = true,
    ): BatchFetchResult<List<PolymarketPosition>> = BatchFetchResult.Success(positions, positions.isEmpty(), last)

    /** Mirrors the production batch fetcher: pages are read in order, a failed page is read again. */
    private inner class PagesFetcher : BatchFetcher<PolymarketPositionsListConfig, List<PolymarketPosition>> {

        private var config: PolymarketPositionsListConfig? = null
        private var index = 0

        override suspend fun fetchFirst(
            requestParams: PolymarketPositionsListConfig,
        ): BatchFetchResult<List<PolymarketPosition>> {
            config = requestParams
            index = 0
            return read()
        }

        override suspend fun fetchNext(
            overrideRequestParams: PolymarketPositionsListConfig?,
            lastResult: BatchFetchResult<List<PolymarketPosition>>,
        ): BatchFetchResult<List<PolymarketPosition>> =
            if (overrideRequestParams != null) fetchFirst(overrideRequestParams) else read()

        private suspend fun read(): BatchFetchResult<List<PolymarketPosition>> {
            requests += requireNotNull(config) to index
            return fetch(index).also { if (it is BatchFetchResult.Success) index++ }
        }
    }

    /**
     * Mirrors the production update fetcher: page `k` is read again through the pages before it, so a page past
     * the end of a shrunk list is emptied without a read.
     */
    private fun refreshByWalking() =
        object : BatchUpdateFetcher<Int, List<PolymarketPosition>, PolymarketPositionsUpdateRequest> {
            override suspend fun BatchUpdateFetcher.UpdateContext<Int, List<PolymarketPosition>>.fetchUpdateAsync(
                toUpdate: List<Batch<Int, List<PolymarketPosition>>>,
                updateRequest: PolymarketPositionsUpdateRequest,
            ) {
                var hasNext = true
                toUpdate.sortedBy { it.key }.forEach { batch ->
                    val positions = if (hasNext) {
                        requests += updateRequest.config to batch.key
                        when (val result = fetch(batch.key)) {
                            is BatchFetchResult.Success -> result.data.also { hasNext = !result.isLast }
                            is BatchFetchResult.Error -> throw result.throwable
                        }
                    } else {
                        emptyList()
                    }
                    update {
                        BatchUpdateResult.Success(
                            mapNotNull { if (it.key == batch.key) it.copy(data = positions) else null },
                        )
                    }
                }
            }
        }
}