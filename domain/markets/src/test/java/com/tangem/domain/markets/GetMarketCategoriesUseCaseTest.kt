package com.tangem.domain.markets

import arrow.core.Either
import com.tangem.domain.markets.repositories.MarketsTokenRepository
import com.tangem.test.core.assertEither
import com.tangem.test.core.assertEitherLeft
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal

internal class GetMarketCategoriesUseCaseTest {

    private val marketsTokenRepository: MarketsTokenRepository = mockk()

    private val useCase = GetMarketCategoriesUseCase(marketsTokenRepository = marketsTokenRepository)

    @BeforeEach
    fun setup() {
        clearMocks(marketsTokenRepository)
    }

    @Test
    fun `GIVEN repository returns a page WHEN invoke THEN Right with the page`() = runTest {
        // Arrange
        val page = marketCategoryPage(categories = listOf(marketCategory(id = "layer-2")))
        coEvery { marketsTokenRepository.getMarketCategories(any(), any(), any(), any(), any()) } returns page

        // Act
        val actual = useCase(currency = "usd", offset = 0, limit = 20)

        // Assert
        assertEither(actual = actual, expected = Either.Right(page))
    }

    @Test
    fun `GIVEN repository returns an empty page WHEN invoke THEN Right with the empty page`() = runTest {
        // Arrange — an empty page is a successful answer, not a failure
        val page = marketCategoryPage(categories = emptyList(), total = 0)
        coEvery { marketsTokenRepository.getMarketCategories(any(), any(), any(), any(), any()) } returns page

        // Act
        val actual = useCase(currency = "usd", offset = 0, limit = 20)

        // Assert
        assertEither(actual = actual, expected = Either.Right(page))
    }

    @Test
    fun `GIVEN repository throws WHEN invoke THEN Left with the thrown error`() = runTest {
        // Arrange — the repository is not total, so the use case is the layer that closes the error channel
        val failure = IllegalStateException("categories unavailable")
        coEvery { marketsTokenRepository.getMarketCategories(any(), any(), any(), any(), any()) } throws failure

        // Act
        val actual = useCase(currency = "usd", offset = 0, limit = 20)

        // Assert
        assertEitherLeft(actual = actual, expected = failure)
    }

    @Test
    fun `GIVEN paging arguments WHEN invoke THEN they are forwarded to the repository`() = runTest {
        // Arrange
        coEvery {
            marketsTokenRepository.getMarketCategories(any(), any(), any(), any(), any())
        } returns marketCategoryPage()

        // Act
        useCase(currency = "eur", offset = 20, limit = 50, timestamp = 1754838000000L, featured = true)

        // Assert
        coVerify(exactly = 1) {
            marketsTokenRepository.getMarketCategories(
                currency = "eur",
                offset = 20,
                limit = 50,
                timestamp = 1754838000000L,
                featured = true,
            )
        }
    }

    @Test
    fun `GIVEN no timestamp and no featured filter WHEN invoke THEN nulls are forwarded to the repository`() = runTest {
        // Arrange — the first page is fetched without a snapshot token
        coEvery {
            marketsTokenRepository.getMarketCategories(any(), any(), any(), any(), any())
        } returns marketCategoryPage()

        // Act
        useCase(currency = "usd", offset = 0, limit = 20)

        // Assert
        coVerify(exactly = 1) {
            marketsTokenRepository.getMarketCategories(
                currency = "usd",
                offset = 0,
                limit = 20,
                timestamp = null,
                featured = null,
            )
        }
    }

    private fun marketCategoryPage(
        categories: List<MarketCategory> = listOf(marketCategory()),
        total: Int = 42,
        offset: Int = 0,
        limit: Int = 20,
    ) = MarketCategoryPage(
        categories = categories,
        total = total,
        offset = offset,
        limit = limit,
    )

    private fun marketCategory(
        id: String = "layer-2",
        name: String = "Layer 2",
        description: String? = "Scaling solutions",
        marketCap: BigDecimal? = BigDecimal("1000"),
        marketCapChangePercent24h: BigDecimal? = BigDecimal("-0.25"),
        volume24h: BigDecimal? = BigDecimal("500"),
        isFeatured: Boolean = true,
        tokensCount: Int = 7,
    ) = MarketCategory(
        id = id,
        name = name,
        description = description,
        marketCap = marketCap,
        marketCapChangePercent24h = marketCapChangePercent24h,
        volume24h = volume24h,
        isFeatured = isFeatured,
        tokensCount = tokensCount,
    )
}