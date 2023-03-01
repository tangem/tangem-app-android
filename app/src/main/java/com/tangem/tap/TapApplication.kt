package com.tangem.tap

import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import coil.ImageLoader
import coil.ImageLoaderFactory
import com.tangem.Log
import com.tangem.LogFormat
import com.tangem.blockchain.common.BlockchainSdkConfig
import com.tangem.blockchain.common.WalletManagerFactory
import com.tangem.blockchain.network.BlockchainSdkRetrofitBuilder
import com.tangem.core.analytics.Analytics
import com.tangem.core.featuretoggle.manager.FeatureTogglesManager
import com.tangem.datasource.api.common.MoshiConverter
import com.tangem.datasource.asset.AssetReader
import com.tangem.datasource.config.ConfigManager
import com.tangem.datasource.config.FeaturesLocalLoader
import com.tangem.datasource.config.models.Config
import com.tangem.domain.DomainLayer
import com.tangem.domain.common.LogConfig
import com.tangem.tap.common.IntentHandler
import com.tangem.tap.common.analytics.AnalyticsFactory
import com.tangem.tap.common.analytics.api.AnalyticsHandlerBuilder
import com.tangem.tap.common.analytics.filters.BasicSignInFilter
import com.tangem.tap.common.analytics.filters.BasicTopUpFilter
import com.tangem.tap.common.analytics.handlers.amplitude.AmplitudeAnalyticsHandler
import com.tangem.tap.common.analytics.handlers.appsFlyer.AppsFlyerAnalyticsHandler
import com.tangem.tap.common.analytics.handlers.firebase.FirebaseAnalyticsHandler
import com.tangem.tap.common.chat.ChatManager
import com.tangem.tap.common.feedback.AdditionalFeedbackInfo
import com.tangem.tap.common.feedback.FeedbackManager
import com.tangem.tap.common.images.createCoilImageLoader
import com.tangem.tap.common.log.TangemLogCollector
import com.tangem.tap.common.redux.AppState
import com.tangem.tap.common.redux.appReducer
import com.tangem.tap.common.redux.global.GlobalAction
import com.tangem.tap.common.shop.TangemShopService
import com.tangem.tap.domain.configurable.warningMessage.WarningMessagesManager
import com.tangem.tap.domain.tokens.UserTokensRepository
import com.tangem.tap.domain.totalBalance.TotalFiatBalanceCalculator
import com.tangem.tap.domain.totalBalance.di.provideDefaultImplementation
import com.tangem.tap.domain.walletCurrencies.WalletCurrenciesManager
import com.tangem.tap.domain.walletCurrencies.di.provideDefaultImplementation
import com.tangem.tap.domain.walletStores.WalletStoresManager
import com.tangem.tap.domain.walletStores.di.provideDefaultImplementation
import com.tangem.tap.domain.walletStores.repository.WalletAmountsRepository
import com.tangem.tap.domain.walletStores.repository.WalletManagersRepository
import com.tangem.tap.domain.walletStores.repository.WalletStoresRepository
import com.tangem.tap.domain.walletStores.repository.di.provideDefaultImplementation
import com.tangem.tap.domain.walletconnect.WalletConnectRepository
import com.tangem.tap.features.di.redux.DaggerGraphAction
import com.tangem.tap.domain.walletconnect2.data.WalletConnectRepositoryImpl
import com.tangem.tap.domain.walletconnect2.data.WalletConnectSessionsRepositoryImpl
import com.tangem.tap.domain.walletconnect2.domain.WcJrpcRequestsDeserializer
import com.tangem.tap.network.NetworkConnectivity
import com.tangem.tap.persistence.PreferencesStorage
import com.tangem.tap.proxy.AppStateHolder
import com.tangem.wallet.BuildConfig
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.launch
import okhttp3.logging.HttpLoggingInterceptor
import org.rekotlin.Store
import timber.log.Timber
import javax.inject.Inject

lateinit var store: Store<AppState>

lateinit var foregroundActivityObserver: ForegroundActivityObserver
lateinit var activityResultCaller: ActivityResultCaller
lateinit var preferencesStorage: PreferencesStorage
lateinit var walletConnectRepository: WalletConnectRepository
lateinit var shopService: TangemShopService
lateinit var userTokensRepository: UserTokensRepository
lateinit var walletConnect2Repository: com.tangem.tap.domain.walletconnect2.domain.WalletConnectRepository
lateinit var walletConnectSessionsRepository: WalletConnectSessionsRepositoryImpl

private val walletStoresRepository by lazy { WalletStoresRepository.provideDefaultImplementation() }
private val walletManagersRepository by lazy {
    WalletManagersRepository.provideDefaultImplementation(
        walletManagerFactory = WalletManagerFactory(
            config = store.state.globalState.configManager
                ?.config
                ?.blockchainSdkConfig
                ?: BlockchainSdkConfig(),
        ),
    )
}
private val walletAmountsRepository by lazy {
    WalletAmountsRepository.provideDefaultImplementation(
        tangemTechService = store.state.domainNetworks.tangemTechService,
    )
}
val walletStoresManager by lazy {
    WalletStoresManager.provideDefaultImplementation(
        userTokensRepository = userTokensRepository,
        walletStoresRepository = walletStoresRepository,
        walletManagersRepository = walletManagersRepository,
        walletAmountsRepository = walletAmountsRepository,
        appCurrencyProvider = { store.state.globalState.appCurrency },
    )
}
val walletCurrenciesManager by lazy {
    WalletCurrenciesManager.provideDefaultImplementation(
        userTokensRepository = userTokensRepository,
        walletStoresRepository = walletStoresRepository,
        walletManagersRepository = walletManagersRepository,
        walletAmountsRepository = walletAmountsRepository,
        appCurrencyProvider = { store.state.globalState.appCurrency },
    )
}
val totalFiatBalanceCalculator by lazy {
    TotalFiatBalanceCalculator.provideDefaultImplementation()
}
val intentHandler by lazy { IntentHandler() }

@HiltAndroidApp
class TapApplication : Application(), ImageLoaderFactory {

    @Inject
    lateinit var appStateHolder: AppStateHolder

    @Inject
    lateinit var configManager: ConfigManager

    @Inject
    lateinit var assetReader: AssetReader

    @Inject
    lateinit var featureTogglesManager: FeatureTogglesManager

    override fun onCreate() {
        super.onCreate()

        store = Store(
            reducer = { action, state ->
                appReducer(action, state, appStateHolder)
            },
            middleware = AppState.getMiddleware(),
            state = AppState(),
        )

        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }

        foregroundActivityObserver = ForegroundActivityObserver()
        activityResultCaller = foregroundActivityObserver
        registerActivityLifecycleCallbacks(foregroundActivityObserver.callbacks)

        DomainLayer.init()
        NetworkConnectivity.createInstance(store, this)
        preferencesStorage = PreferencesStorage(this)
        walletConnectRepository = WalletConnectRepository(this)

        val configLoader = FeaturesLocalLoader(assetReader, MoshiConverter.sdkMoshi, BuildConfig.ENVIRONMENT)
        initConfigManager(configLoader, ::initWithConfigDependency)
        initWarningMessagesManager()

        if (LogConfig.network.blockchainSdkNetwork) {
            BlockchainSdkRetrofitBuilder.interceptors = listOf(
                HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY },
            )
        }

        userTokensRepository = UserTokensRepository.init(
            context = this,
            tangemTechService = store.state.domainNetworks.tangemTechService,
        )
        appStateHolder.mainStore = store
        appStateHolder.userTokensRepository = userTokensRepository
        appStateHolder.walletStoresManager = walletStoresManager

        store.dispatch(DaggerGraphAction.SetDependencies(assetReader))

        scope.launch {
            featureTogglesManager.init()
        }

        walletConnect2Repository = WalletConnectRepositoryImpl(
            application = this,
            wcRequestDeserializer = WcJrpcRequestsDeserializer(MoshiConverter.sdkMoshi),
        )
        walletConnect2Repository.init("")
        walletConnectSessionsRepository = WalletConnectSessionsRepositoryImpl(context = this)
    }

    override fun newImageLoader(): ImageLoader {
        return createCoilImageLoader(
            context = this,
            logEnabled = LogConfig.imageLoader,
        )
    }

    private fun initConfigManager(loader: FeaturesLocalLoader, onComplete: (Config) -> Unit) {
        configManager.load(loader) { config ->
            store.dispatch(GlobalAction.SetConfigManager(configManager))
            onComplete(config)
        }
    }

    private fun initWithConfigDependency(config: Config) {
        shopService = TangemShopService(this, config.shopify!!)
        initAnalytics(this, config)
        initFeedbackManager(this, preferencesStorage, foregroundActivityObserver, store)
    }

    private fun initAnalytics(application: Application, config: Config) {
        val factory = AnalyticsFactory()
        factory.addHandlerBuilder(AmplitudeAnalyticsHandler.Builder())
        factory.addHandlerBuilder(AppsFlyerAnalyticsHandler.Builder())
        factory.addHandlerBuilder(FirebaseAnalyticsHandler.Builder())

        factory.addFilter(BasicSignInFilter())
        factory.addFilter(BasicTopUpFilter(preferencesStorage.toppedUpWalletStorage))

        val buildData = AnalyticsHandlerBuilder.Data(
            application = application,
            config = config,
            isDebug = BuildConfig.DEBUG,
            logConfig = LogConfig.analyticsHandlers,
            jsonConverter = MoshiConverter.sdkMoshiConverter,
        )
        factory.build(Analytics, buildData)
    }

    private fun initFeedbackManager(
        context: Context,
        preferencesStorage: PreferencesStorage,
        foregroundActivityObserver: ForegroundActivityObserver,
        store: Store<AppState>,
    ) {
        fun initAdditionalFeedbackInfo(context: Context): AdditionalFeedbackInfo = AdditionalFeedbackInfo().apply {
            appVersion = try {
                // TODO don't use deprecated method
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
            chatManager = ChatManager(preferencesStorage, foregroundActivityObserver, store),
        )
        store.dispatch(GlobalAction.SetFeedbackManager(feedbackManager))
    }

    private fun initWarningMessagesManager() {
        store.dispatch(GlobalAction.SetWarningManager(WarningMessagesManager()))
    }
}
