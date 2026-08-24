package com.tangem.domain.markets

import arrow.core.Either
import com.tangem.domain.markets.repositories.MarketsTokenRepository
import com.tangem.test.core.assertEither
import com.tangem.test.core.assertEitherLeft
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class GetCoinCategoriesUseCaseTest {

    private val marketsTokenRepository: MarketsTokenRepository = mockk()

    private val useCase = GetCoinCategoriesUseCase(marketsTokenRepository = marketsTokenRepository)

    @BeforeEach
    fun setup() {
        clearMocks(marketsTokenRepository)
    }

    @Test
    fun `GIVEN repository returns categories WHEN invoke THEN Right with the categories`() = runTest {
        // Arrange
        val categories = listOf(coinCategory(id = "42", code = "stocks"))
        coEvery { marketsTokenRepository.getCoinCategories() } returns categories

        // Act
        val actual = useCase()

        // Assert
        assertEither(actual = actual, expected = Either.Right(categories))
    }

    @Test
    fun `GIVEN repository returns empty list WHEN invoke THEN Right with an empty list`() = runTest {
        // Arrange — no categories is a successful answer, not a failure
        coEvery { marketsTokenRepository.getCoinCategories() } returns emptyList()

        // Act
        val actual = useCase()

        // Assert
        assertEither(actual = actual, expected = Either.Right(emptyList()))
    }

    @Test
    fun `GIVEN repository throws WHEN invoke THEN Left with the thrown error`() = runTest {
        // Arrange — the repository is not total, so the use case is the layer that closes the error channel
        val failure = IllegalStateException("categories unavailable")
        coEvery { marketsTokenRepository.getCoinCategories() } throws failure

        // Act
        val actual = useCase()

        // Assert
        assertEitherLeft(actual = actual, expected = failure)
    }

    private fun coinCategory(
        id: String = "42",
        code: String = "stocks",
        displayName: String = "Stocks",
        displayOrder: Int = 1,
        tokensCount: Int = 5,
        isRestricted: Boolean = false,
        isVisible: Boolean = true,
        sectors: List<CoinCategory.Sector> = emptyList(),
    ) = CoinCategory(
        id = id,
        code = code,
        displayName = displayName,
        displayOrder = displayOrder,
        tokensCount = tokensCount,
        isRestricted = isRestricted,
        isVisible = isVisible,
        sectors = sectors,
    )
}