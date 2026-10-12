package com.tangem.features.polymarket.impl.positions.model

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.polymarket.PolymarketPositionsRepository
import com.tangem.domain.polymarket.flow.DefaultPredictionPositionsUpdater
import com.tangem.domain.polymarket.model.PolymarketAddresses
import com.tangem.domain.polymarket.model.PolymarketPosition
import com.tangem.domain.polymarket.model.PolymarketPositionResult
import com.tangem.domain.polymarket.model.PolymarketPositionsFilter
import com.tangem.domain.polymarket.model.PolymarketPositionsBatchingContext
import com.tangem.domain.polymarket.model.PolymarketPositionsListConfig
import com.tangem.domain.polymarket.model.PolymarketPositionsTab
import com.tangem.domain.polymarket.model.PolymarketPositionsUpdateRequest
import com.tangem.domain.polymarket.usecase.CreatePolymarketPositionsSessionUseCase
import com.tangem.domain.polymarket.usecase.DerivePolymarketAddressesUseCase
import com.tangem.features.polymarket.impl.positions.ui.state.PolymarketPositionsUM.Content
import com.tangem.features.polymarket.impl.positions.ui.state.PolymarketPositionsUM.NextPage
import com.tangem.pagination.Batch
import com.tangem.pagination.BatchFetchResult
import com.tangem.pagination.BatchListSource
import com.tangem.pagination.BatchUpdateFetcher
import com.tangem.pagination.BatchUpdateResult
import com.tangem.pagination.fetcher.LimitOffsetBatchFetcher
import com.tangem.pagination.toBatchFlow
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
internal class PolymarketPositionsLoaderTest {

    private val repository: PolymarketPositionsRepository = mockk()
    private val deriveAddresses: DerivePolymarketAddressesUseCase = mockk()
    private val wallet = UserWalletId("011")
    private val addresses: PolymarketAddresses = mockk()
    private val requests = mutableListOf<Pair<PolymarketPositionsListConfig, Int>>()
    private var fetch: suspend (PolymarketPositionsListConfig, Int) -> BatchFetchResult<List<PolymarketPosition>> =
        { _, _ -> page("one") }

    @Test
    fun `GIVEN stored wallet WHEN resumed THEN reads active positions without deriving keys`() = runTest {
        // Arrange
        val feed = createLoader().createFeed(wallet, PolymarketPositionsTab.ACTIVE)

        // Act
        feed.resume(backgroundScope)
        runCurrent()

        // Assert
        assertThat(requests).containsExactly(PolymarketPositionsListConfig(addresses, PolymarketPositionsTab.ACTIVE) to 0)
        assertThat(ids(feed)).containsExactly("condition:one")
        coVerify(exactly = 0) { deriveAddresses.invoke(any()) }
    }

    @Test
    fun `GIVEN cached list WHEN filter changes and resets before recalculation THEN restores content without loading forever`() = runTest {
        // Arrange
        val feed = createLoader().createFeed(wallet, PolymarketPositionsTab.ACTIVE)
        feed.resume(backgroundScope)
        runCurrent()

        // Act
        feed.selectFilter(PolymarketPositionsFilter.WON)
        feed.selectFilter(PolymarketPositionsFilter.ALL)
        runCurrent()

        // Assert
        assertThat(ids(feed)).containsExactly("condition:one")
        assertThat(requests.map { it.second }).containsExactly(0)
    }

    @Test
    fun `GIVEN unmatched first page WHEN Won is selected THEN waits for matches and clearing reuses all cached rows`() = runTest {
        // Arrange
        val feed = createLoader().createFeed(wallet, PolymarketPositionsTab.ACTIVE)
        val response = CompletableDeferred<BatchFetchResult<List<PolymarketPosition>>>()
        fetch = { _, offset -> if (offset == 0) page("active", last = false) else response.await() }
        feed.resume(backgroundScope)
        runCurrent()

        // Act
        feed.selectFilter(PolymarketPositionsFilter.WON)
        runCurrent()
        val loading = feed.state.value.content
        response.complete(
            BatchFetchResult.Success(
                listOf(createPolymarketPosition(assetId = "won", result = PolymarketPositionResult.WON)),
                empty = false,
                last = true,
            ),
        )
        runCurrent()
        val matched = ids(feed)
        feed.selectFilter(PolymarketPositionsFilter.ALL)
        runCurrent()

        // Assert
        assertThat(loading).isEqualTo(Content.Loading)
        assertThat(matched).containsExactly("condition:won")
        assertThat(ids(feed)).containsExactly("condition:active", "condition:won").inOrder()
        assertThat(requests.map { it.second }).containsExactly(0, 30).inOrder()
    }

    @Test
    fun `GIVEN no matching rows WHEN source ends THEN shows empty and clearing restores cached positions`() = runTest {
        // Arrange
        val feed = createLoader().createFeed(wallet, PolymarketPositionsTab.ACTIVE)
        feed.resume(backgroundScope)
        runCurrent()

        // Act
        feed.selectFilter(PolymarketPositionsFilter.LOST)
        runCurrent()
        val filtered = feed.state.value.content
        feed.selectFilter(PolymarketPositionsFilter.ALL)
        runCurrent()

        // Assert
        assertThat(filtered).isEqualTo(Content.Empty)
        assertThat(ids(feed)).containsExactly("condition:one")
        assertThat(requests.map { it.second }).containsExactly(0)
    }

    @Test
    fun `GIVEN filtered continuation fails without matches WHEN retried THEN keeps the filter and retry offset`() = runTest {
        // Arrange
        val feed = createLoader().createFeed(wallet, PolymarketPositionsTab.ACTIVE)
        fetch = { _, offset -> if (offset == 0) page("active", last = false) else error() }
        feed.resume(backgroundScope)
        runCurrent()

        // Act
        feed.selectFilter(PolymarketPositionsFilter.WON)
        runCurrent()
        val failed = feed.state.value.content as Content.Error
        fetch = { _, _ -> page("another active") }
        failed.onRetry()
        runCurrent()

        // Assert
        assertThat(feed.state.value.content).isEqualTo(Content.Empty)
        assertThat(requests.map { it.second }).containsExactly(0, 30, 30).inOrder()
    }

    @Test
    fun `GIVEN empty intermediate page WHEN continuation fails THEN shows loading and a retryable error`() = runTest {
        // Arrange
        val feed = createLoader().createFeed(wallet, PolymarketPositionsTab.ACTIVE)
        val response = CompletableDeferred<BatchFetchResult<List<PolymarketPosition>>>()
        fetch = { _, offset ->
            if (offset == 0) BatchFetchResult.Success(emptyList(), empty = true, last = false) else response.await()
        }

        // Act
        feed.resume(backgroundScope)
        runCurrent()
        val loading = feed.state.value.content
        response.complete(error())
        runCurrent()
        val failed = feed.state.value.content as Content.Error
        fetch = { _, _ -> page("one") }
        failed.onRetry()
        runCurrent()

        // Assert
        assertThat(loading).isEqualTo(Content.Loading)
        assertThat(ids(feed)).containsExactly("condition:one")
        assertThat(requests.map { it.second }).containsExactly(0, 30, 30).inOrder()
    }

    @Test
    fun `GIVEN failed next page WHEN retry tapped THEN retries same offset and stops at the end`() = runTest {
        // Arrange
        val feed = createLoader().createFeed(wallet, PolymarketPositionsTab.ACTIVE)
        fetch = { _, offset -> if (offset == 0) page("one", last = false) else error() }
        feed.resume(backgroundScope)
        runCurrent()
        val loadMore = (data(feed).nextPage as NextPage.Ready).onLoadMore

        // Act
        loadMore()
        loadMore()
        runCurrent()
        val failed = data(feed)
        fetch = { _, _ -> page("two") }
        (failed.nextPage as NextPage.Error).onRetry()
        runCurrent()
        loadMore()
        runCurrent()

        // Assert
        assertThat(failed.positions.map { it.id }).containsExactly("condition:one")
        assertThat(ids(feed)).containsExactly("condition:one", "condition:two").inOrder()
        assertThat(data(feed).nextPage).isEqualTo(NextPage.End)
        assertThat(requests.map { it.second }).containsExactly(0, 30, 30).inOrder()
    }

    @Test
    fun `GIVEN two loaded pages WHEN time passes THEN does not poll or reload pages`() = runTest {
        // Arrange
        val feed = twoPages()
        val initial = data(feed)

        // Act
        advanceTimeBy(120_000)
        runCurrent()

        // Assert
        assertThat(feed.state.value.content).isSameInstanceAs(initial)
        assertThat(requests.map { it.second }).containsExactly(0, 30).inOrder()
    }

    @Test
    fun `GIVEN loaded pages WHEN returning from details THEN re-reads them in place and continues the same pagination`() =
        runTest {
            // Arrange
            val feed = createLoader().createFeed(wallet, PolymarketPositionsTab.ACTIVE)
            fetch = { _, offset -> page(offset.toString(), last = offset == 60) }
            feed.resume(backgroundScope)
            runCurrent()
            (data(feed).nextPage as NextPage.Ready).onLoadMore()
            runCurrent()
            fetch = { _, offset -> page(if (offset == 30) "31" else offset.toString(), last = offset == 60) }

            // Act
            feed.pause()
            feed.resume(backgroundScope)
            runCurrent()
            val returned = data(feed)
            (returned.nextPage as NextPage.Ready).onLoadMore()
            runCurrent()

            // Assert
            assertThat(returned.positions.map { it.id }).containsExactly("condition:0", "condition:31").inOrder()
            assertThat(ids(feed)).containsExactly("condition:0", "condition:31", "condition:60").inOrder()
            assertThat(requests.map { it.second }).containsExactly(0, 30, 0, 30, 60).inOrder()
        }

    @Test
    fun `GIVEN closed list WHEN opened in a new feed THEN starts from the first page`() = runTest {
        // Arrange
        val previous = twoPages()
        previous.dispose()
        val reopened = createLoader().createFeed(wallet, PolymarketPositionsTab.ACTIVE)

        // Act
        reopened.resume(backgroundScope)
        runCurrent()

        // Assert
        assertThat(ids(reopened)).containsExactly("condition:one")
        assertThat(requests.map { it.second }).containsExactly(0, 30, 0).inOrder()
    }

    @Test
    fun `GIVEN page overlaps with previous page WHEN appended THEN retains unique cards in server order`() = runTest {
        // Arrange
        val feed = createLoader().createFeed(wallet, PolymarketPositionsTab.ACTIVE)
        fetch = { _, offset ->
            if (offset == 0) page("one", last = false) else BatchFetchResult.Success(
                listOf(createPolymarketPosition(assetId = "one"), createPolymarketPosition(assetId = "two")),
                empty = false,
                last = true,
            )
        }
        feed.resume(backgroundScope)
        runCurrent()

        // Act
        (data(feed).nextPage as NextPage.Ready).onLoadMore()
        runCurrent()

        // Assert
        assertThat(ids(feed)).containsExactly("condition:one", "condition:two").inOrder()
    }

    @Test
    fun `GIVEN paused feed WHEN time passes or old callback runs THEN nothing loads until resume re-reads the page`() =
        runTest {
            // Arrange
            val feed = createLoader().createFeed(wallet, PolymarketPositionsTab.ACTIVE)
            fetch = { _, _ -> page("one", last = false) }
            feed.resume(backgroundScope)
            runCurrent()
            val loadMore = (data(feed).nextPage as NextPage.Ready).onLoadMore

            // Act
            feed.pause()
            loadMore()
            advanceTimeBy(60_000)
            runCurrent()

            // Assert
            assertThat(requests).hasSize(1)

            // Act
            feed.resume(backgroundScope)
            runCurrent()

            // Assert
            assertThat(requests.map { it.second }).containsExactly(0, 0).inOrder()
        }

    @Test
    fun `GIVEN disposed feed WHEN old request completes THEN cannot publish or affect the new feed`() = runTest {
        // Arrange
        val loader = createLoader()
        val previous = loader.createFeed(wallet, PolymarketPositionsTab.ACTIVE)
        val oldResponse = CompletableDeferred<Unit>()
        fetch = { _, _ -> withContext(NonCancellable) { oldResponse.await(); page("old") } }
        previous.resume(backgroundScope)
        runCurrent()
        previous.dispose()
        val current = loader.createFeed(wallet, PolymarketPositionsTab.ACTIVE)
        fetch = { _, _ -> page("new") }

        // Act
        current.resume(backgroundScope)
        runCurrent()
        oldResponse.complete(Unit)
        runCurrent()

        // Assert
        assertThat(ids(current)).containsExactly("condition:new")
        assertThat(previous.state.value.content).isEqualTo(Content.Loading)
    }

    @Test
    fun `GIVEN missing stored addresses WHEN retried THEN reads portfolio without onboarding`() = runTest {
        // Arrange
        val feed = createLoader().createFeed(wallet, PolymarketPositionsTab.FINISHED)
        coEvery { deriveAddresses.stored(wallet) } returns null
        feed.resume(backgroundScope)
        runCurrent()
        val failed = feed.state.value.content as Content.Error

        // Act
        coEvery { deriveAddresses.stored(wallet) } returns addresses
        failed.onRetry()
        runCurrent()

        // Assert
        assertThat(requests).containsExactly(PolymarketPositionsListConfig(addresses, PolymarketPositionsTab.FINISHED) to 0)
        coVerify(exactly = 0) { deriveAddresses.invoke(any()) }
    }

    @Test
    fun `GIVEN different wallets and tabs WHEN loaded THEN keeps independent request and state scopes`() = runTest {
        // Arrange
        val loader = createLoader()
        val otherWallet = UserWalletId("022")
        val otherAddresses: PolymarketAddresses = mockk()
        coEvery { deriveAddresses.stored(otherWallet) } returns otherAddresses
        val active = loader.createFeed(wallet, PolymarketPositionsTab.ACTIVE)
        val finished = loader.createFeed(wallet, PolymarketPositionsTab.FINISHED)
        val other = loader.createFeed(otherWallet, PolymarketPositionsTab.ACTIVE)
        fetch = { config, _ -> page(if (config.addresses == otherAddresses) "other" else config.tab.name) }

        // Act
        active.resume(backgroundScope)
        finished.resume(backgroundScope)
        other.resume(backgroundScope)
        runCurrent()

        // Assert
        assertThat(ids(active)).containsExactly("condition:ACTIVE")
        assertThat(ids(finished)).containsExactly("condition:FINISHED")
        assertThat(ids(other)).containsExactly("condition:other")
    }

    @Test
    fun `GIVEN loaded Finished page WHEN card tapped THEN opens that snapshot without another portfolio request`() = runTest {
        // Arrange
        val selected = createClosedPolymarketPosition()
        val other = selected.copy(id = selected.id.copy(assetId = "other"), title = "Another position")
        val onClosedClick: (PolymarketPosition.Closed) -> Unit = mockk(relaxed = true)
        val feed = createLoader().createFeed(
            wallet, PolymarketPositionsTab.FINISHED, onClosedPositionClick = onClosedClick,
        )
        fetch = { _, _ -> BatchFetchResult.Success(listOf(selected, other), empty = false, last = true) }
        feed.resume(backgroundScope)
        runCurrent()
        val initial = data(feed)

        // Act
        initial.positions[1].onClick?.invoke()
        runCurrent()

        // Assert
        verify(exactly = 1) { onClosedClick(other) }
        verify(exactly = 0) { onClosedClick(selected) }
        assertThat(feed.state.value.content).isSameInstanceAs(initial)
        assertThat(requests).containsExactly(PolymarketPositionsListConfig(addresses, PolymarketPositionsTab.FINISHED) to 0)
        coVerify(exactly = 1) { deriveAddresses.stored(wallet) }
        coVerify(exactly = 0) { deriveAddresses.invoke(any()) }
    }

    private suspend fun TestScope.twoPages(): PolymarketPositionsLoader.Feed {
        val feed = createLoader().createFeed(wallet, PolymarketPositionsTab.ACTIVE)
        fetch = { _, offset -> if (offset == 0) page("one", last = false) else page("two") }
        feed.resume(backgroundScope)
        runCurrent()
        (data(feed).nextPage as NextPage.Ready).onLoadMore()
        runCurrent()
        return feed
    }

    private fun TestScope.createLoader(): PolymarketPositionsLoader {
        val dispatcher = StandardTestDispatcher(testScheduler)
        coEvery { deriveAddresses.stored(wallet) } returns addresses
        every { repository.getPositionsBatchFlow(any(), any()) } answers {
            val context = firstArg<PolymarketPositionsBatchingContext>()
            val size = secondArg<Int>()
            assertThat(size).isEqualTo(30)
            BatchListSource(
                fetchDispatcher = dispatcher,
                context = context,
                generateNewKey = { keys -> keys.size },
                batchFetcher = LimitOffsetBatchFetcher<PolymarketPositionsListConfig, List<PolymarketPosition>>(
                    prefetchDistance = size,
                    batchSize = size,
                    subFetcher = { request, _, _ ->
                        requests += request.params to request.offset
                        fetch(request.params, request.offset)
                    },
                ),
                updateFetcher = refreshByOffset(size),
            ).toBatchFlow()
        }
        val dispatchers = TestingCoroutineDispatcherProvider(dispatcher, dispatcher, dispatcher, dispatcher, dispatcher)
        return PolymarketPositionsLoader(
            createSession = CreatePolymarketPositionsSessionUseCase(
                repository = repository,
                deriveAddresses = deriveAddresses,
                updateListener = DefaultPredictionPositionsUpdater(),
                dispatchers = dispatchers,
            ),
            dispatchers = dispatchers,
        )
    }

    private fun page(id: String, last: Boolean = true): BatchFetchResult<List<PolymarketPosition>> =
        BatchFetchResult.Success(listOf(createPolymarketPosition(assetId = id)), empty = false, last = last)

    private fun error() = BatchFetchResult.Error(IllegalStateException("Offline"))

    private fun data(feed: PolymarketPositionsLoader.Feed) = feed.state.value.content as Content.Data

    private fun ids(feed: PolymarketPositionsLoader.Feed) = data(feed).positions.map { it.id }

    /** Mirrors the production update fetcher: page `k` is read again at offset `k * size` and replaced. */
    private fun refreshByOffset(size: Int) =
        object : BatchUpdateFetcher<Int, List<PolymarketPosition>, PolymarketPositionsUpdateRequest> {
            override suspend fun BatchUpdateFetcher.UpdateContext<Int, List<PolymarketPosition>>.fetchUpdateAsync(
                toUpdate: List<Batch<Int, List<PolymarketPosition>>>,
                updateRequest: PolymarketPositionsUpdateRequest,
            ) {
                toUpdate.forEach { batch ->
                    requests += updateRequest.config to batch.key * size
                    val positions = when (val result = fetch(updateRequest.config, batch.key * size)) {
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