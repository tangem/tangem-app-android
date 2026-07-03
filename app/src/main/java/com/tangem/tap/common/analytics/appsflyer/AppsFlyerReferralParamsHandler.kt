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
        TangemLogger.i("AppsFlyer deeplink received: value=$deepLinkValue")
        when (AppsFlyerDeeplink.from(deepLinkValue)) {
            AppsFlyerDeeplink.Referral -> handleReferral(deepLinkSub1, deepLinkSub2)
            AppsFlyerDeeplink.TangemPayMobileOnboarding ->
                storeNavigationDeeplink(AppsFlyerDeeplink.TangemPayMobileOnboarding.deepLinkValue)
            null -> TangemLogger.i("Ignoring deep link with value: ${deepLinkValue ?: "null"}")
        }
    }

    private fun handleReferral(deepLinkSub1: String?, deepLinkSub2: String?) {
        @Suppress("NullableToStringCall")
        TangemLogger.i("refcode=$deepLinkSub1\ncampaign=$deepLinkSub2")

        storeNavigationDeeplink(AppsFlyerDeeplink.Referral.deepLinkValue)

        if (!isValidParam(deepLinkSub1)) {
            TangemLogger.e("Deeplink conversion data is invalid")
            return
        }

        storeConversionData(refcode = deepLinkSub1, campaign = deepLinkSub2)
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