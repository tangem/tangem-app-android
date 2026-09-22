package com.tangem.core.ui.components.fields

import com.google.common.truth.Truth.assertThat
import com.tangem.core.ui.utils.createAmountDecimalFormat
import com.tangem.core.ui.utils.parseBigDecimal
import com.tangem.core.ui.utils.parseToBigDecimal
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.parallel.ResourceLock
import org.junit.jupiter.api.parallel.Resources
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.math.BigDecimal
import java.util.Locale

@ResourceLock(Resources.LOCALE)
internal class AmountTextFieldTest {

    private lateinit var defaultLocale: Locale

    @BeforeEach
    fun rememberLocale() {
        defaultLocale = Locale.getDefault()
    }

    @AfterEach
    fun restoreLocale() {
        Locale.setDefault(defaultLocale)
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class PrepareEnter {

        @ParameterizedTest
        @MethodSource("provideTestModels")
        fun prepareEnter(model: EnterModel) {
            // Arrange
            Locale.setDefault(model.locale)

            // Act
            val fieldValue = prepareEnter(
                oldValue = model.oldValue,
                newValue = model.typed,
                decimalFormat = createAmountDecimalFormat(),
                decimals = DECIMALS,
            )
            val amount = fieldValue.parseToBigDecimal(DECIMALS)

            // Assert
            assertThat(fieldValue).isEqualTo(model.expectedFieldValue)
            assertThat(amount).isEqualTo(model.expectedAmount)
        }

        private fun provideTestModels() = listOf(
            EnterModel(
                locale = Locale.US,
                typed = "0.00000712",
                expectedFieldValue = "0.00000712",
                expectedAmount = BigDecimal("0.00000712"),
            ),
            // [REDACTED_TASK_KEY]: a keyboard point on a comma locale is a decimal separator, not a grouping one
            EnterModel(
                locale = DANISH,
                typed = "0.00000712",
                expectedFieldValue = "0,00000712",
                expectedAmount = BigDecimal("0.00000712"),
            ),
            EnterModel(
                locale = DANISH,
                typed = "0,00000712",
                expectedFieldValue = "0,00000712",
                expectedAmount = BigDecimal("0.00000712"),
            ),
            EnterModel(
                locale = Locale.GERMANY,
                typed = "1234.5",
                expectedFieldValue = "1234,5",
                expectedAmount = BigDecimal("1234.5"),
            ),
            EnterModel(
                locale = Locale.FRANCE,
                typed = "0,5",
                expectedFieldValue = "0,5",
                expectedAmount = BigDecimal("0.5"),
            ),
            EnterModel(
                locale = DANISH,
                typed = "0.",
                expectedFieldValue = "0,",
                expectedAmount = BigDecimal.ZERO,
            ),
            EnterModel(
                locale = DANISH,
                typed = "01",
                expectedFieldValue = "1",
                expectedAmount = BigDecimal.ONE,
            ),
            EnterModel(
                locale = DANISH,
                oldValue = "1,5",
                typed = "1,5,",
                expectedFieldValue = "1,5",
                expectedAmount = BigDecimal("1.5"),
            ),
        )
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class RoundTrip {

        @ParameterizedTest
        @MethodSource("provideTestModels")
        fun roundTrip(model: RoundTripModel) {
            // Arrange — the value the app writes into the field itself (Max, reduce, a quote)
            Locale.setDefault(model.locale)
            val written = model.amount.parseBigDecimal(DECIMALS)

            // Act — the field re-validates it, then the model reads it back
            val fieldValue = prepareEnter(
                oldValue = "",
                newValue = written,
                decimalFormat = createAmountDecimalFormat(),
                decimals = DECIMALS,
            )

            // Assert
            assertThat(fieldValue).isEqualTo(written)
            assertThat(fieldValue.parseToBigDecimal(DECIMALS)).isEqualTo(model.amount)
        }

        private fun provideTestModels() = listOf(
            RoundTripModel(locale = Locale.US, amount = BigDecimal("0.00000712")),
            RoundTripModel(locale = DANISH, amount = BigDecimal("0.00000712")),
            RoundTripModel(locale = DANISH, amount = BigDecimal("1234.5")),
            RoundTripModel(locale = Locale.GERMANY, amount = BigDecimal("0.5")),
            RoundTripModel(locale = Locale.FRANCE, amount = BigDecimal("1234.5")),
        )
    }

    internal data class EnterModel(
        val locale: Locale,
        val typed: String,
        val expectedFieldValue: String,
        val expectedAmount: BigDecimal,
        val oldValue: String = "",
    )

    internal data class RoundTripModel(val locale: Locale, val amount: BigDecimal)

    private companion object {
        const val DECIMALS = 8
        val DANISH: Locale = Locale.forLanguageTag("da-DK")
    }
}