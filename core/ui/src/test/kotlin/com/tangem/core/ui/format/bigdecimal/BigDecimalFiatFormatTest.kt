package com.tangem.core.ui.format.bigdecimal

import androidx.compose.ui.text.SpanStyle
import com.google.common.truth.Truth
import com.tangem.core.ui.extensions.SpanStyleReference
import com.tangem.core.ui.extensions.TextReference
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.math.BigDecimal
import java.text.DecimalFormat
import java.text.NumberFormat
import java.util.Locale

internal class BigDecimalFiatFormatTest {

    val testLocale = Locale.US
    val testLocale2 = Locale.GERMANY

    val usdCurrencyCode = "USD"
    val usdSymbol = "$"

    private val spanStyleStub = SpanStyleReference { SpanStyle() }

    private fun String.addUsdSymbolLeft() = usdSymbol + this

    // === defaultAmount() ===

    @Test
    fun `defaultAmount smoke`() {
        val testValue = BigDecimal("1234.1234")

        val formatted = testValue.format {
            fiat(
                fiatCurrencyCode = usdCurrencyCode,
                fiatCurrencySymbol = usdSymbol,
                locale = testLocale,
            ).defaultAmount()
        }

        Truth.assertThat(formatted)
            .isEqualTo("1,234.12".addUsdSymbolLeft())
    }

    @Test
    fun `defaultAmount half up`() {
        val testValue = BigDecimal("1234.125")

        val formatted = testValue.format {
            fiat(
                fiatCurrencyCode = usdCurrencyCode,
                fiatCurrencySymbol = usdSymbol,
                locale = testLocale,
            ).defaultAmount()
        }

        Truth.assertThat(formatted)
            .isEqualTo("1,234.13".addUsdSymbolLeft())
    }

    @Test
    fun `defaultAmount diff locale`() {
        val testValue = BigDecimal("1234.1234")

        val formatted = testValue.format {
            fiat(
                fiatCurrencyCode = usdCurrencyCode,
                fiatCurrencySymbol = usdSymbol,
                locale = testLocale2,
            ).defaultAmount()
        }

        Truth.assertThat(formatted)
            .isEqualTo("1.234,12".addSymbolWithSpaceRight(usdSymbol))
    }

    @Test
    fun `defaultAmount less threshold`() {
        val testValue = BigDecimal("0.002234")

        val formatted = testValue.format {
            fiat(
                fiatCurrencyCode = usdCurrencyCode,
                fiatCurrencySymbol = usdSymbol,
                locale = testLocale,
            ).defaultAmount()
        }

        Truth.assertThat(formatted)
            .isEqualTo("<" + "0.01".addUsdSymbolLeft())
    }

    @Test
    fun `defaultAmount less threshold diff locale`() {
        val testValue = BigDecimal("0.002234")

        val formatted = testValue.format {
            fiat(
                fiatCurrencyCode = usdCurrencyCode,
                fiatCurrencySymbol = usdSymbol,
                locale = testLocale2,
            ).defaultAmount()
        }

        Truth.assertThat(formatted)
            .isEqualTo("<" + "0,01".addSymbolWithSpaceRight(usdSymbol))
    }

    @Test
    fun `defaultAmount tiny negative rounding to zero is formatted without sign`() {
        val testValue = BigDecimal("-0.0001")

        val formatted = testValue.format {
            fiat(
                fiatCurrencyCode = usdCurrencyCode,
                fiatCurrencySymbol = usdSymbol,
                locale = testLocale,
            ).defaultAmount()
        }

        Truth.assertThat(formatted)
            .isEqualTo("0.00".addUsdSymbolLeft())
    }

    @Test
    fun `defaultAmount negative rounding away from zero keeps sign`() {
        val testValue = BigDecimal("-0.005")

        val formatted = testValue.format {
            fiat(
                fiatCurrencyCode = usdCurrencyCode,
                fiatCurrencySymbol = usdSymbol,
                locale = testLocale,
            ).defaultAmount()
        }

        Truth.assertThat(formatted)
            .isEqualTo("-" + "0.01".addUsdSymbolLeft())
    }

    // === defaultAmount() styled ===

    @Test
    fun `GIVEN locale with distinct monetary separator WHEN styled defaultAmount THEN fraction split at monetary separator`() {
        // Arrange
        // fr_CH plain separator is ',' but currency output uses '.' — searching for the plain one
        // failed to split the amount into whole and styled fractional parts
        val swissLocale = Locale("fr", "CH")
        val symbols = (NumberFormat.getCurrencyInstance(swissLocale) as DecimalFormat).decimalFormatSymbols
        Truth.assertThat(symbols.monetaryDecimalSeparator).isNotEqualTo(symbols.decimalSeparator)

        val testValue = BigDecimal("12.34")

        // Act
        val formatted = testValue.formatStyled {
            fiat(
                fiatCurrencyCode = usdCurrencyCode,
                fiatCurrencySymbol = usdSymbol,
                spanStyleReference = spanStyleStub,
                locale = swissLocale,
            )
        }

        // Assert
        val refs = (formatted as TextReference.Combined).refs.data
        Truth.assertThat(refs).hasSize(3)
        Truth.assertThat(refs[0]).isEqualTo(TextReference.EMPTY)
        Truth.assertThat((refs[1] as TextReference.Str).value).isEqualTo("12")

        val fraction = refs[2] as TextReference.StyledStr
        Truth.assertThat(fraction.value).startsWith("${symbols.monetaryDecimalSeparator}34")
        Truth.assertThat(fraction.value).endsWith(usdSymbol)
    }

    // === approximateAmount() ===

    @Test
    fun `approximateAmount smoke`() {
        val testValue = BigDecimal("1234.1234")

        val formatted = testValue.format {
            fiat(
                fiatCurrencyCode = usdCurrencyCode,
                fiatCurrencySymbol = usdSymbol,
                locale = testLocale,
            ).approximateAmount()
        }

        Truth.assertThat(formatted)
            .isEqualTo("~" + "1,234.12".addUsdSymbolLeft())
    }

    @Test
    fun `approximateAmount half up`() {
        val testValue = BigDecimal("1234.125")

        val formatted = testValue.format {
            fiat(
                fiatCurrencyCode = usdCurrencyCode,
                fiatCurrencySymbol = usdSymbol,
                locale = testLocale,
            ).approximateAmount()
        }

        Truth.assertThat(formatted)
            .isEqualTo("~" + "1,234.13".addUsdSymbolLeft())
    }

    @Test
    fun `approximateAmount diff locale`() {
        val testValue = BigDecimal("1234.1234")

        val formatted = testValue.format {
            fiat(
                fiatCurrencyCode = usdCurrencyCode,
                fiatCurrencySymbol = usdSymbol,
                locale = testLocale2,
            ).approximateAmount()
        }

        Truth.assertThat(formatted)
            .isEqualTo("~" + "1.234,12".addSymbolWithSpaceRight(usdSymbol))
    }

    @Test
    fun `approximateAmount less threshold`() {
        val testValue = BigDecimal("0.002234")

        val formatted = testValue.format {
            fiat(
                fiatCurrencyCode = usdCurrencyCode,
                fiatCurrencySymbol = usdSymbol,
                locale = testLocale,
            ).approximateAmount()
        }

        Truth.assertThat(formatted)
            .isEqualTo("<" + "0.01".addUsdSymbolLeft())
    }

    // === uncapped() ===

    @Test
    fun `uncapped smoke`() {
        val testValue = BigDecimal("1234.1234")

        val formatted = testValue.format {
            fiat(
                fiatCurrencyCode = usdCurrencyCode,
                fiatCurrencySymbol = usdSymbol,
                locale = testLocale,
            ).uncapped()
        }

        Truth.assertThat(formatted)
            .isEqualTo("1,234.12".addUsdSymbolLeft())
    }

    @Test
    fun `uncapped less threshold`() {
        val testValue = BigDecimal("0.00121")

        val formatted = testValue.format {
            fiat(
                fiatCurrencyCode = usdCurrencyCode,
                fiatCurrencySymbol = usdSymbol,
                locale = testLocale,
            ).uncapped()
        }

        Truth.assertThat(formatted)
            .isEqualTo("0.00121".addUsdSymbolLeft())
    }

    @Test
    fun `uncapped decimals overflow`() {
        val testValue = BigDecimal("0.00123412341234")

        val formatted = testValue.format {
            fiat(
                fiatCurrencyCode = usdCurrencyCode,
                fiatCurrencySymbol = usdSymbol,
                locale = testLocale,
            ).uncapped()
        }

        Truth.assertThat(formatted)
            .isEqualTo("0.001234".addUsdSymbolLeft())
    }

    // === price() ===

    @Test
    fun `price smoke`() {
        val testValue = BigDecimal("1234.1234")

        val formatted = testValue.format {
            fiat(
                fiatCurrencyCode = usdCurrencyCode,
                fiatCurrencySymbol = usdSymbol,
                locale = testLocale,
            ).price()
        }

        Truth.assertThat(formatted)
            .isEqualTo("1,234.12".addUsdSymbolLeft())
    }

    @Test
    fun `price diff locale`() {
        val testValue = BigDecimal("1234.1234")

        val formatted = testValue.format {
            fiat(
                fiatCurrencyCode = usdCurrencyCode,
                fiatCurrencySymbol = usdSymbol,
                locale = testLocale2,
            ).price()
        }

        Truth.assertThat(formatted)
            .isEqualTo("1.234,12".addSymbolWithSpaceRight(usdSymbol))
    }

    @Test
    fun `price less threshold`() {
        val testValue = BigDecimal("0.99987")

        val formatted = testValue.format {
            fiat(
                fiatCurrencyCode = usdCurrencyCode,
                fiatCurrencySymbol = usdSymbol,
                locale = testLocale,
            ).price()
        }

        Truth.assertThat(formatted)
            .isEqualTo("0.9999".addUsdSymbolLeft())
    }

    @Test
    fun `price less threshold more decimals strip zeros`() {
        val testValue = BigDecimal("0.0000123000")

        val formatted = testValue.format {
            fiat(
                fiatCurrencyCode = usdCurrencyCode,
                fiatCurrencySymbol = usdSymbol,
                locale = testLocale,
            ).price()
        }

        Truth.assertThat(formatted)
            .isEqualTo("0.0000123".addUsdSymbolLeft())
    }

    @Test
    fun `price less threshold too much decimals strip zeros`() {
        val testValue = BigDecimal("0.000000000000000000001230001234000")

        val formatted = testValue.format {
            fiat(
                fiatCurrencyCode = usdCurrencyCode,
                fiatCurrencySymbol = usdSymbol,
                locale = testLocale,
            ).price()
        }

        Truth.assertThat(formatted)
            .isEqualTo("0.00000000000000000000123".addUsdSymbolLeft())
    }

    @Test
    fun `price zero is formatted with default precision and does not crash`() {
        val testValue = BigDecimal.ZERO

        val formatted = testValue.format {
            fiat(
                fiatCurrencyCode = usdCurrencyCode,
                fiatCurrencySymbol = usdSymbol,
                locale = testLocale,
            ).price()
        }

        Truth.assertThat(formatted)
            .isEqualTo("0.00".addUsdSymbolLeft())
    }

    @Test
    fun `price negative integer keeps sign and does not crash`() {
        val testValue = BigDecimal("-500")

        val formatted = testValue.format {
            fiat(
                fiatCurrencyCode = usdCurrencyCode,
                fiatCurrencySymbol = usdSymbol,
                locale = testLocale,
            ).price()
        }

        Truth.assertThat(formatted)
            .isEqualTo("-" + "500.00".addUsdSymbolLeft())
    }

    @Test
    fun `price negative fractional keeps sign and does not crash`() {
        val testValue = BigDecimal("-0.5")

        val formatted = testValue.format {
            fiat(
                fiatCurrencyCode = usdCurrencyCode,
                fiatCurrencySymbol = usdSymbol,
                locale = testLocale,
            ).price()
        }

        Truth.assertThat(formatted)
            .isEqualTo("-" + "0.50".addUsdSymbolLeft())
    }

    @ParameterizedTest
    @MethodSource("provideTestCasesForOptionalDecimals")
    fun `GIVEN amount WHEN format with optionalDecimals THEN correct answer`(
        amount: String,
        answer: String,
    ) {
        val testValue = BigDecimal(amount)

        val formatted = testValue.format {
            fiat(
                fiatCurrencyCode = usdCurrencyCode,
                fiatCurrencySymbol = usdSymbol,
                locale = testLocale,
            ).optionalDecimals()
        }

        Truth.assertThat(formatted).isEqualTo(answer)
    }

    private companion object {
        @JvmStatic
        fun provideTestCasesForOptionalDecimals() = listOf(
            Arguments.of("123", "$123"),
            Arguments.of("123.1", "$123.10"),
            Arguments.of("123.10", "$123.10"),
            Arguments.of("123.456", "$123.46"),
            Arguments.of("0", "$0"),
            Arguments.of("0.1", "$0.10"),
            Arguments.of("0.12", "$0.12"),
            Arguments.of("0.127", "$0.13"),
        )
    }
}