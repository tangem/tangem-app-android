package com.tangem.core.ui.format.bigdecimal

import com.tangem.utils.StringsSigns.DASH_SIGN
import com.tangem.utils.StringsSigns.LOWER_SIGN
import java.math.BigDecimal
import java.util.Currency

object BigDecimalFormatConstants {

    const val EMPTY_BALANCE_SIGN = DASH_SIGN
    const val CAN_BE_LOWER_SIGN = LOWER_SIGN
    val FORMAT_THRESHOLD = BigDecimal("0.01")
    const val CURRENCY_SPACE = "\u00a0"

    val FIAT_FORMAT_THRESHOLD = BigDecimal("0.01")
    val CRYPTO_FEE_FORMAT_THRESHOLD = BigDecimal("0.000001")

    const val FIAT_MARKET_DEFAULT_DIGITS = 2
    const val FIAT_MARKET_EXTENDED_DIGITS = 6
    const val FRACTIONAL_PART_LENGTH_AFTER_LEADING_ZEROES = 4

    val usdCurrency = Currency.getInstance("USD")
}
