package com.tangem.tap.common.analytics.appsflyer

import android.content.Context
import com.appsflyer.AppsFlyerLib
import com.tangem.utils.logging.TangemLogger
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GoogleAdsConversionDataReporter @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    fun report(conversionData: Map<String?, Any?>) {
        if (conversionData[MEDIA_SOURCE]?.toString() != GOOGLE_ADS_MEDIA_SOURCE) return
        if (conversionData[IS_FIRST_LAUNCH]?.toString() != "true") return

        AppsFlyerLib.getInstance().logEvent(context, REPORT_EVENT_NAME, conversionData.toReportValues())
        TangemLogger.i("Google Ads conversion data reported to AppsFlyer")
    }

    private companion object {
        const val MEDIA_SOURCE = "media_source"
        const val IS_FIRST_LAUNCH = "is_first_launch"
        const val GOOGLE_ADS_MEDIA_SOURCE = "googleadwords_int"
        const val REPORT_EVENT_NAME = "debug_google_ads_conversion_data"
    }
}

internal fun Map<String?, Any?>.toReportValues(): Map<String, String> {
    val otherKeys = filter { (key, value) -> key != null && value != null && key !in REPORTED_KEYS }
        .keys
        .filterNotNull()
        .sorted()

    return buildMap {
        for (key in REPORTED_KEYS) {
            if (key in this@toReportValues) put(key, this@toReportValues[key].toString())
        }
        if (otherKeys.isNotEmpty()) put(OTHER_KEYS_KEY, otherKeys.joinToString(separator = ","))
    }
}

private const val OTHER_KEYS_KEY = "tangem_other_keys"

private val REPORTED_KEYS = listOf(
    "af_dp",
    "deep_link_value",
    "media_source",
    "af_status",
    "is_first_launch",
    "match_type",
    "af_channel",
    "campaign",
    "campaign_id",
    "af_adset",
    "af_ad",
    "install_time",
    "click_time",
)