package com.tangem.domain.marketing.models

import java.math.BigDecimal

/**
 * USD min/max eligibility gate. Applies only to swap/onramp campaigns and only when [amountUsd] is known;
 * otherwise the campaign passes (non-amount screens and the "amount unknown" case are not gated).
 */
fun MarketingCampaign.matchesUsdAmount(amountUsd: BigDecimal?): Boolean {
    val isAmountScreen = type == MarketingScreenType.SWAP || type == MarketingScreenType.ONRAMP
    if (!isAmountScreen || amountUsd == null) return true

    if (minAmount != null && amountUsd < minAmount) return false
    if (maxAmount != null && amountUsd > maxAmount) return false
    return true
}