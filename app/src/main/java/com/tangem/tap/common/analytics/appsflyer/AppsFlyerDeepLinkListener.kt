package com.tangem.tap.common.analytics.appsflyer

import com.appsflyer.deeplink.DeepLinkListener
import com.appsflyer.deeplink.DeepLinkResult
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppsFlyerDeepLinkListener @Inject constructor(
    private val referralParamsHandler: AppsFlyerReferralParamsHandler,
) : DeepLinkListener {

    override fun onDeepLinking(p0: DeepLinkResult) {
        when (p0.status) {
            DeepLinkResult.Status.FOUND -> {
                referralParamsHandler.handle(deepLink = p0.deepLink)
            }
            DeepLinkResult.Status.NOT_FOUND -> {
                Timber.i("No deep link found")
            }
            DeepLinkResult.Status.ERROR -> {
                Timber.e("Deep link error: ${p0.error}")
            }
        }
    }
}