package com.tangem.features.polymarket.impl.common

import com.tangem.domain.appcurrency.model.AppCurrency
import java.math.BigDecimal

internal object PolymarketConst {
    const val CENT_SIGN = "¢"
    val USD_CODE = AppCurrency.Default.code
    val USD_SYMBOL = AppCurrency.Default.symbol
    const val COLLATERAL_DISPLAY_DECIMALS = 2
    const val SHARES_DISPLAY_DECIMALS = 2
    val SHARES_DISPLAY_STEP: BigDecimal = BigDecimal.ONE.movePointLeft(SHARES_DISPLAY_DECIMALS)

    const val EASTERN_TIME_ZONE = "America/New_York"

    val SHARES_STEP: BigDecimal = BigDecimal.ONE

    const val POSITION_INDEXING_READS = 5
    const val CREATED_POSITION_READS = 15
    const val POSITION_INDEXING_RETRY_MILLIS = 2_000L
}