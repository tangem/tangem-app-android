package com.tangem.tap

import android.app.Application
import com.tangem.tap.common.analytics.AnalyticsHandler
import com.tangem.tap.common.analytics.FirebaseAnalyticsHandler
import com.tangem.tap.common.images.PicassoHelper
import com.tangem.tap.common.redux.AppState
import com.tangem.tap.common.redux.appReducer
import com.tangem.tap.network.NetworkConnectivity
import com.tangem.tap.persistence.PreferencesStorage
import com.tangem.wallet.BuildConfig
import org.rekotlin.Store
import timber.log.Timber

val store = Store(
        reducer = ::appReducer,
        middleware = AppState.getMiddleware(),
        state = AppState()
)
lateinit var preferencesStorage: PreferencesStorage
lateinit var analytics: AnalyticsHandler

class TapApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
        NetworkConnectivity.createInstance(store, this)
        preferencesStorage = PreferencesStorage(this)
        PicassoHelper.initPicassoWithCaching(this)
        analytics = FirebaseAnalyticsHandler(this)
    }
}