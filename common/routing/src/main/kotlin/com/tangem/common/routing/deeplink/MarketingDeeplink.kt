package com.tangem.common.routing.deeplink

import com.tangem.common.routing.AppRoute
import com.tangem.common.routing.DeepLinkRoute
import com.tangem.common.routing.DeepLinkScheme
import com.tangem.core.analytics.models.AnalyticsParam
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.onramp.model.OnrampSource
import java.net.URI

/**
 * Classification of a marketing-banner deeplink used to decide how a banner tap is routed.
 *
 * Only the `tangem://` scheme with a swap/buy host triggers contextual in-app routing; everything
 * else (external `https://` T&S links, unknown hosts, malformed input) is [EXTERNAL] and handed off
 * to the generic deeplink launcher. This mirrors iOS `DefaultIncomingLinkParser`, where
 * `https://tangem.com/...` links always resolve to an external link, not an in-app destination.
 */
enum class MarketingDeeplink {
    /** `tangem://swap` — open swap for the current token. */
    SWAP,

    /** `tangem://buy` — open onramp for the current token. */
    BUY,

    /** External or unrecognized link — route through the generic deeplink launcher. */
    EXTERNAL,
}

// TODO: [temporary] Banner taps are intercepted in-host and mapped to an AppRoute directly because the
//  shared tangem://swap and tangem://buy deeplinks open context-less screens (generic swap / BuyCrypto
//  hub) with no current-token prefill. Replace with targeted swap/onramp deeplinks and drop this
//  interception: [REDACTED_JIRA]
/**
 * Resolves a marketing-banner [link] into a [MarketingDeeplink]. Never throws: malformed input
 * degrades to [MarketingDeeplink.EXTERNAL].
 */
fun resolveMarketingDeeplink(link: String): MarketingDeeplink {
    val uri = runCatching { URI(link) }.getOrNull() ?: return MarketingDeeplink.EXTERNAL

    if (!uri.scheme.equals(DeepLinkScheme.Tangem.scheme, ignoreCase = true)) {
        return MarketingDeeplink.EXTERNAL
    }

    return when (uri.host) {
        DeepLinkRoute.Swap.host -> MarketingDeeplink.SWAP
        DeepLinkRoute.Buy.host -> MarketingDeeplink.BUY
        else -> MarketingDeeplink.EXTERNAL
    }
}

/**
 * Builds the contextual in-app route for a marketing-banner deeplink on a token-scoped screen (staking,
 * yield, swap, onramp): swap for the current token, or onramp to buy it. Returns `null` for
 * [MarketingDeeplink.EXTERNAL] so the caller falls back to the generic deeplink launcher.
 */
fun MarketingDeeplink.toContextualRoute(
    userWalletId: UserWalletId,
    currency: CryptoCurrency,
    screenSource: AnalyticsParam.ScreensSources,
    onrampSource: OnrampSource = OnrampSource.MARKETING_BANNER,
): AppRoute? = when (this) {
    MarketingDeeplink.SWAP -> AppRoute.Swap(
        userWalletId = userWalletId,
        fromCryptoCurrency = currency,
        screenSource = screenSource.value,
    )
    MarketingDeeplink.BUY -> AppRoute.Onramp(
        userWalletId = userWalletId,
        currency = currency,
        source = onrampSource,
    )
    MarketingDeeplink.EXTERNAL -> null
}