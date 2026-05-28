package com.tangem.tap.common.analytics.appsflyer

import com.appsflyer.deeplink.DeepLinkListener
import com.appsflyer.deeplink.DeepLinkResult
import com.tangem.utils.logging.TangemLogger
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppsFlyerDeepLinkListener @Inject constructor(
    private val referralParamsHandler: AppsFlyerReferralParamsHandler,
) : DeepLinkListener {

    override fun onDeepLinking(p0: DeepLinkResult) {
        when (p0.status) {
            DeepLinkResult.Status.FOUND -> {
                referralParamsHandler.handleDeeplink(deepLink = p0.deepLink)
            }
            DeepLinkResult.Status.NOT_FOUND -> {
                referralParamsHandler.handleNoDeeplink()
                TangemLogger.i("No deep link found")
            }
            DeepLinkResult.Status.ERROR -> {
                referralParamsHandler.handleNoDeeplink()
                TangemLogger.e("Deep link error: ${p0.error}")
            }
        }
    }
}