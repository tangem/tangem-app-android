package com.tangem.core.ui.utils

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.math.BigDecimal
import java.util.Locale

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class ParseBigDecimalOrNullTest {

    private val originalLocale = Locale.getDefault()

    @AfterEach
    fun restoreLocale() {
        Locale.setDefault(originalLocale)
    }

    @ParameterizedTest
    @MethodSource("provideTestModels")
    fun `GIVEN value in locale WHEN parseBigDecimalOrNull THEN whole value is parsed`(model: ParseModel) {
        // Arrange
        Locale.setDefault(Locale.forLanguageTag(model.localeTag))

        // Act
        val actual = model.input.parseBigDecimalOrNull()

        // Assert
        assertThat(actual).isEqualTo(model.expected)
    }

    @ParameterizedTest
    @MethodSource("provideLocaleTags")
    fun `GIVEN amount formatted in locale WHEN parseBigDecimalOrNull THEN amount round-trips`(localeTag: String) {
        // Arrange
        Locale.setDefault(Locale.forLanguageTag(localeTag))
        val amount = BigDecimal("9.876543")
        val formatted = amount.parseBigDecimal(decimals = 6)

        // Act
        val actual = formatted.parseBigDecimalOrNull()

        // Assert
        assertThat(actual).isEqualTo(amount)
    }

    private fun provideTestModels() = listOf(
        ParseModel(localeTag = "en-US", input = "9.876543", expected = BigDecimal("9.876543")),
        ParseModel(localeTag = "en-US", input = "9,876543", expected = BigDecimal("9.876543")),
        ParseModel(localeTag = "ru-RU", input = "9,876543", expected = BigDecimal("9.876543")),
        ParseModel(localeTag = "ar-EG", input = "٩٫٨٧٦٥٤٣", expected = BigDecimal("9.876543")),
        ParseModel(localeTag = "fa-IR", input = "۹٫۸۷۶۵۴۳", expected = BigDecimal("9.876543")),
        ParseModel(localeTag = "en-US", input = "9,87abc", expected = null),
        ParseModel(localeTag = "en-US", input = "1,000.5", expected = null),
    )

    private fun provideLocaleTags() = listOf("en-US", "ru-RU", "de-DE", "de-CH", "ar-EG", "fa-IR")

    data class ParseModel(val localeTag: String, val input: String, val expected: BigDecimal?)
}