package com.tangem.tap.common.analytics.appsflyer

import com.appsflyer.AppsFlyerConversionListener
import com.tangem.datasource.local.preferences.AppPreferencesStore
import javax.inject.Singleton

@Singleton
class TangemAppsFlyerConversionListener(
    private val appPreferencesStore: AppPreferencesStore,
): AppsFlyerConversionListener {

    override fun onConversionDataSuccess(p0: Map<String?, Any?>?) {
        TODO("Not yet implemented")
    }

    override fun onConversionDataFail(p0: String?) {
        TODO("Not yet implemented")
    }

    override fun onAppOpenAttribution(p0: Map<String?, String?>?) {
        TODO("Not yet implemented")
    }

    override fun onAttributionFailure(p0: String?) {
        TODO("Not yet implemented")
    }
}