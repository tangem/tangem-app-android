package com.tangem.features.feed.crypto.categories

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.domain.markets.GetMarketCategoriesUseCase
import com.tangem.domain.markets.MarketCategory
import com.tangem.domain.markets.MarketCategoryPage
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

/**
 * The listing is walked because the endpoint has no by-id lookup, so what these tests pin down is the
 * cost of that walk: it stops at the match, at the end of the listing, and at the page cap.
 */
internal class MarketCategoryResolverTest {

    private val getMarketCategoriesUseCase: GetMarketCategoriesUseCase = mockk()

    private val requestedOffsets = mutableListOf<Int>()
    private val requestedCurrencies = mutableListOf<String>()

    private val resolver = MarketCategoryResolver(getMarketCategoriesUseCase = getMarketCategoriesUseCase)

    @Test
    fun `GIVEN the id is on the first page WHEN resolved THEN it is found in one request`() = runTest {
        // Arrange
        stubPages(pageOf(categories = listOf(category(id = "layer-1", name = "Layer 1")), total = 1))

        // Act
        val entry = resolver.resolve(categoryId = "layer-1", currency = "USD")

        // Assert
        assertThat(entry).isEqualTo(MarketCategorySelection.Entry(id = "layer-1", name = "Layer 1"))
        assertThat(requestedOffsets).containsExactly(0)
        assertThat(requestedCurrencies).containsExactly("USD")
    }

    @Test
    fun `GIVEN the id is on a later page WHEN resolved THEN the listing is paged until it is found`() = runTest {
        // Arrange
        stubPages(
            pageOf(categories = listOf(category(id = "meme", name = "Meme")), total = 2),
            pageOf(categories = listOf(category(id = "layer-1", name = "Layer 1")), total = 2),
        )

        // Act
        val entry = resolver.resolve(categoryId = "layer-1", currency = "USD")

        // Assert — the second request starts where the first page ended
        assertThat(entry?.name).isEqualTo("Layer 1")
        assertThat(requestedOffsets).containsExactly(0, 1).inOrder()
    }

    @Test
    fun `GIVEN the listing is exhausted WHEN resolved THEN it stops instead of paging past the end`() = runTest {
        // Arrange
        stubPages(pageOf(categories = listOf(category(id = "meme", name = "Meme")), total = 1))

        // Act
        val entry = resolver.resolve(categoryId = "layer-1", currency = "USD")

        // Assert
        assertThat(entry).isNull()
        assertThat(requestedOffsets).containsExactly(0)
    }

    @Test
    fun `GIVEN a listing longer than the page cap WHEN resolved THEN the walk is bounded`() = runTest {
        // Arrange — a total that never runs out, so only the cap can stop the walk
        val endless = pageOf(categories = listOf(category(id = "meme", name = "Meme")), total = Int.MAX_VALUE)
        stub { endless.right() }

        // Act
        val entry = resolver.resolve(categoryId = "layer-1", currency = "USD")

        // Assert
        assertThat(entry).isNull()
        assertThat(requestedOffsets).containsExactly(0, 1, 2, 3, 4).inOrder()
    }

    @Test
    fun `GIVEN the request fails WHEN resolved THEN nothing is resolved and the tab stays unfiltered`() = runTest {
        // Arrange
        stub { IllegalStateException("no network").left() }

        // Act
        val entry = resolver.resolve(categoryId = "layer-1", currency = "USD")

        // Assert
        assertThat(entry).isNull()
    }

    @Test
    fun `GIVEN the listing never answers WHEN resolved THEN the walk gives up instead of holding the tab`() =
        runTest {
            // Arrange
            coEvery {
                getMarketCategoriesUseCase(
                    currency = any(),
                    offset = any(),
                    limit = any(),
                    timestamp = any(),
                    featured = any(),
                )
            } coAnswers { CompletableDeferred<Nothing>().await() }

            // Act
            val entry = resolver.resolve(categoryId = "layer-1", currency = "USD")

            // Assert
            assertThat(entry).isNull()
        }

    private fun stubPages(vararg pages: MarketCategoryPage) {
        var index = 0
        stub { pages[index++.coerceAtMost(pages.lastIndex)].right() }
    }

    private fun stub(answer: () -> Either<Throwable, MarketCategoryPage>) {
        coEvery {
            getMarketCategoriesUseCase(
                currency = capture(requestedCurrencies),
                offset = capture(requestedOffsets),
                limit = any(),
                timestamp = any(),
                featured = any(),
            )
        } coAnswers { answer() }
    }

    private fun pageOf(categories: List<MarketCategory>, total: Int) = MarketCategoryPage(
        categories = categories,
        total = total,
        offset = 0,
        limit = 100,
    )

    private fun category(id: String, name: String) = MarketCategory(
        id = id,
        name = name,
        description = null,
        marketCap = null,
        marketCapChangePercent24h = null,
        volume24h = null,
        isFeatured = false,
        tokensCount = 1,
    )
}