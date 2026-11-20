package com.tangem.features.marketing.impl.promopage

import com.tangem.common.routing.deeplink.DeepLinkHandler
import com.tangem.common.routing.deeplink.DeepLinkRequest
import com.tangem.domain.marketing.MarketingFeatureToggles
import com.tangem.features.marketing.api.promopage.PromoPageDeepLinkHandler
import javax.inject.Inject

/** Only makes the link known to the uid binding; `PromoPageRouter` opens the page. */
internal class DefaultPromoPageDeepLinkHandler @Inject constructor(
    private val marketingFeatureToggles: MarketingFeatureToggles,
) : DeepLinkHandler, PromoPageDeepLinkHandler {

    override val isEnabled: Boolean
        get() = marketingFeatureToggles.isPromoPageEnabled

    override fun handle(request: DeepLinkRequest) = Unit
}