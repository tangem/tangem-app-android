package com.tangem.tap.common.analytics.appsflyer

import android.content.Context
import com.appsflyer.AppsFlyerConversionListener
import com.appsflyer.AppsFlyerLib
import com.tangem.core.analytics.AppsflyerConversionHandler
import com.tangem.datasource.local.config.environment.EnvironmentConfigStorage
import com.tangem.datasource.local.preferences.AppPreferencesStore
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Singleton

@Singleton
class DefaultAppsflyerConversionHandler(
    context: Context,
    environmentConfigStorage: EnvironmentConfigStorage,
    private val appPreferencesStore: AppPreferencesStore,
    dispatchersProvider: CoroutineDispatcherProvider,
) : AppsflyerConversionHandler {

    private val appsFlyerLib: AppsFlyerLib = AppsFlyerLib.getInstance()
    private val coroutineScope = CoroutineScope(dispatchersProvider.io + SupervisorJob())

    init {
        coroutineScope.launch {
            val conversionListener = TangemAppsFlyerConversionListener(appPreferencesStore)
            val environmentConfig = environmentConfigStorage.getConfigSync()
            appsFlyerLib.init(environmentConfig.appsFlyerApiKey, conversionListener, context)
            appsFlyerLib.setAppId(environmentConfig.appsAppId)
            appsFlyerLib.start(context)
        }
    }

    override fun getConversionParams(): Map<String, String> {
        TODO("Not yet implemented")
    }

    class TangemAppsFlyerConversionListener(
        private val appPreferencesStore: AppPreferencesStore,
    ) : AppsFlyerConversionListener {

        override fun onConversionDataSuccess(p0: Map<String?, Any?>?) {
            Timber.tag("appsflyer_test").e(p0.toString())
        }

        override fun onConversionDataFail(p0: String?) {
            Timber.tag("appsflyer_test").e(p0)
        }

        override fun onAppOpenAttribution(p0: Map<String?, String?>?) {
            Timber.tag("appsflyer_test").e(p0.toString())
        }

        override fun onAttributionFailure(p0: String?) {
            Timber.tag("appsflyer_test").e(p0)
        }
    }
}