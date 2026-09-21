package com.tangem.data.collectibles.pagination

import com.google.common.truth.Truth.assertThat
import com.tangem.data.collectibles.MachineFactory
import com.tangem.domain.collectibles.models.CollectibleGachaMachineNft
import com.tangem.domain.collectibles.models.CollectibleGachaMachineNftsListConfig
import com.tangem.domain.collectibles.models.CollectibleGachaMachineNftsPage
import com.tangem.pagination.BatchFetchResult
import com.tangem.pagination.exception.EndOfPaginationException
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

internal class CollectibleGachaMachineNftsBatchFetcherTest {

    private val requestedPages = mutableListOf<Int>()

    private fun createFetcher(
        batchSize: Int = 50,
        pages: Map<Int, CollectibleGachaMachineNftsPage> = emptyMap(),
        onFetch: (Int) -> Unit = {},
    ) = CollectibleGachaMachineNftsBatchFetcher(
        batchSize = batchSize,
        fetchPage = { _, page, _ ->
            requestedPages += page
            onFetch(page)
            pages[page] ?: error("no page $page")
        },
    )

    private fun createPage(
        nfts: List<CollectibleGachaMachineNft> = listOf(MachineFactory.createNft()),
        page: Int = 1,
        hasMore: Boolean = false,
    ) = CollectibleGachaMachineNftsPage(nfts = nfts, page = page, hasMore = hasMore)

    private val config = CollectibleGachaMachineNftsListConfig(externalId = "pokemon_50")

    @Test
    fun `GIVEN more pages WHEN fetchFirst THEN result is not the last one`() = runTest {
        // Arrange
        val fetcher = createFetcher(pages = mapOf(1 to createPage(hasMore = true)))

        // Act
        val actual = fetcher.fetchFirst(config)

        // Assert
        assertThat(actual).isInstanceOf(BatchFetchResult.Success::class.java)
        assertThat((actual as BatchFetchResult.Success).last).isFalse()
        assertThat(requestedPages).containsExactly(1)
    }

    @Test
    fun `GIVEN no more pages WHEN fetchFirst THEN result is the last one`() = runTest {
        // Arrange
        val fetcher = createFetcher(pages = mapOf(1 to createPage(hasMore = false)))

        // Act
        val actual = fetcher.fetchFirst(config)

        // Assert
        assertThat((actual as BatchFetchResult.Success).last).isTrue()
    }

    @Test
    fun `GIVEN an empty page WHEN fetchFirst THEN result is empty`() = runTest {
        // Arrange
        val fetcher = createFetcher(pages = mapOf(1 to createPage(nfts = emptyList())))

        // Act
        val actual = fetcher.fetchFirst(config)

        // Assert
        assertThat((actual as BatchFetchResult.Success).empty).isTrue()
    }

    @Test
    fun `GIVEN a loaded page WHEN fetchNext THEN the following page is requested`() = runTest {
        // Arrange
        val fetcher = createFetcher(
            pages = mapOf(
                1 to createPage(hasMore = true),
                2 to createPage(page = 2, hasMore = false),
            ),
        )
        val first = fetcher.fetchFirst(config)

        // Act
        val actual = fetcher.fetchNext(overrideRequestParams = null, lastResult = first)

        // Assert
        assertThat((actual as BatchFetchResult.Success).last).isTrue()
        assertThat(requestedPages).containsExactly(1, 2).inOrder()
    }

    @Test
    fun `GIVEN the backend repeats a page number WHEN fetchNext THEN the pagination still advances`() = runTest {
        // Arrange
        val fetcher = createFetcher(
            pages = mapOf(
                1 to createPage(page = 1, hasMore = true),
                // the echoed page is wrong; the fetcher must not ask for page 1 again
                2 to createPage(page = 1, hasMore = true),
                3 to createPage(page = 1, hasMore = false),
            ),
        )
        val first = fetcher.fetchFirst(config)
        val second = fetcher.fetchNext(overrideRequestParams = null, lastResult = first)

        // Act
        fetcher.fetchNext(overrideRequestParams = null, lastResult = second)

        // Assert
        assertThat(requestedPages).containsExactly(1, 2, 3).inOrder()
    }

    @Test
    fun `GIVEN the last page WHEN fetchNext THEN pagination ends without a request`() = runTest {
        // Arrange
        val fetcher = createFetcher(pages = mapOf(1 to createPage(hasMore = false)))
        val first = fetcher.fetchFirst(config)
        requestedPages.clear()

        // Act
        val actual = fetcher.fetchNext(overrideRequestParams = null, lastResult = first)

        // Assert
        assertThat((actual as BatchFetchResult.Error).throwable).isInstanceOf(EndOfPaginationException::class.java)
        assertThat(requestedPages).isEmpty()
    }

    @Test
    fun `GIVEN a page that failed WHEN fetchNext THEN the same page is requested again`() = runTest {
        // Arrange
        var shouldFail = true
        val fetcher = createFetcher(
            pages = mapOf(
                1 to createPage(hasMore = true),
                2 to createPage(page = 2, hasMore = false),
            ),
            onFetch = { page ->
                if (page == 2 && shouldFail) {
                    shouldFail = false
                    error("boom")
                }
            },
        )
        val first = fetcher.fetchFirst(config)
        val failed = fetcher.fetchNext(overrideRequestParams = null, lastResult = first)

        // Act
        val retried = fetcher.fetchNext(overrideRequestParams = null, lastResult = failed)

        // Assert
        assertThat(failed).isInstanceOf(BatchFetchResult.Error::class.java)
        assertThat(retried).isInstanceOf(BatchFetchResult.Success::class.java)
        assertThat(requestedPages).containsExactly(1, 2, 2).inOrder()
    }
}