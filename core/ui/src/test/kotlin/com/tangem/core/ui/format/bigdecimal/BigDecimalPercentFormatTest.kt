package com.tangem.core.ui.format.bigdecimal

import com.google.common.truth.Truth
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.math.BigDecimal
import java.util.Locale

internal class BigDecimalPercentFormatTest {

    val testLocale = Locale.US
    val testLocale2 = Locale.GERMANY

    @Test
    fun smoke() {
        val value = BigDecimal("00.34")

        val formatted = value.format {
            percent(locale = testLocale)
        }

        Truth.assertThat(formatted).isEqualTo("34.00%")
    }

    @Test
    fun negative() {
        val value = BigDecimal("00.34").negate()

        val formatted = value.format {
            percent(locale = testLocale)
        }

        Truth.assertThat(formatted).isEqualTo("34.00%")
    }

    @Test
    fun `negative with sign`() {
        val value = BigDecimal("00.34").negate()

        val formatted = value.format {
            percent(
                withoutSign = false,
                locale = testLocale,
            )
        }

        Truth.assertThat(formatted).isEqualTo("-34.00%")
    }

    @Test
    fun `default more decimals half up`() {
        val value = BigDecimal("00.345678").negate()

        val formatted = value.format {
            percent(locale = testLocale)
        }

        Truth.assertThat(formatted).isEqualTo("34.57%")
    }

    @Test
    fun `default diff locale`() {
        val value = BigDecimal("00.345678").negate()

        val formatted = value.format {
            percent(locale = testLocale2)
        }

        Truth.assertThat(formatted).isEqualTo("34,57".addSymbolWithSpaceRight("%"))
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class CanBeLower {

        @ParameterizedTest
        @MethodSource("provideTestModels")
        fun format(model: CanBeLowerModel) {
            // Act
            val formatted = model.value.format {
                percent(
                    withoutSign = model.withoutSign,
                    withPercentSign = model.withPercentSign,
                    locale = testLocale,
                    minFractionDigits = model.fractionDigits,
                    maxFractionDigits = model.fractionDigits,
                    canBeLower = model.canBeLower,
                )
            }

            // Assert
            Truth.assertThat(formatted).isEqualTo(model.expected)
        }

        private fun provideTestModels() = listOf(
            CanBeLowerModel(
                value = BigDecimal("0.000049"),
                canBeLower = true,
                expected = "<0.01%",
            ),
            CanBeLowerModel(
                value = BigDecimal("0.00009999"),
                canBeLower = true,
                expected = "<0.01%",
            ),
            // Exactly at the threshold is representable, so it is shown rather than bounded
            CanBeLowerModel(
                value = BigDecimal("0.0001"),
                canBeLower = true,
                expected = "0.01%",
            ),
            // An empty holding stays distinguishable from a dust one
            CanBeLowerModel(
                value = BigDecimal.ZERO,
                canBeLower = true,
                expected = "0.00%",
            ),
            // The opt-out is what keeps the app's other percent call sites unchanged
            CanBeLowerModel(
                value = BigDecimal("0.000049"),
                canBeLower = false,
                expected = "0.00%",
            ),
            CanBeLowerModel(
                value = BigDecimal("0.5"),
                canBeLower = true,
                expected = "50.00%",
            ),
            // withoutSign takes the absolute value before the bound is applied
            CanBeLowerModel(
                value = BigDecimal("0.000049").negate(),
                canBeLower = true,
                expected = "<0.01%",
            ),
            // The bound follows the requested precision instead of assuming two decimals
            CanBeLowerModel(
                value = BigDecimal("0.005"),
                canBeLower = true,
                fractionDigits = 0,
                expected = "<1%",
            ),
            CanBeLowerModel(
                value = BigDecimal("0.0000001"),
                canBeLower = true,
                fractionDigits = 4,
                expected = "<0.0001%",
            ),
            CanBeLowerModel(
                value = BigDecimal("0.000049"),
                canBeLower = true,
                withPercentSign = false,
                expected = "<0.01",
            ),
        )
    }

    data class CanBeLowerModel(
        val value: BigDecimal,
        val canBeLower: Boolean,
        val expected: String,
        val fractionDigits: Int = 2,
        val withPercentSign: Boolean = true,
        val withoutSign: Boolean = true,
    )
}