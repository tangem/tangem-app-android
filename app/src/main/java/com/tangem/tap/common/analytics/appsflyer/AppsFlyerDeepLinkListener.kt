package com.tangem.tap.common.analytics.appsflyer

import androidx.annotation.VisibleForTesting
import com.appsflyer.deeplink.DeepLink
import com.appsflyer.deeplink.DeepLinkListener
import com.appsflyer.deeplink.DeepLinkResult
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
class AppsFlyerDeepLinkListener @Inject constructor(
    private val appsFlyerConversionStore: AppsFlyerConversionStore,
    dispatchers: CoroutineDispatcherProvider,
) : DeepLinkListener {

    private val coroutineScope = CoroutineScope(dispatchers.io + SupervisorJob())
    private val mutex = Mutex()

    override fun onDeepLinking(p0: DeepLinkResult) {
        when (p0.status) {
            DeepLinkResult.Status.FOUND -> {
                onDeepLinkFound(p0.deepLink)
            }
            DeepLinkResult.Status.NOT_FOUND -> {
                Timber.i("No deep link found")
            }
            DeepLinkResult.Status.ERROR -> {
                Timber.e("Deep link error: ${p0.error}")
            }
        }
    }

    private fun onDeepLinkFound(deepLink: DeepLink) {
        if (deepLink.deepLinkValue != REFERRAL_DEEP_LINK_VALUE) {
            Timber.i("Ignoring deep link with value: ${deepLink.deepLinkValue ?: "null"}")
            return
        }

        val refcode = deepLink.getStringValue(REFCODE_PARAM_KEY)
        val campaign = deepLink.getStringValue(CAMPAIGN_PARAM_KEY)

        @Suppress("NullableToStringCall")
        Timber.i("refcode=$refcode\ncampaign=$campaign")

        if (!isValidParam(refcode)) {
            Timber.e("Deep link conversion data is invalid")
            return
        }

        storeConversionData(refcode = refcode, campaign = campaign)
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
                appsFlyerConversionStore.store(
                    value = AppsFlyerConversionData(refcode = refcode, campaign = campaign),
                )
            }
        }
    }

    companion object Companion {

        @VisibleForTesting(otherwise = VisibleForTesting.NONE)
        const val REFERRAL_DEEP_LINK_VALUE = "referral"

        @VisibleForTesting(otherwise = VisibleForTesting.NONE)
        const val REFCODE_PARAM_KEY = "deep_link_sub1"

        @VisibleForTesting(otherwise = VisibleForTesting.NONE)
        const val CAMPAIGN_PARAM_KEY = "deep_link_sub2"
    }
}