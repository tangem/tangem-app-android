package com.tangem.tap.common.analytics.appsflyer

import android.content.Context
import com.appsflyer.AppsFlyerConversionListener
import com.appsflyer.AppsFlyerLib
import com.tangem.core.analytics.AppsflyerConversionHandler
import com.tangem.datasource.local.config.environment.EnvironmentConfig
import com.tangem.datasource.local.preferences.AppPreferencesStore
import javax.inject.Singleton

@Singleton
class DefaultAppsflyerConversionHandler(
    context: Context,
    environmentConfig: EnvironmentConfig,
    appsFlyerConversionListener: AppsFlyerConversionListener,
    private val appPreferencesStore: AppPreferencesStore,
) : AppsflyerConversionHandler {

    private val appsFlyerLib: AppsFlyerLib = AppsFlyerLib.getInstance()

    init {
        appsFlyerLib.init(environmentConfig.appsFlyerApiKey, appsFlyerConversionListener, context)
        appsFlyerLib.setAppId(environmentConfig.appsAppId)
        appsFlyerLib.start(context)
    }

    override fun getConversionParams(): Map<String, String> {
        TODO("Not yet implemented")
    }
}