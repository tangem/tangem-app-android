package com.tangem.tap

import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import coil.ImageLoader
import coil.ImageLoaderFactory
import com.appsflyer.AppsFlyerLib
import com.tangem.Log
import com.tangem.LogFormat
import com.tangem.blockchain.network.BlockchainSdkRetrofitBuilder
import com.tangem.domain.DomainLayer
import com.tangem.network.common.MoshiConverter
import com.tangem.tap.common.AndroidAssetReader
import com.tangem.tap.common.analytics.GlobalAnalyticsHandler
import com.tangem.tap.common.feedback.AdditionalFeedbackInfo
import com.tangem.tap.common.feedback.FeedbackManager
import com.tangem.tap.common.images.createCoilImageLoader
import com.tangem.tap.common.log.TangemLogCollector
import com.tangem.tap.common.redux.AppState
import com.tangem.tap.common.redux.appReducer
import com.tangem.tap.common.redux.global.GlobalAction
import com.tangem.tap.common.shop.TangemShopService
import com.tangem.tap.domain.configurable.config.Config
import com.tangem.tap.domain.configurable.config.ConfigManager
import com.tangem.tap.domain.configurable.config.FeaturesLocalLoader
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
    state = AppState(),
)
val logConfig = LogConfig()

lateinit var foregroundActivityObserver: ForegroundActivityObserver

lateinit var preferencesStorage: PreferencesStorage
lateinit var currenciesRepository: CurrenciesRepository
lateinit var walletConnectRepository: WalletConnectRepository
lateinit var shopService: TangemShopService

class TapApplication : Application(), ImageLoaderFactory {

    override fun onCreate() {
        super.onCreate()

        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }

        foregroundActivityObserver = ForegroundActivityObserver()
        registerActivityLifecycleCallbacks(foregroundActivityObserver.callbacks)

        DomainLayer.init()
        NetworkConnectivity.createInstance(store, this)
        preferencesStorage = PreferencesStorage(this)
        currenciesRepository = CurrenciesRepository(this, store.state.domainNetworks.tangemTechService)
        walletConnectRepository = WalletConnectRepository(this)

        val configLoader = FeaturesLocalLoader(AndroidAssetReader(this), MoshiConverter.defaultMoshi())
        initConfigManager(configLoader) { config ->
            shopService = TangemShopService(this, config.shopify!!)
            initAppsFlyer(this, config)
            initFeedbackManager(this, preferencesStorage, config)
        }
        initWarningMessagesManager()

        BlockchainSdkRetrofitBuilder.enableNetworkLogging = BuildConfig.DEBUG
    }

    override fun newImageLoader(): ImageLoader {
        return createCoilImageLoader(context = this)
    }

    private fun initConfigManager(loader: FeaturesLocalLoader, onComplete: (Config) -> Unit) {
        val configManager = ConfigManager()
        configManager.load(loader) { config ->
            store.dispatch(GlobalAction.SetConfigManager(configManager))
            onComplete(config)
        }
    }

    private fun initAppsFlyer(context: Context, config: Config) {
        AppsFlyerLib.getInstance().init(config.appsFlyerDevKey, null, context)
        AppsFlyerLib.getInstance().start(context)
        val analyticsHandler = GlobalAnalyticsHandler.createDefaultAnalyticHandlers(context)
        store.dispatch(GlobalAction.SetAnanlyticHandlers(analyticsHandler))
    }

    private fun initFeedbackManager(context: Context, preferencesStorage: PreferencesStorage, config: Config) {
        fun initAdditionalFeedbackInfo(context: Context): AdditionalFeedbackInfo = AdditionalFeedbackInfo().apply {
            appVersion = try {
                val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
                pInfo.versionName
            } catch (e: PackageManager.NameNotFoundException) {
                e.printStackTrace()
                "x.y.z"
            }
        }

        fun initTangemLogCollector(): TangemLogCollector {
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
            return TangemLogCollector(logLevels, LogFormat.StairsFormatter())
        }

        val additionalFeedbackInfo = initAdditionalFeedbackInfo(context)
        val tangemLogCollector = initTangemLogCollector()
        Log.addLogger(tangemLogCollector)

        val feedbackManager = FeedbackManager(
            infoHolder = additionalFeedbackInfo,
            logCollector = tangemLogCollector,
            preferencesStorage = preferencesStorage,
        )
        feedbackManager.initChat(
            context = context,
            zendeskConfig = config.zendesk!!,
        )
        store.dispatch(GlobalAction.SetFeedbackManager(feedbackManager))
    }

    private fun initWarningMessagesManager() {
        store.dispatch(GlobalAction.SetWarningManager(WarningMessagesManager()))
    }
}

data class LogConfig(
    val coil: Boolean = BuildConfig.DEBUG,
    val storeAction: Boolean = BuildConfig.DEBUG,
    val zendesk: Boolean = BuildConfig.DEBUG,
)
