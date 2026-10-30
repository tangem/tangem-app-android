package com.tangem.features.marketing.api

import com.tangem.domain.marketing.models.MarketingScreen
import java.math.BigDecimal

/** Context for a STANDALONE banner request on any of the 6 surfaces. */
data class MarketingBannerRequest(
    val screen: MarketingScreen,
    val amount: MarketingBannerAmount? = null,
)

/**
 * Context for LINKED_TO_PROVIDER banner requests (onramp only). Provider matching happens per offer at
 * render time via [MarketingBannerComponent.LinkedContent], so the request carries no provider id.
 */
data class LinkedBannerRequest(
    val screen: MarketingScreen.Onramp,
    val amount: MarketingBannerAmount?,
)

/**
 * Amount entered on the screen, matched against the campaigns' USD min/max bounds. A [Fiat] amount is converted
 * to USD by the banner itself, and only when a fetched campaign has amount bounds.
 */
sealed interface MarketingBannerAmount {

    data class Usd(val value: BigDecimal) : MarketingBannerAmount

    data class Fiat(val value: BigDecimal, val currencyCode: String) : MarketingBannerAmount
}