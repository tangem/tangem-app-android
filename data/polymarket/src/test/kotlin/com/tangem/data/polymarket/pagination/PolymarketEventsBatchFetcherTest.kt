package com.tangem.data.polymarket.pagination

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.polymarket.model.PolymarketEvent
import com.tangem.domain.polymarket.model.PolymarketEventsListConfig
import com.tangem.domain.polymarket.model.PolymarketEventsPage
import com.tangem.pagination.BatchFetchResult
import com.tangem.pagination.exception.EndOfPaginationException
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import java.io.IOException

internal class PolymarketEventsBatchFetcherTest {

    private val config = PolymarketEventsListConfig(category = 7)
    private val event: PolymarketEvent = mockk()

    private val requestedCursors = mutableListOf<String?>()

    @Test
    fun `GIVEN a page with more to come WHEN fetchFirst THEN it is not the last one`() = runTest {
        // Arrange
        val fetcher = createFetcher(pages = listOf(Result.success(page(hasNext = true, cursor = "cursor-1"))))

        // Act
        val result = fetcher.fetchFirst(config)

        // Assert
        assertThat(result).isInstanceOf(BatchFetchResult.Success::class.java)
        val success = result as BatchFetchResult.Success
        assertThat(success.data.events).containsExactly(event)
        assertThat(success.isLast).isFalse()
    }

    @Test
    fun `GIVEN a page without a cursor WHEN fetchFirst THEN it is the last one`() = runTest {
        // Arrange
        val fetcher = createFetcher(pages = listOf(Result.success(page(hasNext = true, cursor = null))))

        // Act
        val result = fetcher.fetchFirst(config)

        // Assert
        assertThat((result as BatchFetchResult.Success).isLast).isTrue()
    }

    @Test
    fun `GIVEN one failure WHEN fetchFirst THEN it is retried silently and succeeds`() = runTest {
        // Arrange
        val fetcher = createFetcher(
            pages = listOf(
                Result.failure(IOException("boom")),
                Result.success(page()),
            ),
        )

        // Act
        val result = fetcher.fetchFirst(config)

        // Assert
        assertThat(result).isInstanceOf(BatchFetchResult.Success::class.java)
        assertThat(requestedCursors).hasSize(2)
    }

    @Test
    fun `GIVEN two failures WHEN fetchFirst THEN the failure is reported`() = runTest {
        // Arrange
        val failure = IOException("boom")
        val fetcher = createFetcher(pages = List(size = 2) { Result.failure(failure) })

        // Act
        val result = fetcher.fetchFirst(config)

        // Assert
        assertThat((result as BatchFetchResult.Error).throwable).isEqualTo(failure)
        assertThat(requestedCursors).hasSize(2)
    }

    @Test
    fun `GIVEN an empty first page with nothing behind WHEN fetchFirst THEN the feed is empty and ends`() = runTest {
        // Arrange
        val fetcher = createFetcher(
            pages = listOf(Result.success(page(events = emptyList(), hasNext = false, cursor = null))),
        )

        // Act
        val result = fetcher.fetchFirst(config)

        // Assert
        val success = result as BatchFetchResult.Success
        assertThat(success.isEmpty).isTrue()
        assertThat(success.isLast).isTrue()
        assertThat(requestedCursors).containsExactly(null)
    }

    @Test
    fun `GIVEN an empty first page with more behind WHEN fetchFirst THEN its cursor is followed at once`() = runTest {
        // Arrange
        val fetcher = createFetcher(
            pages = listOf(
                Result.success(page(events = emptyList(), hasNext = true, cursor = "cursor-1")),
                Result.success(page(hasNext = true, cursor = "cursor-2")),
            ),
        )

        // Act
        val result = fetcher.fetchFirst(config)

        // Assert
        val success = result as BatchFetchResult.Success
        assertThat(success.data.events).containsExactly(event)
        assertThat(success.data.requestCursor).isEqualTo("cursor-1")
        assertThat(success.isLast).isFalse()
        assertThat(requestedCursors).containsExactly(null, "cursor-1").inOrder()
    }

    @Test
    fun `GIVEN a followed empty page WHEN fetchNext THEN the cursor of the served page is requested`() = runTest {
        // Arrange
        val fetcher = createFetcher(
            pages = listOf(
                Result.success(page(events = emptyList(), hasNext = true, cursor = "cursor-1")),
                Result.success(page(hasNext = true, cursor = "cursor-2")),
                Result.success(page(hasNext = false, cursor = null)),
            ),
        )
        val first = fetcher.fetchFirst(config)

        // Act
        fetcher.fetchNext(overrideRequestParams = null, lastResult = first)

        // Assert
        assertThat(requestedCursors).containsExactly(null, "cursor-1", "cursor-2").inOrder()
    }

    @Test
    fun `GIVEN empty pages that keep promising more WHEN fetchFirst THEN the feed ends after the followed ones`() =
        runTest {
            // Arrange
            val fetcher = createFetcher(
                pages = List(size = 5) { index ->
                    Result.success(page(events = emptyList(), hasNext = true, cursor = "cursor-$index"))
                },
            )

            // Act
            val result = fetcher.fetchFirst(config)

            // Assert
            val success = result as BatchFetchResult.Success
            assertThat(success.isEmpty).isTrue()
            assertThat(success.isLast).isTrue()
            assertThat(requestedCursors).containsExactly(null, "cursor-0", "cursor-1", "cursor-2").inOrder()
        }

    @Test
    fun `GIVEN a failure behind an empty page WHEN fetchFirst THEN the failure is reported`() = runTest {
        // Arrange
        val failure = IOException("boom")
        val fetcher = createFetcher(
            pages = listOf(
                Result.success(page(events = emptyList(), hasNext = true, cursor = "cursor-1")),
                Result.failure(failure),
                Result.failure(failure),
            ),
        )

        // Act
        val result = fetcher.fetchFirst(config)

        // Assert
        assertThat((result as BatchFetchResult.Error).throwable).isEqualTo(failure)
        assertThat(requestedCursors).containsExactly(null, "cursor-1", "cursor-1").inOrder()
    }

    @Test
    fun `GIVEN a loaded page WHEN fetchNext THEN the cursor of that page is requested`() = runTest {
        // Arrange
        val fetcher = createFetcher(
            pages = listOf(
                Result.success(page(hasNext = true, cursor = "cursor-1")),
                Result.success(page(hasNext = false, cursor = "cursor-2")),
            ),
        )
        val first = fetcher.fetchFirst(config)

        // Act
        val second = fetcher.fetchNext(overrideRequestParams = null, lastResult = first)

        // Assert
        assertThat(requestedCursors).containsExactly(null, "cursor-1").inOrder()
        assertThat((second as BatchFetchResult.Success).isLast).isTrue()
    }

    @Test
    fun `GIVEN the last page WHEN fetchNext THEN pagination ends without a request`() = runTest {
        // Arrange
        val fetcher = createFetcher(pages = listOf(Result.success(page(hasNext = false, cursor = null))))
        val first = fetcher.fetchFirst(config)

        // Act
        val second = fetcher.fetchNext(overrideRequestParams = null, lastResult = first)

        // Assert
        assertThat((second as BatchFetchResult.Error).throwable)
            .isInstanceOf(EndOfPaginationException::class.java)
        assertThat(requestedCursors).hasSize(1)
    }

    @Test
    fun `GIVEN an empty next page with more behind WHEN fetchNext THEN the page behind it is served`() = runTest {
        // Arrange
        val fetcher = createFetcher(
            pages = listOf(
                Result.success(page(hasNext = true, cursor = "cursor-1")),
                Result.success(page(events = emptyList(), hasNext = true, cursor = "cursor-2")),
                Result.success(page(hasNext = false, cursor = "cursor-3")),
            ),
        )
        val first = fetcher.fetchFirst(config)

        // Act
        val second = fetcher.fetchNext(overrideRequestParams = null, lastResult = first)

        // Assert
        val success = second as BatchFetchResult.Success
        assertThat(success.data.events).containsExactly(event)
        assertThat(success.data.requestCursor).isEqualTo("cursor-2")
        assertThat(success.isLast).isTrue()
        assertThat(requestedCursors).containsExactly(null, "cursor-1", "cursor-2").inOrder()
    }

    @Test
    fun `GIVEN an empty next page WHEN fetchNext THEN it ends the feed instead of failing`() = runTest {
        // Arrange
        val fetcher = createFetcher(
            pages = listOf(
                Result.success(page(hasNext = true, cursor = "cursor-1")),
                Result.success(page(events = emptyList(), hasNext = false, cursor = "cursor-2")),
            ),
        )
        val first = fetcher.fetchFirst(config)

        // Act
        val second = fetcher.fetchNext(overrideRequestParams = null, lastResult = first)

        // Assert
        val success = second as BatchFetchResult.Success
        assertThat(success.isEmpty).isTrue()
        assertThat(success.isLast).isTrue()
    }

    @Test
    fun `GIVEN the first page WHEN fetchFirst THEN it carries no request cursor`() = runTest {
        // Arrange
        val fetcher = createFetcher(pages = listOf(Result.success(page(hasNext = true, cursor = "cursor-1"))))

        // Act
        val result = fetcher.fetchFirst(config)

        // Assert
        assertThat((result as BatchFetchResult.Success).data.requestCursor).isNull()
    }

    @Test
    fun `GIVEN a next page WHEN fetchNext THEN it carries the cursor it was requested with`() = runTest {
        // Arrange
        val fetcher = createFetcher(
            pages = listOf(
                Result.success(page(hasNext = true, cursor = "cursor-1")),
                Result.success(page(hasNext = true, cursor = "cursor-2")),
            ),
        )
        val first = fetcher.fetchFirst(config)

        // Act
        val second = fetcher.fetchNext(overrideRequestParams = null, lastResult = first)

        // Assert
        assertThat((second as BatchFetchResult.Success).data.requestCursor).isEqualTo("cursor-1")
    }

    private fun page(
        events: List<PolymarketEvent> = listOf(event),
        hasNext: Boolean = false,
        cursor: String? = null,
    ) = PolymarketEventsPage(events = events, cursor = cursor, hasNext = hasNext)

    /** Serves [pages] in order, one per call, recording the cursor each call was made with. */
    private fun createFetcher(pages: List<Result<PolymarketEventsPage>>): PolymarketEventsBatchFetcher {
        val remaining = pages.toMutableList()
        return PolymarketEventsBatchFetcher(
            batchSize = 20,
            fetchPage = { _, cursor, _ ->
                requestedCursors += cursor
                remaining.removeAt(0).getOrThrow()
            },
        )
    }
}