package com.tangem.tap.common.analytics.handlers.appsflyer

import android.content.Context
import com.appsflyer.AppsFlyerConversionListener
import com.appsflyer.AppsFlyerLib
import com.appsflyer.attribution.AppsFlyerRequestListener
import com.tangem.core.analytics.api.EventLogger
import com.tangem.core.analytics.api.UserIdHolder
import com.tangem.tap.common.analytics.handlers.firebase.UnderscoreAnalyticsEventConverter
import timber.log.Timber

interface AppsFlyerAnalyticsClient : EventLogger, UserIdHolder

internal class AppsFlyerClient(
    private val context: Context,
    key: String,
) : AppsFlyerAnalyticsClient {

    private val appsFlyerLib: AppsFlyerLib = AppsFlyerLib.getInstance()
    private val eventConverter = UnderscoreAnalyticsEventConverter()
    private val logEventListener = object : AppsFlyerRequestListener {
        override fun onSuccess() {
            Timber.tag("AppsFlyerClient").i("AppsFlyerRequestListener send")
        }

        override fun onError(p0: Int, p1: String) {
            Timber.tag("AppsFlyerClient").e("AppsFlyerRequestListener onError: $p0, $p1")
        }
    }

    init {
        val conversionListener = object : AppsFlyerConversionListener {
            override fun onConversionDataSuccess(p0: Map<String?, Any?>?) {}
            override fun onConversionDataFail(p0: String?) {}
            override fun onAppOpenAttribution(p0: Map<String?, String?>?) {}
            override fun onAttributionFailure(p0: String?) {}
        }

        appsFlyerLib.run {
            setAppId(context.packageName)
            setDebugLog(true)

            init(key, conversionListener, context)
            start(context, key, object : AppsFlyerRequestListener {
                override fun onSuccess() {
                    Timber.i("AppsFlyer initialized successfully")
                }

                override fun onError(p0: Int, p1: String) {
                    Timber.e("AppsFlyer initialization error: $p0, $p1")
                }
            })
        }
    }

    override fun setUserId(userId: String) {
        appsFlyerLib.setCustomerUserId(userId)
    }

    override fun clearUserId() {
        appsFlyerLib.setCustomerUserId(null)
    }

    override fun logEvent(event: String, params: Map<String, String>) {
        appsFlyerLib.logEvent(
            context,
            event,
            eventConverter.convertEventParams(params),
            logEventListener,
        )
    }
}