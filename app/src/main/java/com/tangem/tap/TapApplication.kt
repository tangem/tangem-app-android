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
import com.tangem.domain.common.LogConfig
import com.tangem.network.common.MoshiConverter
import com.tangem.tap.common.AndroidAssetReader
import com.tangem.tap.common.AssetReader
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
import com.tangem.tap.domain.tokens.UserTokensRepository
import com.tangem.tap.domain.walletconnect.WalletConnectRepository
import com.tangem.tap.network.NetworkConnectivity
import com.tangem.tap.persistence.PreferencesStorage
import com.tangem.wallet.BuildConfig
import com.zendesk.logger.Logger
import org.rekotlin.Store
import timber.log.Timber
import zendesk.chat.Chat

val store = Store(
    reducer = ::appReducer,
    middleware = AppState.getMiddleware(),
    state = AppState(),
)

lateinit var foregroundActivityObserver: ForegroundActivityObserver
lateinit var preferencesStorage: PreferencesStorage
lateinit var walletConnectRepository: WalletConnectRepository
lateinit var shopService: TangemShopService
lateinit var assetReader: AssetReader
lateinit var userTokensRepository: UserTokensRepository

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
        walletConnectRepository = WalletConnectRepository(this)

        assetReader = AndroidAssetReader(this)
        val configLoader = FeaturesLocalLoader(assetReader, MoshiConverter.defaultMoshi())
        initConfigManager(configLoader, ::initWithConfigDependency)
        initWarningMessagesManager()

        BlockchainSdkRetrofitBuilder.enableNetworkLogging = LogConfig.network.blockchainSdkNetwork

        userTokensRepository = UserTokensRepository.init(
            context = this,
            tangemTechService = store.state.domainNetworks.tangemTechService,
        )
    }

    override fun newImageLoader(): ImageLoader {
        return createCoilImageLoader(
            context = this,
            logEnabled = LogConfig.imageLoader,
        )
    }

    private fun initConfigManager(loader: FeaturesLocalLoader, onComplete: (Config) -> Unit) {
        val configManager = ConfigManager()
        configManager.load(loader) { config ->
            store.dispatch(GlobalAction.SetConfigManager(configManager))
            onComplete(config)
        }
    }

    private fun initWithConfigDependency(config: Config) {
        shopService = TangemShopService(this, config.shopify!!)
        initAppsFlyer(this, config)
        initFeedbackManager(this, preferencesStorage, config)
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
        feedbackManager.chatInitializer = { zendeskConfig ->
            Chat.INSTANCE.init(context, zendeskConfig.accountKey, zendeskConfig.appId)
            Logger.setLoggable(LogConfig.zendesk)
        }
        store.dispatch(GlobalAction.SetFeedbackManager(feedbackManager))
    }

    private fun initWarningMessagesManager() {
        store.dispatch(GlobalAction.SetWarningManager(WarningMessagesManager()))
    }
}