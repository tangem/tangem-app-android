package com.tangem.features.earn.impl.analytics

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

internal class EarnAnalyticsFormatTest {

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class ToAnalyticsApy {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN rate fraction WHEN toAnalyticsApy THEN percent with two decimals and no sign`(model: ApyModel) {
            // Arrange
            val rate = EarnProductRate(fraction = BigDecimal(model.fraction), type = EarnRateType.APY)

            // Act
            val actual = rate.toAnalyticsApy()

            // Assert
            assertThat(actual).isEqualTo(model.expected)
        }

        @Test
        fun `GIVEN German default locale WHEN toAnalyticsApy THEN decimal point is kept`() {
            // Arrange
            val rate = EarnProductRate(fraction = BigDecimal("0.055"), type = EarnRateType.APY)
            val previousLocale = Locale.getDefault()
            Locale.setDefault(Locale.GERMANY)

            // Act
            val actual = try {
                rate.toAnalyticsApy()
            } finally {
                Locale.setDefault(previousLocale)
            }

            // Assert
            assertThat(actual).isEqualTo("5.50")
        }

        private fun provideTestModels() = listOf(
            ApyModel(fraction = "0.055", expected = "5.50"),
            ApyModel(fraction = "0.12345", expected = "12.35"),
            ApyModel(fraction = "0", expected = "0.00"),
        )
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class UsdBucket {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN usd amount WHEN usdBucketOf THEN bucket with the boundary in the upper one`(model: BucketModel) {
            // Act
            val actual = usdBucketOf(model.usd?.let(::BigDecimal))

            // Assert
            assertThat(actual).isEqualTo(model.expected)
        }

        private fun provideTestModels() = listOf(
            BucketModel(usd = "0", expected = "<50"),
            BucketModel(usd = "49.99", expected = "<50"),
            BucketModel(usd = "50", expected = "50-250"),
            BucketModel(usd = "249.99", expected = "50-250"),
            BucketModel(usd = "250", expected = "250-1K"),
            BucketModel(usd = "999.99", expected = "250-1K"),
            BucketModel(usd = "1000", expected = "1K-5K"),
            BucketModel(usd = "4999.99", expected = "1K-5K"),
            BucketModel(usd = "5000", expected = "5K-25K"),
            BucketModel(usd = "24999.99", expected = "5K-25K"),
            BucketModel(usd = "25000", expected = "25K-100K"),
            BucketModel(usd = "99999.99", expected = "25K-100K"),
            BucketModel(usd = "100000", expected = ">100K"),
            BucketModel(usd = "5000000", expected = ">100K"),
            BucketModel(usd = null, expected = null),
        )
    }

    internal data class ApyModel(val fraction: String, val expected: String) {
        override fun toString(): String = "$fraction -> $expected"
    }

    internal data class BucketModel(val usd: String?, val expected: String?) {
        override fun toString(): String = "$usd -> $expected"
    }
}