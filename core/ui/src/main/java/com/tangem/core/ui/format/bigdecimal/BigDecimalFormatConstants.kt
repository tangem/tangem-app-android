package com.tangem.core.ui.format.bigdecimal

import com.tangem.utils.StringsSigns.DASH_SIGN
import com.tangem.utils.StringsSigns.LOWER_SIGN
import java.math.BigDecimal
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

object BigDecimalFormatConstants {

    const val EMPTY_BALANCE_SIGN = DASH_SIGN
    const val CAN_BE_LOWER_SIGN = LOWER_SIGN
    val FORMAT_THRESHOLD = BigDecimal("0.01")

    val CURRENCY_SPACE by lazy {
        val formatter = NumberFormat.getCurrencyInstance(Locale.GERMAN).apply {
            currency = Currency.getInstance("USD")
        }
        formatter.format(BigDecimal.TEN).firstOrNull { it.isWhitespace() } ?: '\u00a0'
    }

    val CRYPTO_FEE_FORMAT_THRESHOLD = BigDecimal("0.000001")

    val usdCurrency: Currency = Currency.getInstance("USD")
}
