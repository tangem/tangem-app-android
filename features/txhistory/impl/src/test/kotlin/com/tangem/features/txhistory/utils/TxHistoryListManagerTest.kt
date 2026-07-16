package com.tangem.features.txhistory.utils

import com.google.common.truth.Truth.assertThat
import com.tangem.core.ui.DesignFeatureToggles
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.network.TxInfo
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.txhistory.model.ExpressTx
import com.tangem.domain.txhistory.model.TxHistoryListBatchFlow
import com.tangem.domain.txhistory.model.TxHistoryListBatchingContext
import com.tangem.domain.txhistory.model.TxHistoryListConfig
import com.tangem.domain.txhistory.models.Page
import com.tangem.domain.txhistory.models.PaginationWrapper
import com.tangem.domain.txhistory.repository.TxHistoryRepositoryV2
import com.tangem.features.txhistory.converter.TxHistoryItemToTransactionStateConverter
import com.tangem.pagination.BatchFetchResult
import com.tangem.pagination.BatchListSource
import com.tangem.pagination.PaginationStatus
import com.tangem.pagination.fetcher.BatchFetcher
import com.tangem.pagination.toBatchFlow
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

/**
 * Verifies the auto-load behavior for Solana-style histories, where a fetched page is paginated over RAW
 * transactions and then filtered down to a single token, so a page can yield few or zero displayable items.
 * The manager must keep requesting the next page until the list is long enough to be scrolled or pagination
 * ends — instead of stopping on the first page that adds no items.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class TxHistoryListManagerTest {

    private val userWalletId = UserWalletId(stringValue = "01")
    private val currency = mockk<CryptoCurrency>(relaxed = true)

    @Test
    fun `GIVEN pages that are empty for the token WHEN loading THEN auto-loads through them until the end`() =
        runTest {
            // page 0: 2 items, then two empty-for-token pages, then 3 items on the last page → 5 items total.
            val fetcher = ScriptedFetcher { call ->
                when (call) {
                    0 -> page(itemCount = 2, isLast = false)
                    1 -> page(itemCount = 0, isLast = false)
                    2 -> page(itemCount = 0, isLast = false)
                    else -> page(itemCount = 3, isLast = true)
                }
            }
            val repo = fakeRepository(fetcher)
            val manager = createManager(repo)

            withLoadedManager(manager) {
                // first fetch + 3 auto-loaded next pages = 4
                assertThat(fetcher.fetchCount).isEqualTo(4)
                assertThat(repo.loadedItemsCount()).isEqualTo(5)
                assertThat(repo.status()).isInstanceOf(PaginationStatus.EndOfPagination::class.java)
            }
        }

    @Test
    fun `GIVEN many small non-final pages WHEN loading THEN stops once the list is long enough to scroll`() =
        runTest {
            // every page returns 7 items and is never the last page.
            val fetcher = ScriptedFetcher { page(itemCount = 7, isLast = false) }
            val repo = fakeRepository(fetcher)
            val manager = createManager(repo)

            withLoadedManager(manager) {
                // 7 -> 14 -> 21: stops after crossing AUTO_LOAD_MORE_TARGET_COUNT (20), does not keep loading.
                assertThat(fetcher.fetchCount).isEqualTo(3)
                assertThat(repo.loadedItemsCount()).isEqualTo(21)
                assertThat(repo.status()).isInstanceOf(PaginationStatus.Paginating::class.java)
            }
        }

    @Test
    fun `GIVEN a full first page WHEN loading THEN does not auto-load more`() = runTest {
        val fetcher = ScriptedFetcher { page(itemCount = 25, isLast = false) }
        val repo = fakeRepository(fetcher)
        val manager = createManager(repo)

        withLoadedManager(manager) {
            // first page already exceeds the target → no auto-load, behaves like a normal scroll-driven list.
            assertThat(fetcher.fetchCount).isEqualTo(1)
            assertThat(repo.loadedItemsCount()).isEqualTo(25)
        }
    }

    @Test
    fun `GIVEN a gap of empty pages mid-history WHEN scrolled to the end THEN auto-loads through the gap`() =
        runTest {
            // A full first page (no auto-load), then two empty-for-token pages (a gap of other-token
            // activity), then one final item. Mirrors a busy account where a token has a long activity gap.
            val fetcher = ScriptedFetcher { call ->
                when (call) {
                    0 -> page(itemCount = 25, isLast = false)
                    1 -> page(itemCount = 0, isLast = false)
                    2 -> page(itemCount = 0, isLast = false)
                    else -> page(itemCount = 1, isLast = true)
                }
            }
            val repo = fakeRepository(fetcher)
            val manager = createManager(repo)

            withLoadedManager(manager) {
                // full first page → no auto-load yet, the list is scrollable.
                assertThat(fetcher.fetchCount).isEqualTo(1)
                assertThat(repo.loadedItemsCount()).isEqualTo(25)

                // user scrolls to the bottom → one loadMore; the empty gap must be auto-bridged to the end,
                // otherwise the list dead-ends and the final transaction is never reached.
                manager.loadMore(userWalletId, currency)
                advanceUntilIdle()

                assertThat(fetcher.fetchCount).isEqualTo(4)
                assertThat(repo.loadedItemsCount()).isEqualTo(26)
                assertThat(repo.status()).isInstanceOf(PaginationStatus.EndOfPagination::class.java)
            }
        }

    private suspend fun TestScope.withLoadedManager(
        manager: TxHistoryListManager,
        assertions: suspend TestScope.() -> Unit,
    ) {
        // init() collects forever, so run it in a child coroutine and cancel it once assertions are done.
        // Cancellation resets the source state, so assertions must run before it.
        val initJob = launch { manager.init() }
        advanceUntilIdle()
        manager.startLoading()
        advanceUntilIdle()
        try {
            assertions()
        } finally {
            initJob.cancel()
        }
    }

    private fun TestScope.fakeRepository(
        fetcher: BatchFetcher<TxHistoryListConfig, PaginationWrapper<TxInfo>>,
    ): FakeRepository = FakeRepository(testDispatchers(StandardTestDispatcher(testScheduler)), fetcher)

    private fun createManager(repository: FakeRepository): TxHistoryListManager = TxHistoryListManager(
        repository = repository,
        dispatchers = repository.dispatchers,
        userWalletId = userWalletId,
        currency = currency,
        designFeatureToggles = mockk { every { isRedesignEnabled } returns false },
        txHistoryUiActions = mockk(relaxed = true),
        lookupDataFlow = emptyFlow(),
        legacyTxHistoryItemConverter = mockk<TxHistoryItemToTransactionStateConverter>(relaxed = true),
    )

    private fun page(itemCount: Int, isLast: Boolean): Page2Spec =
        Page2Spec(itemCount = itemCount, isLast = isLast)

    private fun testDispatchers(dispatcher: CoroutineDispatcher): CoroutineDispatcherProvider =
        object : CoroutineDispatcherProvider {
            override val main: CoroutineDispatcher = dispatcher
            override val mainImmediate: CoroutineDispatcher = dispatcher
            override val io: CoroutineDispatcher = dispatcher
            override val default: CoroutineDispatcher = dispatcher
            override val single: CoroutineDispatcher = dispatcher
        }

    /** Page description. The fetcher turns it into a wrapper with a unique cursor, mirroring real pagination. */
    private data class Page2Spec(val itemCount: Int, val isLast: Boolean)

    private class ScriptedFetcher(
        private val pageAt: (call: Int) -> Page2Spec,
    ) : BatchFetcher<TxHistoryListConfig, PaginationWrapper<TxInfo>> {

        var fetchCount = 0
            private set

        override suspend fun fetchFirst(requestParams: TxHistoryListConfig) = produce()

        override suspend fun fetchNext(
            overrideRequestParams: TxHistoryListConfig?,
            lastResult: BatchFetchResult<PaginationWrapper<TxInfo>>,
        ) = produce()

        private fun produce(): BatchFetchResult<PaginationWrapper<TxInfo>> {
            val spec = pageAt(fetchCount)
            // A unique cursor per fetch mirrors real pagination (each page has its own paginationToken) and
            // prevents StateFlow from conflating two otherwise-identical empty pages.
            val wrapper = PaginationWrapper(
                currentPage = if (fetchCount == 0) Page.Initial else Page.Next(value = "cursor-$fetchCount"),
                nextPage = if (spec.isLast) Page.LastPage else Page.Next(value = "cursor-${fetchCount + 1}"),
                items = List(spec.itemCount) { mockk<TxInfo>(relaxed = true) },
            )
            fetchCount++
            return BatchFetchResult.Success(
                data = wrapper,
                empty = wrapper.items.isEmpty(),
                last = spec.isLast,
            )
        }
    }

    private class FakeRepository(
        val dispatchers: CoroutineDispatcherProvider,
        private val fetcher: BatchFetcher<TxHistoryListConfig, PaginationWrapper<TxInfo>>,
    ) : TxHistoryRepositoryV2 {

        private lateinit var batchFlow: TxHistoryListBatchFlow

        override fun getTxHistoryBatchFlow(
            batchSize: Int,
            context: TxHistoryListBatchingContext,
        ): TxHistoryListBatchFlow = BatchListSource(
            fetchDispatcher = dispatchers.io,
            context = context,
            generateNewKey = { keys -> keys.lastOrNull()?.inc() ?: 0 },
            batchFetcher = fetcher,
        ).toBatchFlow().also { batchFlow = it }

        override fun getExpressHistory(
            userWalletId: UserWalletId,
            currency: CryptoCurrency,
            fromCreatedAtMillis: Long,
        ) = emptyFlow<List<ExpressTx>>()

        fun loadedItemsCount(): Int = batchFlow.state.value.data.sumOf { batch -> batch.data.items.size }

        fun status(): PaginationStatus<*> = batchFlow.state.value.status
    }
}