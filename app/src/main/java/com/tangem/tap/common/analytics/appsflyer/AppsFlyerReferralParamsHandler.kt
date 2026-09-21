package com.tangem.tap.common.analytics.appsflyer

import com.appsflyer.deeplink.DeepLink
import com.tangem.datasource.local.appsflyer.AppsFlyerStore
import com.tangem.domain.appsflyer.AppsFlyerDeeplinkTarget
import com.tangem.domain.wallets.models.AppsFlyerConversionData
import com.tangem.utils.coroutines.AppCoroutineScope
import com.tangem.utils.extensions.uriValidate
import com.tangem.utils.logging.TangemLogger
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

@Singleton
class AppsFlyerReferralParamsHandler @Inject constructor(
    private val appsFlyerStore: AppsFlyerStore,
    private val coroutineScope: AppCoroutineScope,
) {

    private val isDeferredDeepLinkHandled = AtomicBoolean(false)

    fun handle(params: Map<String?, Any?>) {
        if (isDeferredDeepLinkHandled.get()) {
            TangemLogger.i("Skipping AppsFlyer conversion data: the deferred deep link is already handled by UDL")
            return
        }

        if (!isFirstLaunch(params)) {
            TangemLogger.i("Skipping AppsFlyer conversion data: cached install payload replayed on app open")
            return
        }

        handle(
            deepLinkValue = params[DEEP_LINK_VALUE] as? String,
            afDp = params[AF_DP] as? String,
            deepLinkSub1 = params[DEEP_LINK_SUB_1] as? String,
            deepLinkSub2 = params[DEEP_LINK_SUB_2] as? String,
            isDeferredDelivery = true,
        )
    }

    fun handleDeeplink(deepLink: DeepLink) {
        @Suppress("NullableToStringCall")
        TangemLogger.i("AppsFlyer UDL payload: deferred=${deepLink.isDeferred}, click=${deepLink.clickEvent}")

        val isDeferredDelivery = deepLink.isDeferred == true

        val isHandled = handle(
            deepLinkValue = deepLink.deepLinkValue,
            afDp = deepLink.getStringValue(AF_DP),
            deepLinkSub1 = deepLink.getStringValue(DEEP_LINK_SUB_1),
            deepLinkSub2 = deepLink.getStringValue(DEEP_LINK_SUB_2),
            isDeferredDelivery = isDeferredDelivery,
        )

        if (isHandled && isDeferredDelivery) isDeferredDeepLinkHandled.set(true)
    }

    @Suppress("LongParameterList")
    private fun handle(
        deepLinkValue: String?,
        afDp: String?,
        deepLinkSub1: String?,
        deepLinkSub2: String?,
        isDeferredDelivery: Boolean,
    ): Boolean {
        @Suppress("NullableToStringCall")
        TangemLogger.i(
            "AppsFlyer deeplink received: value=$deepLinkValue, af_dp=$afDp, sub1=$deepLinkSub1, sub2=$deepLinkSub2",
        )

        // Referral params are stored for any deep_link_value: a scenario link may also carry a referral code.
        storeReferralParams(refcode = deepLinkSub1, campaign = deepLinkSub2)

        return when {
            storeTarget(deepLinkValue, isDeferredDelivery) -> true
            storeTarget(afDp, isDeferredDelivery) -> true
            else -> {
                @Suppress("NullableToStringCall")
                TangemLogger.i("Ignoring deep link with value: ${deepLinkValue ?: afDp ?: "null"}")
                false
            }
        }
    }

    private fun storeTarget(value: String?, isDeferredDelivery: Boolean): Boolean {
        if (!isValidParam(value)) return false

        return when (val target = AppsFlyerDeeplinkTarget.from(value)) {
            is AppsFlyerDeeplinkTarget.Known -> {
                storeNavigationDeeplink(target.deeplink.deepLinkValue)
                true
            }
            is AppsFlyerDeeplinkTarget.Direct -> storeDirectDeeplink(target.uri, isDeferredDelivery)
            null -> false
        }
    }

    private fun storeDirectDeeplink(uri: String, isDeferredDelivery: Boolean): Boolean {
        if (!isDeferredDelivery) {
            TangemLogger.i("Ignoring direct deep link click, it is delivered as an intent: $uri")
            return false
        }

        if (!uri.uriValidate()) {
            TangemLogger.i("Ignoring deferred deep link with unsafe characters: $uri")
            return false
        }

        if (!isSupportedDeferredDeeplink(uri)) {
            TangemLogger.i("Ignoring unsupported deferred deep link: $uri")
            return false
        }

        storeNavigationDeeplink(uri)
        return true
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

        return !value.isNullOrBlank() && !value.equals("null", ignoreCase = true)
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

        const val IS_FIRST_LAUNCH = "is_first_launch"
        const val DEEP_LINK_VALUE = "deep_link_value"
        const val DEEP_LINK_SUB_1 = "deep_link_sub1"
        const val DEEP_LINK_SUB_2 = "deep_link_sub2"
        const val AF_DP = "af_dp"
    }
}