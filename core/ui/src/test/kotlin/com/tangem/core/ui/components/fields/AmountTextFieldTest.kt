package com.tangem.core.ui.components.fields

import com.google.common.truth.Truth.assertThat
import com.tangem.core.ui.utils.createAmountDecimalFormat
import com.tangem.core.ui.utils.parseBigDecimal
import com.tangem.core.ui.utils.parseToBigDecimal
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.math.BigDecimal
import java.util.Locale

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class AmountTextFieldTest {

    private val defaultLocale = Locale.getDefault()

    @AfterEach
    fun restoreLocale() {
        Locale.setDefault(defaultLocale)
    }

    @ParameterizedTest
    @MethodSource("provideTestModels")
    fun prepareEnter(model: EnterModel) {
        // Arrange
        Locale.setDefault(model.locale)

        // Act
        val fieldValue = prepareEnter(
            oldValue = "",
            newValue = model.typed,
            decimalFormat = createAmountDecimalFormat(),
            decimals = DECIMALS,
        )
        val amount = fieldValue.parseToBigDecimal(DECIMALS)

        // Assert
        assertThat(fieldValue).isEqualTo(model.expectedFieldValue)
        assertThat(amount).isEqualTo(model.expectedAmount)
    }

    @ParameterizedTest
    @MethodSource("provideTestModels")
    fun displayMatchesFieldValue(model: EnterModel) {
        // Arrange
        Locale.setDefault(model.locale)

        // Act
        val displayed = model.expectedAmount.parseBigDecimal(DECIMALS)

        // Assert
        assertThat(displayed).isEqualTo(model.expectedFieldValue)
    }

    internal data class EnterModel(
        val locale: Locale,
        val typed: String,
        val expectedFieldValue: String,
        val expectedAmount: BigDecimal,
    )

    private fun provideTestModels() = listOf(
        EnterModel(
            locale = Locale.US,
            typed = "0.00000712",
            expectedFieldValue = "0.00000712",
            expectedAmount = BigDecimal("0.00000712"),
        ),
        // [REDACTED_TASK_KEY]: a keyboard point on a comma locale must stay a decimal separator, not become a grouping one
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
    )

    private companion object {
        const val DECIMALS = 8
        val DANISH: Locale = Locale.forLanguageTag("da-DK")
    }
}