package com.tangem.tap.common.analytics.appsflyer

import com.appsflyer.AppsFlyerConversionListener
import com.tangem.utils.logging.TangemLogger
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TangemAFConversionListener @Inject constructor(
    private val referralParamsHandler: AppsFlyerReferralParamsHandler,
) : AppsFlyerConversionListener {

    override fun onConversionDataSuccess(p0: Map<String?, Any?>?) {
        TangemLogger.i("AppsFlyer conversion data success: ${p0.orEmpty()}")

        if (p0 == null) return

        referralParamsHandler.handle(params = p0)
    }

    override fun onConversionDataFail(p0: String?) {
        TangemLogger.e("AppsFlyer conversion data failure: ${p0.orEmpty()}")
    }

    override fun onAppOpenAttribution(p0: Map<String?, String?>?) {
        TangemLogger.i("AppsFlyer app open attribution: ${p0.orEmpty()}")
    }

    override fun onAttributionFailure(p0: String?) {
        TangemLogger.e("AppsFlyer attribution failure: ${p0.orEmpty()}")
    }
}