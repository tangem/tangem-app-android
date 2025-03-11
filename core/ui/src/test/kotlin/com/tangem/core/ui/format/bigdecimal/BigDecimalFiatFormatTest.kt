package com.tangem.core.ui.format.bigdecimal

import com.google.common.truth.Truth
import org.junit.Test
import java.math.BigDecimal
import java.util.Locale

internal class BigDecimalFiatFormatTest {

    val testLocale = Locale.US
    val testLocale2 = Locale.GERMANY

    val usdCurrencyCode = "USD"
    val usdSymbol = "$"

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
}