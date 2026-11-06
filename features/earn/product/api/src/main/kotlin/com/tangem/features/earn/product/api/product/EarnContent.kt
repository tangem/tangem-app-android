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

/**
 * A chart period. [id] is the stable key a product recognises the period by in `apyHistory`
 * and is unique within `apyPeriods`; [label] is display text only and may be localised.
 */
data class EarnPeriod(val id: String, val label: TextReference)

/** @property points in no guaranteed order: a consumer that needs the time order sorts them by [EarnApyPoint.at] */
data class EarnApyHistory(
    val points: List<EarnApyPoint>,
    val average: BigDecimal?,
)

data class EarnApyPoint(val at: Instant, val rate: EarnProductRate)