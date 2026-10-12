package com.tangem.data.polymarket.pagination

import arrow.core.Either
import com.google.common.truth.Truth.assertThat
import com.tangem.domain.polymarket.model.PolymarketAddresses
import com.tangem.domain.polymarket.model.PolymarketPosition
import com.tangem.domain.polymarket.model.PolymarketPositionsError
import com.tangem.domain.polymarket.model.PolymarketPositionsListConfig
import com.tangem.domain.polymarket.model.PolymarketPositionsPage
import com.tangem.domain.polymarket.model.PolymarketPositionsTab
import com.tangem.pagination.BatchFetchResult
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

internal class PolymarketPositionsBatchFetcherTest {

    private val addresses: PolymarketAddresses = mockk()
    private val config = PolymarketPositionsListConfig(addresses, PolymarketPositionsTab.ACTIVE)
    private val position: PolymarketPosition.Open = mockk()

    @Test
    fun `GIVEN a failed second page WHEN retrying THEN its offset is retried without skipping holdings`() = runTest {
        // Arrange
        val offsets = mutableListOf<Int>()
        val fetcher = PolymarketPositionsBatchFetcher(batchSize = 10) { _, _, offset ->
            offsets.add(offset)
            if (offsets.size == 2) {
                Either.Left(PolymarketPositionsError.Network)
            } else {
                Either.Right(PolymarketPositionsPage(listOf(position), hasNext = true))
            }
        }

        // Act
        val first = fetcher.fetchFirst(config)
        val failure = fetcher.fetchNext(overrideRequestParams = null, lastResult = first)
        val retry = fetcher.fetchNext(overrideRequestParams = null, lastResult = failure)

        // Assert
        assertThat(offsets).containsExactly(0, 10, 10).inOrder()
        assertThat(retry).isEqualTo(BatchFetchResult.Success(listOf(position), empty = false, last = false))
    }

    @Test
    fun `GIVEN an empty first page WHEN loading THEN it is a valid empty portfolio`() = runTest {
        // Arrange
        val fetcher = PolymarketPositionsBatchFetcher(batchSize = 10) { _, _, _ ->
            Either.Right(PolymarketPositionsPage(emptyList(), hasNext = false))
        }

        // Act
        val result = fetcher.fetchFirst(config)

        // Assert
        assertThat(result).isEqualTo(BatchFetchResult.Success(emptyList<PolymarketPosition>(), empty = true, last = true))
    }

    @Test
    fun `GIVEN a short page with hasNext WHEN loading THEN pagination continues by requested limit`() = runTest {
        // Arrange
        val offsets = mutableListOf<Int>()
        val fetcher = PolymarketPositionsBatchFetcher(batchSize = 10) { _, _, offset ->
            offsets.add(offset)
            Either.Right(PolymarketPositionsPage(listOf(position), hasNext = true))
        }

        // Act
        val first = fetcher.fetchFirst(config)
        fetcher.fetchNext(overrideRequestParams = null, lastResult = first)

        // Assert
        assertThat(offsets).containsExactly(0, 10).inOrder()
    }

    @Test
    fun `GIVEN a different wallet and tab WHEN reloading THEN pagination restarts with the new identity`() = runTest {
        // Arrange
        val requests = mutableListOf<Pair<PolymarketPositionsListConfig, Int>>()
        val other = PolymarketPositionsListConfig(mockk(), PolymarketPositionsTab.FINISHED)
        val fetcher = PolymarketPositionsBatchFetcher(batchSize = 10) { params, _, offset ->
            requests.add(params to offset)
            Either.Right(PolymarketPositionsPage(listOf(position), hasNext = true))
        }

        // Act
        val first = fetcher.fetchFirst(config)
        fetcher.fetchNext(overrideRequestParams = null, lastResult = first)
        fetcher.fetchFirst(other)

        // Assert
        assertThat(requests).containsExactly(config to 0, config to 10, other to 0).inOrder()
    }

    @Test
    fun `GIVEN the final page WHEN loading more THEN the backend is not called again`() = runTest {
        // Arrange
        var calls = 0
        val fetcher = PolymarketPositionsBatchFetcher(batchSize = 10) { _, _, _ ->
            calls++
            Either.Right(PolymarketPositionsPage(listOf(position), hasNext = false))
        }

        // Act
        val first = fetcher.fetchFirst(config)
        fetcher.fetchNext(overrideRequestParams = null, lastResult = first)

        // Assert
        assertThat(calls).isEqualTo(1)
    }

    @Test
    fun `GIVEN all pages advertise more WHEN the offset cap is reached THEN no unsupported request is sent`() = runTest {
        // Arrange
        val offsets = mutableListOf<Int>()
        val fetcher = PolymarketPositionsBatchFetcher(batchSize = 30) { _, _, offset ->
            offsets.add(offset)
            Either.Right(PolymarketPositionsPage(listOf(position), hasNext = true))
        }

        // Act
        var result = fetcher.fetchFirst(config)
        while (result is BatchFetchResult.Success && !result.last) {
            result = fetcher.fetchNext(overrideRequestParams = null, lastResult = result)
        }

        // Assert
        assertThat(offsets.last()).isEqualTo(9_990)
        assertThat(result).isEqualTo(BatchFetchResult.Success(listOf(position), empty = false, last = true))
    }
}