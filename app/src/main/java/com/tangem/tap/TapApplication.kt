package com.tangem.tap

import android.app.Application
import com.google.firebase.ktx.Firebase
import com.google.firebase.remoteconfig.ktx.remoteConfig
import com.google.firebase.remoteconfig.ktx.remoteConfigSettings
import com.tangem.tap.common.images.PicassoHelper
import com.tangem.tap.common.redux.AppState
import com.tangem.tap.common.redux.appReducer
import com.tangem.tap.common.redux.global.GlobalAction
import com.tangem.tap.domain.config.ConfigManager
import com.tangem.tap.domain.config.LocalLoader
import com.tangem.tap.domain.config.RemoteLoader
import com.tangem.tap.network.NetworkConnectivity
import com.tangem.tap.network.createMoshi
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

class TapApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
            Firebase.remoteConfig.setConfigSettingsAsync(remoteConfigSettings {
                this.minimumFetchIntervalInSeconds = 60
            })
        } else {
            Firebase.remoteConfig.setConfigSettingsAsync(remoteConfigSettings {
                this.minimumFetchIntervalInSeconds = 3600
            })
        }

        NetworkConnectivity.createInstance(store, this)
        preferencesStorage = PreferencesStorage(this)
        PicassoHelper.initPicassoWithCaching(this)

        loadConfigs()
    }


    private fun loadConfigs() {
        val moshi = createMoshi()
        val localLoader = LocalLoader(this, moshi)
        val remoteLoader = RemoteLoader(moshi)
        val configManager = ConfigManager(localLoader, remoteLoader)
        configManager.load { store.dispatch(GlobalAction.SetConfigManager(configManager)) }
    }
}