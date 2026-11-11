package com.tangem.features.earn.product.api.product

import com.tangem.core.ui.extensions.TextReference
import com.tangem.features.earn.api.overview.EarnProductRate
import kotlinx.datetime.Instant
import java.math.BigDecimal

/**
 * [footnote] goes under the entries, for example who audited the contract; [learnMoreUrl] adds a "Learn more" button.
 */
data class EarnHowItWorks(
    val title: TextReference,
    val entries: List<EarnTextBlock>,
    val footnote: TextReference? = null,
    val learnMoreUrl: String? = null,
)

data class EarnLimits(
    val minDeposit: BigDecimal,
    val remainingCap: BigDecimal?,
)

/**
 * A chart period. [id] is the stable key a product recognises the period by in `apyHistory`
 * and is unique within `apyPeriods`; [label] is display text only and may be localised.
 */
data class EarnPeriod(val id: String, val label: TextReference)

data class EarnApyHistory(
    val points: List<EarnApyPoint>,
    val average: BigDecimal?,
)

data class EarnApyPoint(val at: Instant, val rate: EarnProductRate)