package com.tangem.data.collectibles.pagination

import com.google.common.truth.Truth.assertThat
import com.tangem.data.collectibles.MachineFactory
import com.tangem.domain.collectibles.models.CollectibleGachaMachinesBatch
import com.tangem.domain.collectibles.models.CollectibleGachaMachinesListConfig
import com.tangem.domain.collectibles.models.CollectibleGachaMachinesPage
import com.tangem.pagination.BatchFetchResult
import com.tangem.pagination.exception.EndOfPaginationException
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

internal class CollectibleGachaMachinesBatchFetcherTest {

    private val config = MachineFactory.createListConfig(categoryId = "pokemons")

    @Test
    fun `GIVEN page has next WHEN fetchFirst THEN result is not last`() = runTest {
        // Arrange
        val fetcher = fetcherReturning(page(cursor = "c1", hasNext = true))

        // Act
        val result = fetcher.fetchFirst(config)

        // Assert
        assertThat(result).isInstanceOf(BatchFetchResult.Success::class.java)
        assertThat((result as BatchFetchResult.Success).last).isFalse()
    }

    @Test
    fun `GIVEN hasNext is false WHEN fetchFirst THEN result is last`() = runTest {
        // Arrange
        val fetcher = fetcherReturning(page(cursor = "c1", hasNext = false))

        // Act
        val result = fetcher.fetchFirst(config)

        // Assert
        assertThat((result as BatchFetchResult.Success).last).isTrue()
    }

    @Test
    fun `GIVEN hasNext is true but cursor is null WHEN fetchFirst THEN result is last`() = runTest {
        // Arrange
        val fetcher = fetcherReturning(page(cursor = null, hasNext = true))

        // Act
        val result = fetcher.fetchFirst(config)

        // Assert
        assertThat((result as BatchFetchResult.Success).last).isTrue()
    }

    @Test
    fun `GIVEN first page WHEN fetchNext THEN previous cursor is sent`() = runTest {
        // Arrange
        val requestedCursors = mutableListOf<String?>()
        val fetcher = CollectibleGachaMachinesBatchFetcher(batchSize = BATCH_SIZE) { _, cursor, _ ->
            requestedCursors += cursor
            page(cursor = "c2", hasNext = true)
        }

        // Act
        val first = fetcher.fetchFirst(config)
        fetcher.fetchNext(overrideRequestParams = null, lastResult = first)

        // Assert
        assertThat(requestedCursors).containsExactly(null, "c2").inOrder()
    }

    @Test
    fun `GIVEN exhausted pagination WHEN fetchNext THEN end of pagination error`() = runTest {
        // Arrange
        val fetcher = fetcherReturning(page(cursor = null, hasNext = false))

        // Act
        val first = fetcher.fetchFirst(config)
        val next = fetcher.fetchNext(overrideRequestParams = null, lastResult = first)

        // Assert
        assertThat(next).isInstanceOf(BatchFetchResult.Error::class.java)
        assertThat((next as BatchFetchResult.Error).throwable)
            .isInstanceOf(EndOfPaginationException::class.java)
    }

    @Test
    fun `GIVEN a successful page without a cursor WHEN fetchNext THEN end of pagination error`() = runTest {
        // Arrange — hasNext lies, so the result is not marked last, but there is nothing to continue from
        val fetcher = CollectibleGachaMachinesBatchFetcher(batchSize = BATCH_SIZE) { _, _, _ ->
            page(cursor = null, hasNext = true)
        }
        val first = BatchFetchResult.Success(
            data = CollectibleGachaMachinesBatch(collectibleGachaMachines = emptyList(), requestCursor = null),
            empty = false,
            last = false,
        )
        fetcher.fetchFirst(config)

        // Act
        val next = fetcher.fetchNext(overrideRequestParams = null, lastResult = first)

        // Assert
        assertThat((next as BatchFetchResult.Error).throwable).isInstanceOf(EndOfPaginationException::class.java)
    }

    @Test
    fun `GIVEN fetch throws WHEN fetchFirst THEN error result carries the cause`() = runTest {
        // Arrange
        val cause = IllegalStateException("boom")
        val fetcher = CollectibleGachaMachinesBatchFetcher(batchSize = BATCH_SIZE) { _, _, _ -> throw cause }

        // Act
        val result = fetcher.fetchFirst(config)

        // Assert
        assertThat((result as BatchFetchResult.Error).throwable).isEqualTo(cause)
    }

    @Test
    fun `GIVEN empty page WHEN fetchFirst THEN batch is marked empty`() = runTest {
        // Arrange
        val emptyPage = CollectibleGachaMachinesPage(
            collectibleGachaMachines = emptyList(),
            cursor = null,
            hasNext = false,
        )
        val fetcher = fetcherReturning(emptyPage)

        // Act
        val result = fetcher.fetchFirst(config)

        // Assert
        val success = result as BatchFetchResult.Success<CollectibleGachaMachinesBatch>
        assertThat(success.empty).isTrue()
        assertThat(success.data.collectibleGachaMachines).isEmpty()
    }

    @Test
    fun `GIVEN no fetchFirst WHEN fetchNext THEN it fails with an illegal state`() = runTest {
        // Arrange
        val fetcher = fetcherReturning(page(cursor = "c1", hasNext = true))
        val lastResult = BatchFetchResult.Error(IllegalStateException("previous failure"))

        // Act
        val thrown = runCatching {
            fetcher.fetchNext(overrideRequestParams = null, lastResult = lastResult)
        }.exceptionOrNull()

        // Assert
        assertThat(thrown).isInstanceOf(IllegalStateException::class.java)
        assertThat(thrown).hasMessageThat().isEqualTo("fetchFirst() must be called before fetchNext()")
    }

    @Test
    fun `GIVEN a failed page WHEN fetchNext THEN it retries with the cursor the page was requested with`() = runTest {
        // Arrange
        val requestedCursors = mutableListOf<String?>()
        var failNext = true
        val fetcher = CollectibleGachaMachinesBatchFetcher(batchSize = BATCH_SIZE) { _, cursor, _ ->
            requestedCursors += cursor
            if (cursor == "c1" && failNext) {
                failNext = false
                error("page failed")
            }
            page(cursor = "c1", hasNext = true)
        }

        // Act
        val first = fetcher.fetchFirst(config)
        val failed = fetcher.fetchNext(overrideRequestParams = null, lastResult = first)
        val retried = fetcher.fetchNext(overrideRequestParams = null, lastResult = failed)

        // Assert
        assertThat(failed).isInstanceOf(BatchFetchResult.Error::class.java)
        assertThat(retried).isInstanceOf(BatchFetchResult.Success::class.java)
        assertThat(requestedCursors).containsExactly(null, "c1", "c1").inOrder()
    }

    @Test
    fun `GIVEN a paged fetcher WHEN fetchFirst again THEN the held cursor is reset`() = runTest {
        // Arrange
        val requestedCursors = mutableListOf<String?>()
        val fetcher = CollectibleGachaMachinesBatchFetcher(batchSize = BATCH_SIZE) { _, cursor, _ ->
            requestedCursors += cursor
            page(cursor = "c1", hasNext = true)
        }

        // Act
        val first = fetcher.fetchFirst(config)
        fetcher.fetchNext(overrideRequestParams = null, lastResult = first)
        fetcher.fetchFirst(config)

        // Assert
        assertThat(requestedCursors).containsExactly(null, "c1", null).inOrder()
    }

    @Test
    fun `GIVEN override params WHEN fetchNext THEN they replace the config for later pages`() = runTest {
        // Arrange
        val requestedConfigs = mutableListOf<CollectibleGachaMachinesListConfig>()
        val fetcher = CollectibleGachaMachinesBatchFetcher(batchSize = BATCH_SIZE) { requestConfig, _, _ ->
            requestedConfigs += requestConfig
            page(cursor = "c1", hasNext = true)
        }
        val override = MachineFactory.createListConfig(search = "pikachu")

        // Act
        val first = fetcher.fetchFirst(config)
        val second = fetcher.fetchNext(overrideRequestParams = override, lastResult = first)
        fetcher.fetchNext(overrideRequestParams = null, lastResult = second)

        // Assert
        assertThat(requestedConfigs).containsExactly(config, override, override).inOrder()
    }

    @Test
    fun `GIVEN a batch size WHEN fetching THEN it is forwarded as the page limit`() = runTest {
        // Arrange
        val requestedLimits = mutableListOf<Int>()
        val fetcher = CollectibleGachaMachinesBatchFetcher(batchSize = 7) { _, _, limit ->
            requestedLimits += limit
            page(cursor = "c1", hasNext = true)
        }

        // Act
        val first = fetcher.fetchFirst(config)
        fetcher.fetchNext(overrideRequestParams = null, lastResult = first)

        // Assert
        assertThat(requestedLimits).containsExactly(7, 7).inOrder()
    }

    @Test
    fun `GIVEN paged results WHEN fetching THEN each batch carries the cursor it was requested with`() = runTest {
        // Arrange
        val fetcher = CollectibleGachaMachinesBatchFetcher(batchSize = BATCH_SIZE) { _, cursor, _ ->
            page(cursor = "${cursor.orEmpty()}next", hasNext = true)
        }

        // Act
        val first = fetcher.fetchFirst(config) as BatchFetchResult.Success<CollectibleGachaMachinesBatch>
        val second = fetcher.fetchNext(
            overrideRequestParams = null,
            lastResult = first,
        ) as BatchFetchResult.Success<CollectibleGachaMachinesBatch>

        // Assert
        assertThat(first.data.requestCursor).isNull()
        assertThat(second.data.requestCursor).isEqualTo("next")
    }

    private fun fetcherReturning(page: CollectibleGachaMachinesPage) =
        CollectibleGachaMachinesBatchFetcher(batchSize = BATCH_SIZE) { _, _, _ -> page }

    private fun page(cursor: String?, hasNext: Boolean) = CollectibleGachaMachinesPage(
        collectibleGachaMachines = listOf(MachineFactory.create()),
        cursor = cursor,
        hasNext = hasNext,
    )

    private companion object {
        const val BATCH_SIZE = 20
    }
}