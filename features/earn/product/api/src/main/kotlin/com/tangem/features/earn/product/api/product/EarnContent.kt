package com.tangem.features.earn.product.api.product

import com.tangem.core.ui.extensions.TextReference
import com.tangem.features.earn.api.overview.EarnProductRate
import kotlinx.datetime.Instant
import java.math.BigDecimal

data class EarnHowItWorks(val title: TextReference, val entries: List<EarnTextBlock>)

data class EarnLimits(
    val minDeposit: BigDecimal,
    val remainingCap: BigDecimal?,
)

data class EarnPeriod(val label: TextReference)

data class EarnApyHistory(
    val points: List<EarnApyPoint>,
    val average: BigDecimal?,
)

data class EarnApyPoint(val at: Instant, val rate: EarnProductRate)