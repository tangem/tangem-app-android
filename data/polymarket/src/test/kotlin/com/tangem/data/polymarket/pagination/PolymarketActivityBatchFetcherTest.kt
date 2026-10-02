package com.tangem.data.polymarket.pagination

import arrow.core.Either
import com.google.common.truth.Truth.assertThat
import com.tangem.domain.polymarket.model.PolymarketActivityError
import com.tangem.domain.polymarket.model.PolymarketActivityListConfig
import com.tangem.domain.polymarket.model.PolymarketActivityPage
import com.tangem.pagination.BatchFetchResult
import io.mockk.mockk
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

internal class PolymarketActivityBatchFetcherTest {

    private val config: PolymarketActivityListConfig = mockk()

    @Test
    fun `GIVEN a failed next page WHEN retrying THEN reuses the cursor and does not refetch the first page`() = runTest {
        // Arrange
        val cursors = mutableListOf<String?>()
        val responses = ArrayDeque(
            listOf(
                Either.Right(PolymarketActivityPage(emptyList(), "cursor-1")),
                Either.Left(PolymarketActivityError.Network),
                Either.Right(PolymarketActivityPage(emptyList(), null)),
            ),
        )
        val fetcher = PolymarketActivityBatchFetcher(30) { _, cursor, _ ->
            cursors += cursor
            responses.removeFirst()
        }

        // Act
        val first = fetcher.fetchFirst(config)
        val failed = fetcher.fetchNext(null, first)
        val retried = fetcher.fetchNext(null, failed)
        fetcher.fetchNext(null, retried)

        // Assert
        assertThat(cursors).containsExactly(null, "cursor-1", "cursor-1").inOrder()
        assertThat(failed).isInstanceOf(BatchFetchResult.Error::class.java)
        assertThat(retried).isEqualTo(BatchFetchResult.Success(emptyList<Any>(), empty = true, last = true))
    }

    @Test
    fun `GIVEN a repeated cursor WHEN loading the next page THEN stops the pagination loop`() = runTest {
        // Arrange
        val fetcher = PolymarketActivityBatchFetcher(30) { _, _, _ ->
            Either.Right(PolymarketActivityPage(emptyList(), "cursor-1"))
        }

        // Act
        val first = fetcher.fetchFirst(config)
        val next = fetcher.fetchNext(null, first)

        // Assert
        assertThat(first).isEqualTo(BatchFetchResult.Success(emptyList<Any>(), empty = true, last = false))
        assertThat(next).isInstanceOf(BatchFetchResult.Error::class.java)
    }

    @Test
    fun `GIVEN a new filter WHEN reloading THEN resets cursor tracking`() = runTest {
        // Arrange
        val other: PolymarketActivityListConfig = mockk()
        val requests = mutableListOf<Pair<PolymarketActivityListConfig, String?>>()
        val fetcher = PolymarketActivityBatchFetcher(30) { value, cursor, _ ->
            requests += value to cursor
            Either.Right(PolymarketActivityPage(emptyList(), "cursor-1"))
        }

        // Act
        fetcher.fetchFirst(config)
        val actual = fetcher.fetchFirst(other)

        // Assert
        assertThat(requests).containsExactly(config to null, other to null).inOrder()
        assertThat(actual).isInstanceOf(BatchFetchResult.Success::class.java)
    }

    @Test
    fun `GIVEN a cancelled load WHEN fetching THEN cancellation is not a retryable page error`() = runTest {
        // Arrange
        val fetcher = PolymarketActivityBatchFetcher(30) { _, _, _ -> throw CancellationException("left screen") }

        // Act
        val error = runCatching { fetcher.fetchFirst(config) }.exceptionOrNull()

        // Assert
        assertThat(error).isInstanceOf(CancellationException::class.java)
    }
}