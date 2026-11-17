package com.tangem.features.marketing.impl.promopage

import com.tangem.core.navigation.deeplink.DeepLinkHandler
import com.tangem.core.navigation.deeplink.DeepLinkRequest
import com.tangem.features.marketing.api.promopage.PromoPageDeepLinkHandler
import javax.inject.Inject

/** Only makes the link known to the uid binding; `PromoPageRouter` opens the page. */
internal class DefaultPromoPageDeepLinkHandler @Inject constructor() : DeepLinkHandler, PromoPageDeepLinkHandler {

    override fun handle(request: DeepLinkRequest) = Unit
}