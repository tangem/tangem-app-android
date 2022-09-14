package com.tangem.tap

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import com.appsflyer.AppsFlyerLib
import com.google.firebase.ktx.Firebase
import com.google.firebase.remoteconfig.ktx.remoteConfig
import com.google.firebase.remoteconfig.ktx.remoteConfigSettings
import com.tangem.Log
import com.tangem.LogFormat
import com.tangem.blockchain.network.BlockchainSdkRetrofitBuilder
import com.tangem.domain.DomainLayer
import com.tangem.network.common.MoshiConverter
import com.tangem.tap.common.analytics.GlobalAnalyticsHandler
import com.tangem.tap.common.feedback.AdditionalFeedbackInfo
import com.tangem.tap.common.feedback.FeedbackManager
import com.tangem.tap.common.images.createCoilImageLoader
import com.tangem.tap.common.log.TangemLogCollector
import com.tangem.tap.common.redux.AppState
import com.tangem.tap.common.redux.appReducer
import com.tangem.tap.common.redux.global.GlobalAction
import com.tangem.tap.common.shop.TangemShopService
import com.tangem.tap.domain.configurable.config.ConfigManager
import com.tangem.tap.domain.configurable.config.FeaturesLocalLoader
import com.tangem.tap.domain.configurable.config.FeaturesRemoteLoader
import com.tangem.tap.domain.configurable.warningMessage.RemoteWarningLoader
import com.tangem.tap.domain.configurable.warningMessage.WarningMessagesManager
import com.tangem.tap.domain.tokens.CurrenciesRepository
import com.tangem.tap.domain.walletconnect.WalletConnectRepository
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

lateinit var foregroundActivityObserver: ForegroundActivityObserver

lateinit var preferencesStorage: PreferencesStorage
lateinit var currenciesRepository: CurrenciesRepository
lateinit var walletConnectRepository: WalletConnectRepository
lateinit var shopService: TangemShopService

class TapApplication : Application(), ImageLoaderFactory {
    override fun onCreate() {
        super.onCreate()

        DomainLayer.init()
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
        currenciesRepository = CurrenciesRepository(
            this, store.state.domainNetworks.tangemTechService
        )
        walletConnectRepository = WalletConnectRepository(this)

        foregroundActivityObserver = ForegroundActivityObserver()
        registerActivityLifecycleCallbacks(foregroundActivityObserver.callbacks)
        initFeedbackManager()
        loadConfigs()

        BlockchainSdkRetrofitBuilder.enableNetworkLogging =
            store.state.domainState.globalState.logConfig.network.blockchainSdkNetwork

        initAppsFlyer()
    }

    override fun newImageLoader(): ImageLoader {
        return createCoilImageLoader(
            context = this,
            logEnabled = store.state.domainState.globalState.logConfig.imageLoader
        )
    }

    private fun loadConfigs() {
        val moshi = MoshiConverter.defaultMoshi()
        val localLoader = FeaturesLocalLoader(this, moshi)
        val remoteLoader = FeaturesRemoteLoader(moshi)
        val configManager = ConfigManager(localLoader, remoteLoader)
        configManager.load { config ->
            store.dispatch(GlobalAction.SetConfigManager(configManager))
            shopService = TangemShopService(
                application = this,
                shopifyShop = config.shopify!!
            )
            store.state.globalState.feedbackManager?.initChat(
                context = this,
                zendeskConfig = config.zendesk!!
            )
        }
        val warningsManager = WarningMessagesManager(RemoteWarningLoader(moshi))
        warningsManager.load { store.dispatch(GlobalAction.SetWarningManager(warningsManager)) }
    }

    private fun initFeedbackManager() {
        val infoHolder = AdditionalFeedbackInfo()
        infoHolder.setAppVersion(this)

        val logLevels = listOf(
            Log.Level.ApduCommand,
            Log.Level.Apdu,
            Log.Level.Tlv,
            Log.Level.Nfc,
            Log.Level.Command,
            Log.Level.Session,
            Log.Level.View,
            Log.Level.Network,
            Log.Level.Error,
        )
        val logWriter = TangemLogCollector(
            levels = logLevels,
            messageFormatter = LogFormat.StairsFormatter(),
        )
        Log.addLogger(logWriter)

        store.dispatch(
            GlobalAction.SetFeedbackManager(
                FeedbackManager(
                    infoHolder = infoHolder,
                    logCollector = logWriter,
                    preferencesStorage = preferencesStorage,
                    logEnabled = store.state.domainState.globalState.logConfig.zendesk,
                ),
            ),
        )
    }

    private fun initAppsFlyer() {
        val devKey = store.state.globalState.configManager?.config?.appsFlyerDevKey ?: return
        AppsFlyerLib.getInstance().init(devKey, null, this)
        AppsFlyerLib.getInstance().start(this)
        val analyticsHandler = GlobalAnalyticsHandler.createDefaultAnalyticHandlers(this)
        store.dispatch(GlobalAction.SetAnanlyticHandlers(analyticsHandler))
    }
}