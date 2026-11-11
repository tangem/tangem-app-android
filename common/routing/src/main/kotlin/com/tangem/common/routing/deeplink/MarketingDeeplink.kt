package com.tangem.common.routing.deeplink

import com.tangem.common.routing.AppRoute
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.onramp.model.OnrampSource
import com.tangem.features.onramp.deeplink.BuyDeepLinkHandler
import java.net.URI

/**
 * Classification of a marketing-banner deeplink used to decide how a banner tap is routed.
 *
 * Only a [BuyDeepLinkHandler] link (`tangem://buy`) triggers contextual in-app routing; everything
 * else (external `https://` T&S links, unknown hosts, malformed input) is [EXTERNAL] and handed off
 * to the generic deeplink launcher. This mirrors iOS `DefaultIncomingLinkParser`, where
 * `https://tangem.com/...` links always resolve to an external link, not an in-app destination.
 */
enum class MarketingDeeplink {
    /** `tangem://buy` — open onramp for the current token. */
    BUY,

    /** External or unrecognized link — route through the generic deeplink launcher. */
    EXTERNAL,
}

// TODO: [temporary] Banner taps are intercepted in-host and mapped to an AppRoute directly because the
//  shared tangem://buy deeplink opens a context-less screen (BuyCrypto hub) with no current-token
//  prefill. Replace with a targeted onramp deeplink and drop this interception:
//  [REDACTED_JIRA]
/**
 * Resolves a marketing-banner [link] into a [MarketingDeeplink]. Never throws: malformed input
 * degrades to [MarketingDeeplink.EXTERNAL].
 */
fun resolveMarketingDeeplink(link: String): MarketingDeeplink {
    val uri = runCatching { URI(link) }.getOrNull() ?: return MarketingDeeplink.EXTERNAL

    return if (BuyDeepLinkHandler.canHandle(uri)) MarketingDeeplink.BUY else MarketingDeeplink.EXTERNAL
}

/**
 * Builds the contextual in-app route for a marketing-banner deeplink on a token-scoped screen (staking,
 * yield, swap, onramp): onramp to buy the current token. Returns `null` for [MarketingDeeplink.EXTERNAL] so the
 * caller falls back to the generic deeplink launcher.
 */
fun MarketingDeeplink.toContextualRoute(
    userWalletId: UserWalletId,
    currency: CryptoCurrency,
    onrampSource: OnrampSource = OnrampSource.MARKETING_BANNER,
): AppRoute? = when (this) {
    MarketingDeeplink.BUY -> AppRoute.Onramp(
        userWalletId = userWalletId,
        currency = currency,
        source = onrampSource,
    )
    MarketingDeeplink.EXTERNAL -> null
}