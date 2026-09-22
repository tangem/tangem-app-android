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
import com.tangem.pagination.fetcher.LimitOffsetBatchFetcher
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
    protected val requests = mutableListOf<Pair<PolymarketPositionsListConfig, Int>>()
    protected var fetch: suspend (Int) -> BatchFetchResult<List<PolymarketPosition>> = { page() }

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
                batchFetcher = LimitOffsetBatchFetcher<PolymarketPositionsListConfig, List<PolymarketPosition>>(
                    prefetchDistance = size,
                    batchSize = size,
                    subFetcher = { request, _, _ ->
                        requests += request.params to request.offset
                        fetch(request.offset)
                    },
                ),
                updateFetcher = refreshByOffset(size),
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

    /** Mirrors the production update fetcher: page `k` is read again at offset `k * size` and replaced. */
    private fun refreshByOffset(size: Int) =
        object : BatchUpdateFetcher<Int, List<PolymarketPosition>, PolymarketPositionsUpdateRequest> {
            override suspend fun BatchUpdateFetcher.UpdateContext<Int, List<PolymarketPosition>>.fetchUpdateAsync(
                toUpdate: List<Batch<Int, List<PolymarketPosition>>>,
                updateRequest: PolymarketPositionsUpdateRequest,
            ) {
                toUpdate.forEach { batch ->
                    requests += updateRequest.config to batch.key * size
                    val positions = when (val result = fetch(batch.key * size)) {
                        is BatchFetchResult.Success -> result.data
                        is BatchFetchResult.Error -> throw result.throwable
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