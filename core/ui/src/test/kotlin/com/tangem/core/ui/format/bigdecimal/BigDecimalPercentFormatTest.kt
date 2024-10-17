package com.tangem.core.ui.format.bigdecimal

import com.google.common.truth.Truth
import org.junit.Test
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
}