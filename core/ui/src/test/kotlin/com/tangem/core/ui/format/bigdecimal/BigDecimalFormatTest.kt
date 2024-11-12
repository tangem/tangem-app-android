package com.tangem.core.ui.format.bigdecimal

import com.google.common.truth.Truth
import org.junit.Test
import java.math.BigDecimal

internal class BigDecimalFormatTest {

    @Test
    fun smoke() {
        val value = BigDecimal("1234")
        val bgformat = BigDecimalFormat { bg ->
            bg.toPlainString() + "!"
        }
        val expected = "1234!"

        Truth.assertThat(
            value.format(bgformat),
        ).isEqualTo(expected)

        Truth.assertThat(
            value.format { bgformat },
        ).isEqualTo(expected)

        Truth.assertThat(
            null.format(fallbackString = "!") { bgformat },
        ).isEqualTo("!")
    }
}