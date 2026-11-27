package com.tangem.features.earn.product.api.analytics

import com.tangem.features.earn.api.overview.EarnProductRate
import java.math.BigDecimal

data class EarnScreenAnalytics(
    val rate: EarnProductRate? = null,
    val performanceFee: BigDecimal? = null,
    val position: BigDecimal? = null,
    val availableToWithdraw: BigDecimal? = null,
    val isLiquidityKnown: Boolean = false,
)