package com.tangem.features.send.mcp.model

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.features.send.mcp.createCoin
import com.tangem.features.send.mcp.createToken
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.parallel.ResourceLock
import org.junit.jupiter.api.parallel.Resources
import org.junit.jupiter.params.ParameterizedTest
import java.math.BigDecimal
import java.util.Locale

@ResourceLock(Resources.LOCALE)
internal class SendIntentAmountTextConverterTest {

    private val converter = SendIntentAmountTextConverter()

    private val usdt = createToken()
    private val eth = createCoin()
    private lateinit var originalLocale: Locale

    @BeforeEach
    fun setUpLocale() {
        originalLocale = Locale.getDefault()
        Locale.setDefault(Locale.US)
    }

    @AfterEach
    fun restoreLocale() {
        Locale.setDefault(originalLocale)
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Formatting {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN amount and locale WHEN convert THEN amount is rounded up with the symbol after it`(
            model: FormatModel,
        ) {
            // Arrange
            Locale.setDefault(model.locale)
            val input = SendIntentAmountTextConverter.Input(amount = BigDecimal(model.amount), currency = model.currency)

            // Act
            val actual = converter.convert(input)

            // Assert
            assertThat(actual).isEqualTo(model.expected)
        }

        private fun provideTestModels() = listOf(
            FormatModel(Locale.US, usdt, "42", "42.00\u00a0USDT"),
            FormatModel(Locale.US, usdt, "1234.5", "1,234.50\u00a0USDT"),
            FormatModel(Locale.US, usdt, "1.0000001", "1.000001\u00a0USDT"),
            FormatModel(Locale.US, usdt, "0.0000001", "<0.000001\u00a0USDT"),
            FormatModel(Locale.US, eth, "0.000000011", "0.00000002\u00a0ETH"),
            FormatModel(Locale.US, eth, "0.00000001", "0.00000001\u00a0ETH"),
            FormatModel(Locale.US, eth, "0.000000001", "<0.00000001\u00a0ETH"),
            FormatModel(Locale.GERMANY, eth, "0.000000011", "0,00000002\u00a0ETH"),
            FormatModel(Locale.GERMANY, usdt, "42", "42,00\u00a0USDT"),
        )
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class NonPositiveAmounts {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN non-positive amount WHEN convert THEN there is no text`(amount: BigDecimal) {
            // Arrange
            val input = SendIntentAmountTextConverter.Input(amount = amount, currency = usdt)

            // Act
            val actual = converter.convert(input)

            // Assert
            assertThat(actual).isNull()
        }

        private fun provideTestModels() = listOf("0", "0.000", "-0", "-5").map(::BigDecimal)
    }

    data class FormatModel(
        val locale: Locale,
        val currency: CryptoCurrency,
        val amount: String,
        val expected: String,
    )
}