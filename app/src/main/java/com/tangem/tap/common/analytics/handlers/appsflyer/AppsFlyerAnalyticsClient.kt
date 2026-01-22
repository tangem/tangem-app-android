package com.tangem.tap.common.analytics.handlers.appsflyer

import android.content.Context
import com.appsflyer.AppsFlyerLib
import com.appsflyer.attribution.AppsFlyerRequestListener
import com.tangem.core.analytics.api.EventLogger
import com.tangem.core.analytics.api.UserIdHolder
import com.tangem.tap.common.analytics.appsflyer.AppsFlyerDeepLinkListener
import com.tangem.tap.common.analytics.appsflyer.TangemAFConversionListener
import com.tangem.tap.common.analytics.handlers.firebase.UnderscoreAnalyticsEventConverter
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber

interface AppsFlyerAnalyticsClient : EventLogger, UserIdHolder

class AppsFlyerClient @AssistedInject constructor(
    @Assisted apiKey: String,
    @ApplicationContext private val context: Context,
    appsFlyerDeepLinkListener: AppsFlyerDeepLinkListener,
    tangemAFConversionListener: TangemAFConversionListener,
) : AppsFlyerAnalyticsClient {

    private val appsFlyerLib: AppsFlyerLib = AppsFlyerLib.getInstance()
    private val eventConverter = UnderscoreAnalyticsEventConverter()

    init {
        with(appsFlyerLib) {
            setAppId(context.packageName)
            setDebugLog(true)

            subscribeForDeepLink(appsFlyerDeepLinkListener)

            init(apiKey, tangemAFConversionListener, context)

            Timber.i("Starting AppsFlyer SDK")
            start(context, apiKey, InitializationListener)
            Timber.i("AppsFlyer SDK started")
        }
    }

    override fun setUserId(userId: String) {
        appsFlyerLib.setCustomerUserId(userId)
    }

    override fun clearUserId() {
        appsFlyerLib.setCustomerUserId(null)
    }

    override fun logEvent(event: String, params: Map<String, String>) {
        Timber.tag("AppsFlyer").i("Logging event: $event with params: $params")
        appsFlyerLib.logEvent(
            context,
            event,
            eventConverter.convertEventParams(params),
            LogEventListener,
        )
    }

    private object InitializationListener : AppsFlyerRequestListener {
        override fun onSuccess() {
            Timber.d("AppsFlyer initialized successfully")
        }

        override fun onError(p0: Int, p1: String) {
            Timber.e("AppsFlyer initialization error: $p0, $p1")
        }
    }

    private object LogEventListener : AppsFlyerRequestListener {
        override fun onSuccess() {
            Timber.tag("AppsFlyerClient").i("AppsFlyerRequestListener send")
        }

        override fun onError(p0: Int, p1: String) {
            Timber.tag("AppsFlyerClient").e("AppsFlyerRequestListener onError: $p0, $p1")
        }
    }

    @AssistedFactory
    interface Factory {
        fun create(apiKey: String): AppsFlyerClient
    }
}