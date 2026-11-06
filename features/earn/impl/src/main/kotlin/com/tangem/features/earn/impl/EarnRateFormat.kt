package com.tangem.features.earn.impl

import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.core.ui.format.bigdecimal.percent
import java.math.BigDecimal

/** Badge strings already carry the percent sign, so the number is formatted without it. */
internal fun BigDecimal.formatEarnRate(): String = format { percent(canBeLower = true, withPercentSign = false) }