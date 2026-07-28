package com.tangem.features.marketing.api

import com.tangem.domain.marketing.models.MarketingScreen
import java.math.BigDecimal

/** Context for a STANDALONE banner request on any of the 6 surfaces. */
data class MarketingBannerRequest(
    val screen: MarketingScreen,
    val amountUsd: BigDecimal? = null,
)

/**
 * Context for LINKED_TO_PROVIDER banner requests (onramp only). Provider matching happens per offer at
 * render time via [MarketingBannerComponent.LinkedContent], so the request carries no provider id.
 */
data class LinkedBannerRequest(
    val screen: MarketingScreen.Onramp,
    val amountUsd: BigDecimal?,
)