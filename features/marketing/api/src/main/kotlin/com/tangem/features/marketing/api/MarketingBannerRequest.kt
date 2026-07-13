package com.tangem.features.marketing.api

import com.tangem.domain.marketing.models.MarketingScreen
import java.math.BigDecimal

/** Context for a STANDALONE banner request on any of the 6 surfaces. */
data class MarketingBannerRequest(
    val screen: MarketingScreen,
    val amountUsd: BigDecimal? = null,
)

/** Context for a LINKED_TO_PROVIDER banner request (onramp only), matched against the shown provider. */
data class LinkedBannerRequest(
    val screen: MarketingScreen.Onramp,
    val amountUsd: BigDecimal?,
    val currentProviderId: String,
)