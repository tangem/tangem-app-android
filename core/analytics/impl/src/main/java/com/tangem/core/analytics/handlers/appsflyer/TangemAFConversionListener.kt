package com.tangem.core.analytics.handlers.appsflyer

import com.appsflyer.AppsFlyerConversionListener
import com.tangem.core.analytics.api.AppsFlyerConversionDataHandler
import com.tangem.domain.appsflyer.AppsFlyerDeeplink
import com.tangem.domain.wallets.models.ReferralParamValidator
import com.tangem.utils.logging.TangemLogger
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class TangemAFConversionListener @Inject constructor(
    private val googleAdsConversionDataReporter: GoogleAdsConversionDataReporter,
    private val conversionDataHandlers: Set<@JvmSuppressWildcards AppsFlyerConversionDataHandler>,
) : AppsFlyerConversionListener {

    override fun onConversionDataSuccess(p0: Map<String?, Any?>?) {
        TangemLogger.i("AppsFlyer conversion data success: ${p0.orEmpty()}")

        if (p0 == null) return

        googleAdsConversionDataReporter.handle(conversionData = p0)

        val conversionData = p0.withTangemPayCampaignDeepLink()
        conversionDataHandlers.forEach { it.handle(conversionData = conversionData) }
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

    private fun Map<String?, Any?>.withTangemPayCampaignDeepLink(): Map<String?, Any?> {
        val campaign = this[CAMPAIGN] as? String
        val isTangemPayCampaign = this[MEDIA_SOURCE] == GOOGLE_ADS_MEDIA_SOURCE &&
            campaign?.contains(TANGEM_PAY_CAMPAIGN_MARKER, ignoreCase = true) == true
        val hasDeepLink = ReferralParamValidator.sanitize(this[DEEP_LINK_VALUE] as? String) != null ||
            ReferralParamValidator.sanitize(this[AF_DP] as? String) != null

        if (!isTangemPayCampaign || hasDeepLink) return this

        val deepLinkValue = AppsFlyerDeeplink.TangemPayMobileOnboarding.deepLinkValue
        TangemLogger.i("Google Ads campaign \"$campaign\" is handled as deep_link_value=$deepLinkValue")

        return this + (DEEP_LINK_VALUE to deepLinkValue)
    }

    private companion object {
        const val MEDIA_SOURCE = "media_source"
        const val CAMPAIGN = "campaign"
        const val DEEP_LINK_VALUE = "deep_link_value"
        const val AF_DP = "af_dp"
        const val GOOGLE_ADS_MEDIA_SOURCE = "googleadwords_int"
        const val TANGEM_PAY_CAMPAIGN_MARKER = "Tangem Pay [MO]"
    }
}