package com.tangem.features.polymarket.impl.common

import java.math.BigDecimal

internal object PolymarketConst {
    const val CENT_SIGN = "¢"
    const val USD_CODE = "USD"
    const val USD_SYMBOL = "$"
    const val COLLATERAL_DECIMALS = 2
    const val SHARES_DECIMALS = 2

    const val EASTERN_TIME_ZONE = "America/New_York"

    val SHARES_STEP: BigDecimal = BigDecimal.ONE

    const val POSITION_INDEXING_READS = 5
    const val CREATED_POSITION_READS = 15
    const val POSITION_INDEXING_RETRY_MILLIS = 2_000L
}