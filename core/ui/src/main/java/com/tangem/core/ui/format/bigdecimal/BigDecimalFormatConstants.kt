package com.tangem.core.ui.format.bigdecimal

import com.tangem.utils.StringsSigns.DASH_SIGN
import com.tangem.utils.StringsSigns.LOWER_SIGN
import java.math.BigDecimal
import java.util.Currency
import java.util.Locale

object BigDecimalFormatConstants {

    const val EMPTY_BALANCE_SIGN = DASH_SIGN
    const val CAN_BE_LOWER_SIGN = LOWER_SIGN
    val FORMAT_THRESHOLD = BigDecimal("0.01")

    const val CURRENCY_SPACE = '\u00a0'

    val CRYPTO_FEE_FORMAT_THRESHOLD = BigDecimal("0.000001")

    /** Past this many decimals an amount stops being readable; a token carrying more is cut here. */
    const val CRYPTO_REWARD_MAX_DECIMALS = 8

    val usdCurrency: Currency by lazy { Currency.getInstance(Locale.US) }
}