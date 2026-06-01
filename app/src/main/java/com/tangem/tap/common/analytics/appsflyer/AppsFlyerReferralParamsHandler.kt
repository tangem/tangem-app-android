package com.tangem.tap.common.analytics.appsflyer

import com.appsflyer.deeplink.DeepLink
import com.tangem.datasource.local.appsflyer.AppsFlyerDeeplinkSource
import com.tangem.datasource.local.appsflyer.AppsFlyerStore
import com.tangem.domain.wallets.models.AppsFlyerConversionData
import com.tangem.feature.referral.domain.SetShouldShowMobileWalletPromoUseCase
import com.tangem.utils.coroutines.AppCoroutineScope
import com.tangem.utils.logging.TangemLogger
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

@Singleton
class AppsFlyerReferralParamsHandler @Inject constructor(
    private val appsFlyerStore: AppsFlyerStore,
    private val setShouldShowMobileWalletPromoUseCase: SetShouldShowMobileWalletPromoUseCase,
    private val coroutineScope: AppCoroutineScope,
) {

    private val mutex = Mutex()
    private val deepLinkDeferred = CompletableDeferred<String?>()

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
        deepLinkDeferred.complete(deepLink.deepLinkValue)
    }

    fun handleNoDeeplink() {
        deepLinkDeferred.complete(null)
    }

    suspend fun waitForDeeplink(deeplinkSource: AppsFlyerDeeplinkSource): String? {
        val deeplinkFromCache = appsFlyerStore.getDeeplink(deeplinkSource)
        return if (deeplinkFromCache == null) {
            val value = when (deeplinkSource) {
                AppsFlyerDeeplinkSource.TangemPayHotWalletOnboarding -> TANGEM_PAY_HOT_WALLET_ONBOARDING_DEEP_LINK_VALUE
            }
            deepLinkDeferred.await().takeIf { it == value }
        } else {
            deeplinkFromCache
        }
    }

    private fun handle(deepLinkValue: String?, deepLinkSub1: String?, deepLinkSub2: String?) {
        TangemLogger.i("AppsFlyer deeplink received: value=$deepLinkValue")
        when (deepLinkValue) {
            REFERRAL_DEEP_LINK_VALUE -> handleReferral(deepLinkSub1, deepLinkSub2)
            TANGEM_PAY_HOT_WALLET_ONBOARDING_DEEP_LINK_VALUE -> handleTangemPayHotWalletOnboarding(deepLinkValue)
            else -> TangemLogger.i("Ignoring deep link with value: ${deepLinkValue ?: "null"}")
        }
    }

    private fun handleTangemPayHotWalletOnboarding(deepLinkValue: String) {
        coroutineScope.launch {
            appsFlyerStore.storeDeeplink(AppsFlyerDeeplinkSource.TangemPayHotWalletOnboarding, deepLinkValue)
            TangemLogger.i("[TangemPay][HWO] Deep link stored")
        }
    }

    private fun handleReferral(deepLinkSub1: String?, deepLinkSub2: String?) {
        @Suppress("NullableToStringCall")
        TangemLogger.i("refcode=$deepLinkSub1\ncampaign=$deepLinkSub2")

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

    private fun storeConversionData(refcode: String, campaign: String?) {
        coroutineScope.launch {
            mutex.withLock {
                setShouldShowMobileWalletPromoUseCase(true)
                    .onLeft { TangemLogger.e("Error", it) }
                appsFlyerStore.storeIfAbsent(
                    value = AppsFlyerConversionData(refcode = refcode, campaign = campaign),
                )
            }
        }
    }

    private companion object {

        const val REFERRAL_DEEP_LINK_VALUE = "referral"
        const val TANGEM_PAY_HOT_WALLET_ONBOARDING_DEEP_LINK_VALUE = "tpay_mobileonboard"

        const val DEEP_LINK_VALUE = "deep_link_value"
        const val DEEP_LINK_SUB_1 = "deep_link_sub1"
        const val DEEP_LINK_SUB_2 = "deep_link_sub2"
    }
}