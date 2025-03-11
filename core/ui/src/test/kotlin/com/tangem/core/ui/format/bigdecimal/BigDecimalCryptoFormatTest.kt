package com.tangem.core.ui.format.bigdecimal

import com.google.common.truth.Truth
import org.junit.Test
import java.math.BigDecimal
import java.util.Locale

internal class BigDecimalCryptoFormatTest {

    private val testLocale = Locale.US
    private val testLocale2 = Locale.GERMANY
    private val symbol = "BTC"

    // === defaultAmount() ===

    @Test
    fun `defaultAmount (usually used as a user balance)`() {
        val testValue = BigDecimal("0.123456789999")

        val formatted = testValue.format {
            crypto(
                symbol = symbol,
                decimals = 8,
                locale = testLocale,
            ).defaultAmount()
        }

        Truth.assertThat(formatted)
            .isEqualTo("0.12345679".addSymbolWithSpaceLeft(symbol))
    }

    @Test
    fun `defaultAmount (usually used as a user balance) alter locale`() {
        val testValue = BigDecimal("0.123456789999")

        val formatted = testValue.format {
            crypto(
                symbol = symbol,
                decimals = 8,
                locale = testLocale2,
            ).defaultAmount()
        }

        Truth.assertThat(formatted)
            .isEqualTo("0,12345679".addSymbolWithSpaceRight(symbol))
    }

    @Test
    fun `defaultAmount decimals more than 8`() {
        val testValue = BigDecimal("0.123456789999")

        val formatted = testValue.format {
            crypto(
                symbol = symbol,
                decimals = 10,
                locale = testLocale,
            ).defaultAmount()
        }

        Truth.assertThat(formatted)
            .isEqualTo("0.12345679".addSymbolWithSpaceLeft(symbol))
    }

    @Test
    fun `defaultAmount decimals more than 8 (short value)`() {
        val testValue = BigDecimal("0.12345")

        val formatted = testValue.format {
            crypto(
                symbol = symbol,
                decimals = 10,
                locale = testLocale,
            ).defaultAmount()
        }

        Truth.assertThat(formatted)
            .isEqualTo("0.12345".addSymbolWithSpaceLeft(symbol))
    }

    @Test
    fun `defaultAmount decimals minimal (short value)`() {
        val testValue = BigDecimal("0.12345")

        val formatted = testValue.format {
            crypto(
                symbol = symbol,
                decimals = 2,
                locale = testLocale,
            ).defaultAmount()
        }

        Truth.assertThat(formatted)
            .isEqualTo("0.12".addSymbolWithSpaceLeft(symbol))
    }

    @Test
    fun `defaultAmount less than 2 decimals`() {
        val testValue = BigDecimal("0.12345")

        val formatted = testValue.format {
            crypto(
                symbol = symbol,
                decimals = 0,
                locale = testLocale,
            ).defaultAmount()
        }

        Truth.assertThat(formatted)
            .isEqualTo("0.12".addSymbolWithSpaceLeft(symbol))
    }

    @Test
    fun `defaultAmount grouping`() {
        val testValue = BigDecimal("12345678.11")

        val formatted = testValue.format {
            crypto(
                symbol = symbol,
                decimals = 0,
                locale = testLocale,
            ).defaultAmount()
        }

        Truth.assertThat(formatted)
            .isEqualTo("12,345,678.11".addSymbolWithSpaceLeft(symbol))
    }

    // === shorted() ===

    @Test
    fun `shorted amount smoke`() {
        val testValue = BigDecimal("50000.126123")

        val formatted = testValue.format {
            crypto(
                symbol = symbol,
                decimals = 8,
                locale = testLocale,
            ).shorted()
        }

        Truth.assertThat(formatted)
            .isEqualTo("50,000.13".addSymbolWithSpaceLeft(symbol))
    }

    @Test
    fun `shorted amount decimals less than 2 grouping`() {
        val testValue = BigDecimal("50000.126123")

        val formatted = testValue.format {
            crypto(
                symbol = symbol,
                decimals = 1,
                locale = testLocale,
            ).shorted()
        }

        Truth.assertThat(formatted)
            .isEqualTo("50,000.13".addSymbolWithSpaceLeft(symbol))
    }

    @Test
    fun `shorted amount less than threshold`() {
        val testValue = BigDecimal("0.0034567899")

        val formatted = testValue.format {
            crypto(
                symbol = symbol,
                decimals = 4,
                locale = testLocale,
            ).shorted()
        }

        Truth.assertThat(formatted)
            .isEqualTo("0.0034".addSymbolWithSpaceLeft(symbol))
    }

    @Test
    fun `shorted amount less than threshold, more decimals`() {
        val testValue = BigDecimal("0.00345678")

        val formatted = testValue.format {
            crypto(
                symbol = symbol,
                decimals = 8,
                locale = testLocale,
            ).shorted()
        }

        Truth.assertThat(formatted)
            .isEqualTo("0.003456".addSymbolWithSpaceLeft(symbol))
    }

    @Test
    fun `shorted amount diff locale half up`() {
        val testValue = BigDecimal("50000.126123")

        val formatted = testValue.format {
            crypto(
                symbol = symbol,
                decimals = 8,
                locale = testLocale2,
            ).shorted()
        }

        Truth.assertThat(formatted)
            .isEqualTo("50.000,13".addSymbolWithSpaceRight(symbol))
    }

    // === uncapped() ===

    @Test
    fun `uncapped amount`() {
        val testValue = BigDecimal("50000.123412341234")

        val formatted = testValue.format {
            crypto(
                symbol = symbol,
                decimals = 10,
                locale = testLocale,
            ).uncapped()
        }

        Truth.assertThat(formatted)
            .isEqualTo("50,000.1234123412".addSymbolWithSpaceLeft(symbol))
    }

    @Test
    fun `uncapped amount diff locale`() {
        val testValue = BigDecimal("50000.123412341234")

        val formatted = testValue.format {
            crypto(
                symbol = symbol,
                decimals = 10,
                locale = testLocale2,
            ).uncapped()
        }

        Truth.assertThat(formatted)
            .isEqualTo("50.000,1234123412".addSymbolWithSpaceRight(symbol))
    }

    @Test
    fun `uncapped amount half up`() {
        val testValue = BigDecimal("50000.12341234125")

        val formatted = testValue.format {
            crypto(
                symbol = symbol,
                decimals = 10,
                locale = testLocale2,
            ).uncapped()
        }

        Truth.assertThat(formatted)
            .isEqualTo("50.000,1234123413".addSymbolWithSpaceRight(symbol))
    }

    @Test
    fun `uncapped amount min decimals`() {
        val testValue = BigDecimal("50000.12341234125")

        val formatted = testValue.format {
            crypto(
                symbol = symbol,
                decimals = 1,
                locale = testLocale2,
            ).uncapped()
        }

        Truth.assertThat(formatted)
            .isEqualTo("50.000,12".addSymbolWithSpaceRight(symbol))
    }

    // === fee ===

    @Test
    fun `fee amount`() {
        val testValue = BigDecimal("0.000123412341234")

        val formatted = testValue.format {
            crypto(
                symbol = symbol,
                decimals = 10,
                locale = testLocale,
            ).fee()
        }

        Truth.assertThat(formatted)
            .isEqualTo("0.000123".addSymbolWithSpaceLeft(symbol))
    }

    @Test
    fun `fee amount diff locale`() {
        val testValue = BigDecimal("0.000123412341234")

        val formatted = testValue.format {
            crypto(
                symbol = symbol,
                decimals = 10,
                locale = testLocale2,
            ).fee()
        }

        Truth.assertThat(formatted)
            .isEqualTo("0,000123".addSymbolWithSpaceRight(symbol))
    }

    @Test
    fun `fee amount canBeLower true`() {
        val testValue = BigDecimal("0.000123412341234")

        val formatted = testValue.format {
            crypto(
                symbol = symbol,
                decimals = 10,
                locale = testLocale,
            ).fee(canBeLower = true)
        }

        Truth.assertThat(formatted)
            .isEqualTo("<" + CURRENCY_SPACE_FOR_TESTS + "0.000123".addSymbolWithSpaceLeft(symbol))
    }

    @Test
    fun `fee amount canBeLower true (diff locale)`() {
        val testValue = BigDecimal("0.000123412341234")

        val formatted = testValue.format {
            crypto(
                symbol = symbol,
                decimals = 10,
                locale = testLocale2,
            ).fee(canBeLower = true)
        }

        Truth.assertThat(formatted)
            .isEqualTo("<" + "0,000123".addSymbolWithSpaceRight(symbol))
    }

    @Test
    fun `fee amount lee than threshold`() {
        val testValue = BigDecimal("0.0000001234")

        val formatted = testValue.format {
            crypto(
                symbol = symbol,
                decimals = 10,
                locale = testLocale,
            ).fee()
        }

        Truth.assertThat(formatted)
            .isEqualTo("<" + CURRENCY_SPACE_FOR_TESTS + "0.000001".addSymbolWithSpaceLeft(symbol))
    }

    @Test
    fun `fee amount min decimals half up`() {
        val testValue = BigDecimal("0.125412341234")

        val formatted = testValue.format {
            crypto(
                symbol = symbol,
                decimals = 1,
                locale = testLocale,
            ).fee()
        }

        Truth.assertThat(formatted)
            .isEqualTo("0.13".addSymbolWithSpaceLeft(symbol))
    }

    // === anyDecimals() ===

    @Test
    fun `anyDecimals smoke`() {
        val testValue = BigDecimal("0.123412341234")

        val formatted = testValue.format {
            crypto(
                symbol = symbol,
                decimals = 5,
                locale = testLocale,
            ).anyDecimals()
        }

        Truth.assertThat(formatted)
            .isEqualTo("0.12341".addSymbolWithSpaceLeft(symbol))
    }

    @Test
    fun `anyDecimals zero`() {
        val testValue = BigDecimal("0.123412341234")

        val formatted = testValue.format {
            crypto(
                symbol = symbol,
                decimals = 0,
                locale = testLocale,
            ).anyDecimals()
        }

        Truth.assertThat(formatted)
            .isEqualTo("0".addSymbolWithSpaceLeft(symbol))
    }
}