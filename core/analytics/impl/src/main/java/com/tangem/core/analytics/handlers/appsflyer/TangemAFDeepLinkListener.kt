package com.tangem.core.analytics.handlers.appsflyer

import com.appsflyer.deeplink.DeepLink
import com.appsflyer.deeplink.DeepLinkListener
import com.appsflyer.deeplink.DeepLinkResult
import com.tangem.core.analytics.api.AppsFlyerDeepLink
import com.tangem.core.analytics.api.AppsFlyerDeepLinkHandler
import com.tangem.utils.logging.TangemLogger
import javax.inject.Inject

class TangemAFDeepLinkListener @Inject internal constructor(
    private val deepLinkHandler: AppsFlyerDeepLinkHandler,
) : DeepLinkListener {

    override fun onDeepLinking(p0: DeepLinkResult) {
        when (p0.status) {
            DeepLinkResult.Status.FOUND -> {
                val deepLink = p0.deepLink

                @Suppress("NullableToStringCall")
                TangemLogger.i("AppsFlyer UDL payload: deferred=${deepLink.isDeferred}, click=${deepLink.clickEvent}")

                deepLinkHandler.handle(deepLink = deepLink.toAppsFlyerDeepLink())
            }
            DeepLinkResult.Status.NOT_FOUND -> TangemLogger.i("No deep link found")
            DeepLinkResult.Status.ERROR -> TangemLogger.e("Deep link error: ${p0.error}")
        }
    }

    private fun DeepLink.toAppsFlyerDeepLink(): AppsFlyerDeepLink = AppsFlyerDeepLink(
        deepLinkValue = deepLinkValue,
        afDp = getStringValue(AF_DP),
        deepLinkSub1 = getStringValue(DEEP_LINK_SUB_1),
        deepLinkSub2 = getStringValue(DEEP_LINK_SUB_2),
        isDeferred = isDeferred,
    )

    private companion object {
        const val AF_DP = "af_dp"
        const val DEEP_LINK_SUB_1 = "deep_link_sub1"
        const val DEEP_LINK_SUB_2 = "deep_link_sub2"
    }
}