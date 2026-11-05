package com.tangem.domain.marketing.models

import java.math.BigDecimal

/**
 * USD min/max eligibility gate. A campaign without min/max bounds is
 * always eligible. A bounded campaign requires a known [amountUsd] — while the amount is unknown the
 * campaign is NOT eligible (hidden until a quote/amount arrives), then it must fall within the bounds.
 */
fun MarketingCampaign.matchesUsdAmount(amountUsd: BigDecimal?): Boolean {
    if (!hasAmountBounds()) return true

    val usd = amountUsd ?: return false
    if (minAmount != null && usd < minAmount) return false
    if (maxAmount != null && usd > maxAmount) return false
    return true
}

fun MarketingCampaign.hasAmountBounds(): Boolean = minAmount != null || maxAmount != null