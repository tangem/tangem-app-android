package com.tangem.core.ui.components.text

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

internal class CounterNumberTest {

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Parse {
        @ParameterizedTest
        @MethodSource("provideTestModels")
        fun `GIVEN formatted text WHEN parsed THEN preserve its digits and affixes`(model: ParseModel) {
            // Arrange
            val (text, separator, expected) = model

            // Act
            val actual = CounterNumber.parse(text, separator)

            // Assert
            assertThat(actual).isEqualTo(expected)
        }

        private fun provideTestModels() = listOf(
            ParseModel("9¢", '.', CounterNumber("", "9", "", "¢")),
            ParseModel("100¢", '.', CounterNumber("", "100", "", "¢")),
            ParseModel("45.5¢", '.', CounterNumber("", "45", "5", "¢")),
            ParseModel("45,5¢", ',', CounterNumber("", "45", "5", "¢")),
            ParseModel(" • 45.50¢", '.', CounterNumber(" • ", "45", "50", "¢")),
            ParseModel("٤٥٫٥¢", '٫', CounterNumber("", "٤٥", "٥", "¢")),
            ParseModel("—", '.', null),
            ParseModel("", '.', null),
            ParseModel("1,000.5", '.', null),
            ParseModel("45,5¢", '.', null),
            ParseModel("1..5", '.', null),
        )
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class IntegerDigit {
        @ParameterizedTest
        @MethodSource("provideTestModels")
        fun `GIVEN a new price WHEN aligning a decimal place THEN keep units aligned`(model: DigitModel) {
            // Arrange
            val number = requireNotNull(CounterNumber.parse(model.text, '.'))

            // Act
            val actual = number.integerDigit(model.place)

            // Assert
            assertThat(actual).isEqualTo(model.expected)
        }

        private fun provideTestModels() = listOf(
            DigitModel("9¢", 0, '9'),
            DigitModel("9¢", 1, null),
            DigitModel("10¢", 0, '0'),
            DigitModel("10¢", 1, '1'),
            DigitModel("45.5¢", 0, '5'),
            DigitModel("46¢", 0, '6'),
            DigitModel("100¢", 2, '1'),
        )
    }

    internal data class ParseModel(val text: String, val separator: Char, val expected: CounterNumber?)
    internal data class DigitModel(val text: String, val place: Int, val expected: Char?)
}