package com.tangem.domain.quotes

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.models.currency.CryptoCurrency
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal

internal class IsHighNetworkFeeUseCaseTest {

    private val getCurrencyUSDQuoteUseCase: GetCurrencyUSDQuoteUseCase = mockk()
    private val feeCurrency: CryptoCurrency = mockk()
    private val rawCurrencyId = CryptoCurrency.RawID("bitcoin")

    private val useCase = IsHighNetworkFeeUseCase(getCurrencyUSDQuoteUseCase)

    @BeforeEach
    fun setup() {
        clearMocks(getCurrencyUSDQuoteUseCase, feeCurrency)
        every { feeCurrency.id.rawCurrencyId } returns rawCurrencyId
    }

    @Test
    fun `GIVEN fee usd value above threshold WHEN invoke THEN returns true`() = runTest {
        // Arrange — 0.5 coin * 25 USD = 12.5 USD > 10
        coEvery { getCurrencyUSDQuoteUseCase(rawCurrencyId) } returns BigDecimal("25")

        // Act
        val result = useCase(feeCurrency, BigDecimal("0.5"))

        // Assert
        assertThat(result).isTrue()
    }

    @Test
    fun `GIVEN fee usd value below threshold WHEN invoke THEN returns false`() = runTest {
        // Arrange — 0.2 coin * 25 USD = 5 USD < 10
        coEvery { getCurrencyUSDQuoteUseCase(rawCurrencyId) } returns BigDecimal("25")

        // Act
        val result = useCase(feeCurrency, BigDecimal("0.2"))

        // Assert
        assertThat(result).isFalse()
    }

    @Test
    fun `GIVEN fee usd value equal to threshold WHEN invoke THEN returns false`() = runTest {
        // Arrange — 0.4 coin * 25 USD = 10 USD, not strictly above threshold
        coEvery { getCurrencyUSDQuoteUseCase(rawCurrencyId) } returns BigDecimal("25")

        // Act
        val result = useCase(feeCurrency, BigDecimal("0.4"))

        // Assert
        assertThat(result).isFalse()
    }

    @Test
    fun `GIVEN no usd quote WHEN invoke THEN returns false`() = runTest {
        // Arrange
        coEvery { getCurrencyUSDQuoteUseCase(rawCurrencyId) } returns null

        // Act
        val result = useCase(feeCurrency, BigDecimal("100"))

        // Assert
        assertThat(result).isFalse()
    }

    @Test
    fun `GIVEN no raw currency id WHEN invoke THEN returns false`() = runTest {
        // Arrange
        every { feeCurrency.id.rawCurrencyId } returns null

        // Act
        val result = useCase(feeCurrency, BigDecimal("100"))

        // Assert
        assertThat(result).isFalse()
    }
}