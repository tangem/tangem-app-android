package com.tangem.tap.common.analytics.appsflyer

import com.appsflyer.deeplink.DeepLink
import com.tangem.datasource.local.appsflyer.AppsFlyerStore
import com.tangem.domain.appsflyer.AppsFlyerDeeplink
import com.tangem.domain.wallets.models.AppsFlyerConversionData
import com.tangem.utils.coroutines.AppCoroutineScope
import com.tangem.utils.logging.TangemLogger
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

@Singleton
class AppsFlyerReferralParamsHandler @Inject constructor(
    private val appsFlyerStore: AppsFlyerStore,
    private val coroutineScope: AppCoroutineScope,
) {

    fun handle(params: Map<String?, Any?>) {
        handle(
            deepLinkValue = params[DEEP_LINK_VALUE] as? String,
            deepLinkSub1 = params[DEEP_LINK_SUB_1] as? String,
            deepLinkSub2 = params[DEEP_LINK_SUB_2] as? String,
        )
    }

    fun handleDeeplink(deepLink: DeepLink) {
        handle(
            deepLinkValue = deepLink.deepLinkValue,
            deepLinkSub1 = deepLink.getStringValue(DEEP_LINK_SUB_1),
            deepLinkSub2 = deepLink.getStringValue(DEEP_LINK_SUB_2),
        )
    }

    private fun handle(deepLinkValue: String?, deepLinkSub1: String?, deepLinkSub2: String?) {
        @Suppress("NullableToStringCall")
        TangemLogger.i("AppsFlyer deeplink received: value=$deepLinkValue, sub1=$deepLinkSub1, sub2=$deepLinkSub2")

        // Referral params are stored for any deep_link_value: a scenario link may also carry a referral code.
        // Navigation below (and the stories-skip gate reading it) must stay bound to the known deeplink values.
        storeReferralParams(refcode = deepLinkSub1, campaign = deepLinkSub2)

        when (val deeplink = AppsFlyerDeeplink.from(deepLinkValue)) {
            AppsFlyerDeeplink.Referral, AppsFlyerDeeplink.TangemPayMobileOnboarding ->
                storeNavigationDeeplink(deeplink.deepLinkValue)
            null -> TangemLogger.i("Ignoring deep link with value: ${deepLinkValue ?: "null"}")
        }
    }

    private fun storeReferralParams(refcode: String?, campaign: String?) {
        if (!isValidParam(refcode)) return

        val validCampaign = campaign?.takeIf { isValidParam(it) }

        @Suppress("NullableToStringCall")
        TangemLogger.i("Storing referral params: refcode=$refcode, campaign=$validCampaign")

        storeConversionData(refcode = refcode, campaign = validCampaign)
    }

    @OptIn(ExperimentalContracts::class)
    private fun isValidParam(value: String?): Boolean {
        contract {
            returns(true) implies (value != null)
        }

        return value != null && value.isNotBlank() && !value.equals("null", ignoreCase = true)
    }

    private fun storeNavigationDeeplink(deepLinkValue: String) {
        coroutineScope.launch {
            appsFlyerStore.storeNavigationDeeplink(deepLinkValue)
            TangemLogger.i("AppsFlyer navigation deep link stored: $deepLinkValue")
        }
    }

    private fun storeConversionData(refcode: String, campaign: String?) {
        coroutineScope.launch {
            appsFlyerStore.storeIfAbsent(
                value = AppsFlyerConversionData(refcode = refcode, campaign = campaign),
            )
        }
    }

    private companion object {

        const val DEEP_LINK_VALUE = "deep_link_value"
        const val DEEP_LINK_SUB_1 = "deep_link_sub1"
        const val DEEP_LINK_SUB_2 = "deep_link_sub2"
    }
}