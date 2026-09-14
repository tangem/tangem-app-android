package com.tangem.tap.common.analytics.appsflyer

import com.appsflyer.deeplink.DeepLink
import com.tangem.domain.appsflyer.repository.AppsFlyerRepository
import com.tangem.domain.appsflyer.AppsFlyerDeeplinkTarget
import com.tangem.domain.wallets.models.AppsFlyerConversionData
import com.tangem.features.tangempay.TangemPayFeatureToggles
import com.tangem.utils.coroutines.AppCoroutineScope
import com.tangem.utils.logging.TangemLogger
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

@Singleton
class AppsFlyerReferralParamsHandler @Inject constructor(
    private val appsFlyerRepository: AppsFlyerRepository,
    private val coroutineScope: AppCoroutineScope,
    private val tangemPayFeatureToggles: TangemPayFeatureToggles,
) {

    private val isHandledByUnifiedDeepLinking = AtomicBoolean(false)

    fun handle(params: Map<String?, Any?>) {
        if (isHandledByUnifiedDeepLinking.get()) {
            TangemLogger.i("Skipping AppsFlyer conversion data: the deep link is already handled by UDL")
            return
        }

        if (!isFirstLaunch(params)) {
            TangemLogger.i("Skipping AppsFlyer conversion data: cached install payload replayed on app open")
            return
        }

        handle(
            deepLinkValue = params[DEEP_LINK_VALUE] as? String,
            deepLinkSub1 = params[DEEP_LINK_SUB_1] as? String,
            deepLinkSub2 = params[DEEP_LINK_SUB_2] as? String,
        )
    }

    fun handleDeeplink(deepLink: DeepLink) {
        isHandledByUnifiedDeepLinking.set(true)

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

        when (val target = AppsFlyerDeeplinkTarget.from(deepLinkValue)) {
            is AppsFlyerDeeplinkTarget.Known -> storeNavigationDeeplink(target.deeplink.deepLinkValue)
            is AppsFlyerDeeplinkTarget.TangemPayCampaign -> storeCampaignDeeplink(target.key)
            null -> TangemLogger.i("Ignoring deep link with value: ${deepLinkValue ?: "null"}")
        }
    }

    private fun isFirstLaunch(params: Map<String?, Any?>): Boolean = params[IS_FIRST_LAUNCH]?.toString() == "true"

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

    private fun storeCampaignDeeplink(campaignKey: String) {
        if (!tangemPayFeatureToggles.isCustomCardsCobrandEnabled) {
            TangemLogger.i("Custom cards co-brand is disabled, ignoring campaign deep link: $campaignKey")
            return
        }

        storeNavigationDeeplink(campaignKey)
    }

    private fun storeNavigationDeeplink(deepLinkValue: String) {
        coroutineScope.launch {
            appsFlyerRepository.storeNavigationDeeplink(deepLinkValue)
            TangemLogger.i("AppsFlyer navigation deep link stored: $deepLinkValue")
        }
    }

    private fun storeConversionData(refcode: String, campaign: String?) {
        coroutineScope.launch {
            appsFlyerRepository.storeIfAbsent(
                value = AppsFlyerConversionData(refcode = refcode, campaign = campaign),
            )
        }
    }

    private companion object {

        const val IS_FIRST_LAUNCH = "is_first_launch"
        const val DEEP_LINK_VALUE = "deep_link_value"
        const val DEEP_LINK_SUB_1 = "deep_link_sub1"
        const val DEEP_LINK_SUB_2 = "deep_link_sub2"
    }
}