package com.tangem.domain.quotes

import com.google.common.truth.Truth.assertThat
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal

internal class GetFiatUSDRateUseCaseTest {

    private val quotesRepository: QuotesRepository = mockk()

    private val useCase = GetFiatUSDRateUseCase(quotesRepository)

    @BeforeEach
    fun resetMocks() {
        clearMocks(quotesRepository)
    }

    @Test
    fun `GIVEN usd code in any case WHEN invoke THEN returns one without fetching`() = runTest {
        // Act
        val upper = useCase("USD")
        val lower = useCase("usd")

        // Assert
        assertThat(upper).isEqualTo(BigDecimal.ONE)
        assertThat(lower).isEqualTo(BigDecimal.ONE)
        coVerify(exactly = 0) { quotesRepository.getFiatUSDRate(any()) }
    }

    @Test
    fun `GIVEN non usd code WHEN invoke THEN returns repository rate`() = runTest {
        // Arrange
        coEvery { quotesRepository.getFiatUSDRate("EUR") } returns BigDecimal("1.1")

        // Act
        val actual = useCase("EUR")

        // Assert
        assertThat(actual).isEqualTo(BigDecimal("1.1"))
    }

    @Test
    fun `GIVEN repository has no rate WHEN invoke THEN returns null`() = runTest {
        // Arrange
        coEvery { quotesRepository.getFiatUSDRate("EUR") } returns null

        // Act
        val actual = useCase("EUR")

        // Assert
        assertThat(actual).isNull()
    }
}