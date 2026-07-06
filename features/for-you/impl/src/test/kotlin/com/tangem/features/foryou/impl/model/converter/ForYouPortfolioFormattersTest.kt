package com.tangem.features.foryou.impl.model.converter

import com.google.common.truth.Truth.assertThat
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.format.bigdecimal.fiat
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.math.BigDecimal

internal class ForYouPortfolioFormattersTest {

    private val appCurrency: AppCurrency = AppCurrency.Default

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
    inner class ToForYouFiatText {

        @Test
        fun `GIVEN a fiat amount WHEN toForYouFiatText THEN delegates to fiat formatting`() {
            // Arrange
            val amount = BigDecimal("1234.5")

            // Act
            val result = amount.toForYouFiatText(appCurrency)

            // Assert
            val expected = stringReference(
                amount.format { fiat(fiatCurrencyCode = appCurrency.code, fiatCurrencySymbol = appCurrency.symbol) },
            )
            assertThat(result).isEqualTo(expected)
        }

        @Test
        fun `GIVEN null amount WHEN toForYouFiatText THEN renders dash text`() {
            // Arrange
            val amount: BigDecimal? = null

            // Act
            val result = amount.toForYouFiatText(appCurrency)

            // Assert
            val expected = stringReference(
                amount.format { fiat(fiatCurrencyCode = appCurrency.code, fiatCurrencySymbol = appCurrency.symbol) },
            )
            assertThat(result).isEqualTo(expected)
        }
    }

    @Nested
    inner class ToForYouPercentText {

        @Test
        fun `GIVEN null amount WHEN toForYouPercentText THEN returns EMPTY`() {
            // Arrange
            val amount: BigDecimal? = null

            // Act
            val result = amount.toForYouPercentText(BigDecimal("100"))

            // Assert
            assertThat(result).isEqualTo(TextReference.EMPTY)
        }

        @Test
        fun `GIVEN zero total WHEN toForYouPercentText THEN returns EMPTY`() {
            // Arrange
            val amount = BigDecimal("10")

            // Act
            val result = amount.toForYouPercentText(BigDecimal.ZERO)

            // Assert
            assertThat(result).isEqualTo(TextReference.EMPTY)
        }

        @Test
        fun `GIVEN zero amount WHEN toForYouPercentText THEN returns EMPTY`() {
            // Arrange
            val amount = BigDecimal.ZERO

            // Act
            val result = amount.toForYouPercentText(BigDecimal("100"))

            // Assert
            assertThat(result).isEqualTo(TextReference.EMPTY)
        }

        @Test
        fun `GIVEN non-zero amount and total WHEN toForYouPercentText THEN returns rounded percent share`() {
            // Arrange
            val amount = BigDecimal("25.00")
            val total = BigDecimal("100")

            // Act
            val result = amount.toForYouPercentText(total)

            // Assert — 25.00 / 100 = 0.25 -> 25.00%
            assertThat(result).isEqualTo(stringReference("25.00%"))
        }

        @Test
        fun `GIVEN a share requiring rounding WHEN toForYouPercentText THEN applies HALF_UP rounding`() {
            // Arrange — 1.0000 / 3 = 0.3333... -> rounds to 33.33%
            val amount = BigDecimal("1.0000")
            val total = BigDecimal("3")

            // Act
            val result = amount.toForYouPercentText(total)

            // Assert
            assertThat(result).isEqualTo(stringReference("33.33%"))
        }
    }
}