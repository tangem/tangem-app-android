package com.tangem.features.foryou.impl.model.converter

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.math.BigDecimal

internal class ForYouPortfolioFormattersTest {

    @Nested
    inner class ForYouGroupKey {

        @Test
        fun `GIVEN standard currency with raw id WHEN forYouGroupKey THEN returns rawCurrencyId value`() {
            // Arrange
            val id: CryptoCurrency.ID = mockk {
                every { rawCurrencyId } returns CryptoCurrency.RawID("bitcoin")
                every { value } returns "coin-id-value"
            }
            val currency: CryptoCurrency = mockk { every { this@mockk.id } returns id }
            val status = createStatus(currency)

            // Act
            val result = status.forYouGroupKey()

            // Assert
            assertThat(result).isEqualTo("bitcoin")
        }

        @Test
        fun `GIVEN custom token with no raw id WHEN forYouGroupKey THEN falls back to currency id value`() {
            // Arrange
            val id: CryptoCurrency.ID = mockk {
                every { rawCurrencyId } returns null
                every { value } returns "custom-currency-id"
            }
            val currency: CryptoCurrency = mockk { every { this@mockk.id } returns id }
            val status = createStatus(currency)

            // Act
            val result = status.forYouGroupKey()

            // Assert
            assertThat(result).isEqualTo("custom-currency-id")
        }

        private fun createStatus(currency: CryptoCurrency): CryptoCurrencyStatus = CryptoCurrencyStatus(
            currency = currency,
            value = CryptoCurrencyStatus.Loading,
        )
    }

    @Nested
    inner class ToForYouPercent {

        @Test
        fun `GIVEN null amount WHEN toForYouPercent THEN returns null`() {
            // Arrange
            val amount: BigDecimal? = null

            // Act
            val result = amount.toForYouPercent(BigDecimal("100"))

            // Assert
            assertThat(result).isNull()
        }

        @Test
        fun `GIVEN zero total WHEN toForYouPercent THEN returns null`() {
            // Arrange
            val amount = BigDecimal("10")

            // Act
            val result = amount.toForYouPercent(BigDecimal.ZERO)

            // Assert
            assertThat(result).isNull()
        }

        @Test
        fun `GIVEN zero amount WHEN toForYouPercent THEN returns null`() {
            // Arrange
            val amount = BigDecimal.ZERO

            // Act
            val result = amount.toForYouPercent(BigDecimal("100"))

            // Assert
            assertThat(result).isNull()
        }

        @Test
        fun `GIVEN non-zero amount and total WHEN toForYouPercent THEN returns the share as a ratio`() {
            // Arrange — 50.00 / 200 = 0.25 (ratio, scaled to the amount's scale)
            val amount = BigDecimal("50.00")

            // Act
            val result = amount.toForYouPercent(BigDecimal("200"))

            // Assert
            assertThat(result).isEqualTo(BigDecimal("0.25"))
        }

        @Test
        fun `GIVEN a share requiring rounding WHEN toForYouPercent THEN applies HALF_UP rounding`() {
            // Arrange — 1.0000 / 3 = 0.3333... rounds HALF_UP to the amount's scale (4)
            val amount = BigDecimal("1.0000")

            // Act
            val result = amount.toForYouPercent(BigDecimal("3"))

            // Assert
            assertThat(result).isEqualTo(BigDecimal("0.3333"))
        }
    }
}