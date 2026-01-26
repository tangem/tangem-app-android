package com.tangem.tap.common.analytics.appsflyer

import com.appsflyer.deeplink.DeepLink
import com.tangem.datasource.local.appsflyer.AppsFlyerConversionStore
import com.tangem.domain.wallets.models.AppsFlyerConversionData
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

@Singleton
class AppsFlyerReferralParamsHandler @Inject constructor(
    private val appsFlyerConversionStore: AppsFlyerConversionStore,
    dispatchers: CoroutineDispatcherProvider,
) {

    private val coroutineScope = CoroutineScope(dispatchers.io + SupervisorJob())
    private val mutex = Mutex()

    fun handle(deepLink: DeepLink) {
        handle(
            deepLinkValue = deepLink.deepLinkValue,
            deepLinkSub1 = deepLink.getStringValue(DEEP_LINK_SUB_1),
            deepLinkSub2 = deepLink.getStringValue(DEEP_LINK_SUB_2),
        )
    }

    fun handle(params: Map<String?, Any?>) {
        handle(
            deepLinkValue = params[DEEP_LINK_VALUE] as? String,
            deepLinkSub1 = params[DEEP_LINK_SUB_1] as? String,
            deepLinkSub2 = params[DEEP_LINK_SUB_2] as? String,
        )
    }

    private fun handle(deepLinkValue: String?, deepLinkSub1: String?, deepLinkSub2: String?) {
        if (deepLinkValue != REFERRAL_DEEP_LINK_VALUE) {
            Timber.i("Ignoring deep link with value: ${deepLinkValue ?: "null"}")
            return
        }

        @Suppress("NullableToStringCall")
        Timber.i("refcode=$deepLinkSub1\ncampaign=$deepLinkSub2")

        if (!isValidParam(deepLinkSub1)) {
            Timber.e("Deeplink conversion data is invalid")
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
                appsFlyerConversionStore.storeIfAbsent(
                    value = AppsFlyerConversionData(refcode = refcode, campaign = campaign),
                )
            }
        }
    }

    private companion object {

        const val REFERRAL_DEEP_LINK_VALUE = "referral"
        const val DEEP_LINK_VALUE = "deep_link_value"
        const val DEEP_LINK_SUB_1 = "deep_link_sub1"
        const val DEEP_LINK_SUB_2 = "deep_link_sub2"
    }
}