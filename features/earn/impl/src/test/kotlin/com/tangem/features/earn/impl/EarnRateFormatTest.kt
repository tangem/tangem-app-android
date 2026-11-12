package com.tangem.features.earn.impl

import com.google.common.truth.Truth.assertThat
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import java.math.BigDecimal
import java.util.Locale

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class EarnRateFormatTest {

    private var previousLocale: Locale = Locale.getDefault()

    @BeforeEach
    fun pinLocale() {
        previousLocale = Locale.getDefault()
        Locale.setDefault(Locale.US)
    }

    @AfterEach
    fun restoreLocale() {
        Locale.setDefault(previousLocale)
    }

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN rate fraction WHEN formatEarnRate THEN percent number without percent sign`(model: FormatModel) {
        // Act
        val actual = model.fraction.formatEarnRate()

        // Assert
        assertThat(actual).isEqualTo(model.expected)
    }

    @Test
    fun `GIVEN German locale WHEN formatEarnRate THEN decimal comma instead of the default locale point`() {
        // Act
        val actual = BigDecimal("0.055").formatEarnRate(locale = Locale.GERMANY)

        // Assert
        assertThat(actual).isEqualTo("5,50")
    }

    private fun provideTestModels() = listOf(
        FormatModel(fraction = BigDecimal("0.055"), expected = "5.50"),
        FormatModel(fraction = BigDecimal("0.00001"), expected = "<0.01"),
        FormatModel(fraction = BigDecimal.ZERO, expected = "0.00"),
        FormatModel(fraction = BigDecimal("0.0001"), expected = "0.01"),
        FormatModel(fraction = BigDecimal("0.12345"), expected = "12.35"),
        FormatModel(fraction = BigDecimal("1.5"), expected = "150.00"),
    )

    internal data class FormatModel(val fraction: BigDecimal, val expected: String) {
        override fun toString(): String = "$fraction -> $expected"
    }
}