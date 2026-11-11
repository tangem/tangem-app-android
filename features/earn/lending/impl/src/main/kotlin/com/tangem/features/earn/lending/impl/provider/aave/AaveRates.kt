package com.tangem.features.earn.lending.impl.provider.aave

import com.tangem.features.earn.api.overview.EarnProductRate
import com.tangem.features.earn.api.overview.EarnRateType
import java.math.BigDecimal

/** The yield backend reports rates in percent, while Earn keeps them as fractions */
internal fun BigDecimal.percentToApy(): EarnProductRate =
    EarnProductRate(fraction = movePointLeft(PERCENT_SCALE), type = EarnRateType.APY)

private const val PERCENT_SCALE = 2