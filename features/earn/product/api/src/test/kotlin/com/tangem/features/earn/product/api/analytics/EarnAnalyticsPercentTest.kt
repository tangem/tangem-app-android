package com.tangem.features.earn.product.api.analytics

import com.google.common.truth.Truth.assertThat
import com.tangem.features.earn.api.overview.EarnProductRate
import com.tangem.features.earn.api.overview.EarnRateType
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import java.math.BigDecimal
import java.util.Locale

internal class EarnAnalyticsPercentTest {

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Of {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN fraction WHEN of THEN percent with two decimals and no sign`(model: PercentModel) {
            // Act
            val actual = EarnAnalyticsPercent.of(BigDecimal(model.fraction))

            // Assert
            assertThat(actual).isEqualTo(model.expected)
        }

        @Test
        fun `GIVEN German default locale WHEN of THEN decimal point is kept`() {
            // Arrange
            val previousLocale = Locale.getDefault()
            Locale.setDefault(Locale.GERMANY)

            // Act
            val actual = try {
                EarnAnalyticsPercent.of(BigDecimal("0.055"))
            } finally {
                Locale.setDefault(previousLocale)
            }

            // Assert
            assertThat(actual).isEqualTo("5.50")
        }

        private fun provideTestModels() = percentModels()
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class ToAnalyticsApy {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN rate fraction WHEN toAnalyticsApy THEN percent with two decimals and no sign`(model: PercentModel) {
            // Arrange
            val rate = EarnProductRate(fraction = BigDecimal(model.fraction), type = EarnRateType.APY)

            // Act
            val actual = rate.toAnalyticsApy()

            // Assert
            assertThat(actual).isEqualTo(model.expected)
        }

        private fun provideTestModels() = percentModels()
    }

    internal data class PercentModel(val fraction: String, val expected: String) {
        override fun toString(): String = "$fraction -> $expected"
    }

    private companion object {

        fun percentModels() = listOf(
            PercentModel(fraction = "0.055", expected = "5.50"),
            PercentModel(fraction = "0.12345", expected = "12.35"),
            PercentModel(fraction = "0", expected = "0.00"),
        )
    }
}