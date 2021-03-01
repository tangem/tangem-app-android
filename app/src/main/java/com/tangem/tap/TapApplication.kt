package com.tangem.tap

import android.app.Application
import com.google.firebase.ktx.Firebase
import com.google.firebase.remoteconfig.ktx.remoteConfig
import com.google.firebase.remoteconfig.ktx.remoteConfigSettings
import com.tangem.Log
import com.tangem.tap.common.images.PicassoHelper
import com.tangem.tap.common.redux.AppState
import com.tangem.tap.common.redux.appReducer
import com.tangem.tap.common.redux.global.GlobalAction
import com.tangem.tap.domain.configurable.config.ConfigManager
import com.tangem.tap.domain.configurable.config.FeaturesLocalLoader
import com.tangem.tap.domain.configurable.config.FeaturesRemoteLoader
import com.tangem.tap.domain.configurable.warningMessage.RemoteWarningLoader
import com.tangem.tap.domain.configurable.warningMessage.WarningMessagesManager
import com.tangem.tap.features.feedback.AdditionalEmailInfo
import com.tangem.tap.features.feedback.FeedbackManager
import com.tangem.tap.features.feedback.TangemLogCollector
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

        initFeedbackManager()
        loadConfigs()
    }

    private fun loadConfigs() {
        val moshi = createMoshi()
        val localLoader = FeaturesLocalLoader(this, moshi)
        val remoteLoader = FeaturesRemoteLoader(moshi)
        val configManager = ConfigManager(localLoader, remoteLoader)
        configManager.load { store.dispatch(GlobalAction.SetConfigManager(configManager)) }
        val warningsManager = WarningMessagesManager(RemoteWarningLoader(moshi))
        warningsManager.load { store.dispatch(GlobalAction.SetWarningManager(warningsManager)) }
    }

    private fun initFeedbackManager() {
        val infoHolder = AdditionalEmailInfo()
        infoHolder.updateAppVersion(this)

        val logWriter = TangemLogCollector()
        Log.addLogger(logWriter)

        val feedbackManager = FeedbackManager(infoHolder, this, logWriter)
        store.dispatch(GlobalAction.SetFeedbackManager(feedbackManager))
    }
}