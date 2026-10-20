package com.tangem.data.polymarket.pagination

import arrow.core.Either
import com.google.common.truth.Truth.assertThat
import com.tangem.domain.polymarket.error.PolymarketPositionsReadException
import com.tangem.domain.polymarket.model.PolymarketAddresses
import com.tangem.domain.polymarket.model.PolymarketPosition
import com.tangem.domain.polymarket.model.PolymarketPositionsError
import com.tangem.domain.polymarket.model.PolymarketPositionsListConfig
import com.tangem.domain.polymarket.model.PolymarketPositionsPage
import com.tangem.domain.polymarket.model.PolymarketPositionsPageRequest
import com.tangem.domain.polymarket.model.PolymarketPositionsPageRequest.First
import com.tangem.domain.polymarket.model.PolymarketPositionsPageRequest.Next
import com.tangem.domain.polymarket.model.PolymarketPositionsTab
import com.tangem.pagination.BatchFetchResult
import com.tangem.pagination.exception.EndOfPaginationException
import io.mockk.mockk
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

internal class PolymarketPositionsBatchFetcherTest {

    private val addresses: PolymarketAddresses = mockk()
    private val config = PolymarketPositionsListConfig(addresses, PolymarketPositionsTab.ACTIVE)
    private val position: PolymarketPosition.Open = mockk()
    private val requests = mutableListOf<PolymarketPositionsPageRequest>()

    @Test
    fun `GIVEN pages with cursors WHEN paging THEN the first page is asked by size and the rest by cursor only`() =
        runTest {
            // Arrange
            val fetcher = fetcher { request ->
                requests += request
                val cursor = if (request is First) "cursor-1" else "cursor-2"
                Either.Right(PolymarketPositionsPage(listOf(position), nextCursor = cursor))
            }

            // Act
            val first = fetcher.fetchFirst(config)
            fetcher.fetchNext(overrideRequestParams = null, lastResult = first)

            // Assert
            assertThat(requests).containsExactly(First(10), Next("cursor-1")).inOrder()
        }

    @Test
    fun `GIVEN a failed second page WHEN retrying THEN its cursor is retried without skipping holdings`() = runTest {
        // Arrange
        val fetcher = fetcher { request ->
            requests += request
            if (requests.size == 2) {
                Either.Left(PolymarketPositionsError.Network)
            } else {
                Either.Right(PolymarketPositionsPage(listOf(position), nextCursor = "cursor-${requests.size}"))
            }
        }

        // Act
        val first = fetcher.fetchFirst(config)
        val failure = fetcher.fetchNext(overrideRequestParams = null, lastResult = first)
        val retry = fetcher.fetchNext(overrideRequestParams = null, lastResult = failure)

        // Assert
        assertThat(requests).containsExactly(First(10), Next("cursor-1"), Next("cursor-1")).inOrder()
        assertThat((failure as BatchFetchResult.Error).throwable).isInstanceOf(PolymarketPositionsReadException::class.java)
        assertThat(retry).isEqualTo(BatchFetchResult.Success(listOf(position), isEmpty = false, isLast = false))
    }

    @Test
    fun `GIVEN an empty first page WHEN loading THEN it is a valid empty portfolio`() = runTest {
        // Arrange
        val fetcher = fetcher { Either.Right(PolymarketPositionsPage(emptyList(), nextCursor = null)) }

        // Act
        val result = fetcher.fetchFirst(config)

        // Assert
        assertThat(result).isEqualTo(BatchFetchResult.Success(emptyList<PolymarketPosition>(), isEmpty = true, isLast = true))
    }

    @Test
    fun `GIVEN a different wallet and tab WHEN reloading THEN pagination restarts with the new identity`() = runTest {
        // Arrange
        val lists = mutableListOf<Pair<PolymarketPositionsListConfig, PolymarketPositionsPageRequest>>()
        val other = PolymarketPositionsListConfig(mockk(), PolymarketPositionsTab.FINISHED)
        val fetcher = PolymarketPositionsBatchFetcher(batchSize = 10) { params, request ->
            lists += params to request
            Either.Right(PolymarketPositionsPage(listOf(position), nextCursor = "cursor-${lists.size}"))
        }

        // Act
        val first = fetcher.fetchFirst(config)
        fetcher.fetchNext(overrideRequestParams = null, lastResult = first)
        fetcher.fetchFirst(other)

        // Assert
        assertThat(lists).containsExactly(config to First(10), config to Next("cursor-1"), other to First(10)).inOrder()
    }

    @Test
    fun `GIVEN the final page WHEN loading more THEN the backend is not called again`() = runTest {
        // Arrange
        val fetcher = fetcher { request ->
            requests += request
            Either.Right(PolymarketPositionsPage(listOf(position), nextCursor = null))
        }

        // Act
        val first = fetcher.fetchFirst(config)
        val next = fetcher.fetchNext(overrideRequestParams = null, lastResult = first)

        // Assert
        assertThat(first).isEqualTo(BatchFetchResult.Success(listOf(position), isEmpty = false, isLast = true))
        assertThat((next as BatchFetchResult.Error).throwable).isInstanceOf(EndOfPaginationException::class.java)
        assertThat(requests).containsExactly(First(10))
    }

    @Test
    fun `GIVEN a repeated cursor WHEN loading the next page THEN the pagination loop is stopped`() = runTest {
        // Arrange
        val fetcher = fetcher { Either.Right(PolymarketPositionsPage(listOf(position), nextCursor = "cursor-1")) }

        // Act
        val first = fetcher.fetchFirst(config)
        val next = fetcher.fetchNext(overrideRequestParams = null, lastResult = first)

        // Assert
        assertThat(first).isEqualTo(BatchFetchResult.Success(listOf(position), isEmpty = false, isLast = false))
        assertThat(next).isInstanceOf(BatchFetchResult.Error::class.java)
    }

    @Test
    fun `GIVEN a cancelled load WHEN fetching THEN cancellation is not a retryable page error`() = runTest {
        // Arrange
        val fetcher = fetcher { throw CancellationException("left screen") }

        // Act
        val error = runCatching { fetcher.fetchFirst(config) }.exceptionOrNull()

        // Assert
        assertThat(error).isInstanceOf(CancellationException::class.java)
    }

    private fun fetcher(
        fetchPage: suspend (PolymarketPositionsPageRequest) ->
        Either<PolymarketPositionsError, PolymarketPositionsPage<PolymarketPosition>>,
    ) = PolymarketPositionsBatchFetcher(batchSize = 10) { _, request -> fetchPage(request) }
}