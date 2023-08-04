package com.tangem.tap

import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import coil.ImageLoader
import coil.ImageLoaderFactory
import com.orhanobut.logger.AndroidLogAdapter
import com.orhanobut.logger.Logger
import com.tangem.Log
import com.tangem.LogFormat
import com.tangem.blockchain.common.BlockchainSdkConfig
import com.tangem.blockchain.common.WalletManagerFactory
import com.tangem.blockchain.network.BlockchainSdkRetrofitBuilder
import com.tangem.core.analytics.Analytics
import com.tangem.core.featuretoggle.manager.FeatureTogglesManager
import com.tangem.data.source.preferences.PreferencesDataSource
import com.tangem.datasource.api.common.MoshiConverter
import com.tangem.datasource.api.common.createNetworkLoggingInterceptor
import com.tangem.datasource.asset.AssetReader
import com.tangem.datasource.config.ConfigManager
import com.tangem.datasource.config.FeaturesLocalLoader
import com.tangem.datasource.config.models.Config
import com.tangem.datasource.connection.NetworkConnectionManager
import com.tangem.domain.DomainLayer
import com.tangem.domain.card.ScanCardProcessor
import com.tangem.domain.common.LogConfig
import com.tangem.domain.wallets.legacy.WalletManagersRepository
import com.tangem.feature.learn2earn.domain.api.Learn2earnInteractor
import com.tangem.features.tokendetails.featuretoggles.TokenDetailsFeatureToggles
import com.tangem.features.wallet.featuretoggles.WalletFeatureToggles
import com.tangem.tap.common.analytics.AnalyticsFactory
import com.tangem.tap.common.analytics.api.AnalyticsHandlerBuilder
import com.tangem.tap.common.analytics.handlers.BlockchainExceptionHandler
import com.tangem.tap.common.analytics.handlers.amplitude.AmplitudeAnalyticsHandler
import com.tangem.tap.common.analytics.handlers.appsFlyer.AppsFlyerAnalyticsHandler
import com.tangem.tap.common.analytics.handlers.firebase.FirebaseAnalyticsHandler
import com.tangem.tap.common.analytics.topup.TopUpController
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
import com.tangem.tap.domain.walletStores.repository.WalletStoresRepository
import com.tangem.tap.domain.walletStores.repository.di.provideDefaultImplementation
import com.tangem.tap.domain.walletconnect.WalletConnectRepository
import com.tangem.tap.domain.walletconnect2.domain.WalletConnectSessionsRepository
import com.tangem.tap.features.customtoken.api.featuretoggles.CustomTokenFeatureToggles
import com.tangem.tap.proxy.AppStateHolder
import com.tangem.tap.proxy.redux.DaggerGraphState
import com.tangem.wallet.BuildConfig
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.runBlocking
import org.rekotlin.Store
import timber.log.Timber
import javax.inject.Inject
import com.tangem.tap.domain.walletconnect2.domain.WalletConnectRepository as WalletConnect2Repository

lateinit var store: Store<AppState>

lateinit var foregroundActivityObserver: ForegroundActivityObserver
lateinit var activityResultCaller: ActivityResultCaller
lateinit var preferencesStorage: PreferencesDataSource
lateinit var walletConnectRepository: WalletConnectRepository
lateinit var shopService: TangemShopService
lateinit var userTokensRepository: UserTokensRepository

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

    @Inject
    lateinit var networkConnectionManager: NetworkConnectionManager

    @Inject
    lateinit var customTokenFeatureToggles: CustomTokenFeatureToggles

    @Inject
    lateinit var preferencesDataSource: PreferencesDataSource

    @Inject
    lateinit var walletFeatureToggles: WalletFeatureToggles

    @Inject
    lateinit var walletConnect2Repository: WalletConnect2Repository

    @Inject
    lateinit var walletConnectSessionsRepository: WalletConnectSessionsRepository

    @Inject
    lateinit var learn2earnInteractor: Learn2earnInteractor

    @Inject
    lateinit var tokenDetailsFeatureToggles: TokenDetailsFeatureToggles

    @Inject
    lateinit var scanCardProcessor: ScanCardProcessor

    @Inject
    lateinit var blockchainExceptionHandler: BlockchainExceptionHandler

    override fun onCreate() {
        super.onCreate()

        store = Store(
            reducer = { action, state ->
                appReducer(action, state, appStateHolder)
            },
            middleware = AppState.getMiddleware(),
            state = AppState(
                daggerGraphState = DaggerGraphState(
                    assetReader = assetReader,
                    networkConnectionManager = networkConnectionManager,
                    customTokenFeatureToggles = customTokenFeatureToggles,
                    walletFeatureToggles = walletFeatureToggles,
                    walletConnectRepository = walletConnect2Repository,
                    walletConnectSessionsRepository = walletConnectSessionsRepository,
                    tokenDetailsFeatureToggles = tokenDetailsFeatureToggles,
                    scanCardProcessor = scanCardProcessor,
                ),
            ),
        )

        if (BuildConfig.DEBUG) {
            Logger.addLogAdapter(AndroidLogAdapter())
            Timber.plant(
                object : Timber.DebugTree() {
                    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
                        Logger.log(priority, tag, message, t)
                    }
                },
            )
        }

        foregroundActivityObserver = ForegroundActivityObserver()
        activityResultCaller = foregroundActivityObserver
        registerActivityLifecycleCallbacks(foregroundActivityObserver.callbacks)

        DomainLayer.init()
        preferencesStorage = preferencesDataSource
        walletConnectRepository = WalletConnectRepository(this)

        val configLoader = FeaturesLocalLoader(assetReader, MoshiConverter.sdkMoshi, BuildConfig.ENVIRONMENT)
        initConfigManager(configLoader, ::initWithConfigDependency)
        initWarningMessagesManager()

        loadNativeLibraries()

        if (LogConfig.network.blockchainSdkNetwork) {
            BlockchainSdkRetrofitBuilder.interceptors = listOf(
                createNetworkLoggingInterceptor(),
            )
        }

        userTokensRepository = UserTokensRepository.init(
            context = this,
            tangemTechService = store.state.domainNetworks.tangemTechService,
            networkConnectionManager = networkConnectionManager,
        )
        appStateHolder.mainStore = store
        appStateHolder.userTokensRepository = userTokensRepository
        appStateHolder.walletStoresManager = walletStoresManager
// [REDACTED_TODO_COMMENT]
// [REDACTED_JIRA]
        runBlocking {
            featureTogglesManager.init()
            learn2earnInteractor.init()
        }

        initTopUpController()
        walletConnect2Repository.init(projectId = configManager.config.walletConnectProjectId)
    }

    private fun initTopUpController() {
        val topUpController = TopUpController(
            scanResponseProvider = {
                store.state.globalState.scanResponse
                    ?: store.state.globalState.onboardingState.onboardingManager?.scanResponse
            },
            walletStoresManagerProvider = { walletStoresManager },
            topupWalletStorage = preferencesStorage.toppedUpWalletStorage,
        )
        store.dispatch(GlobalAction.SetTopUpController(topUpController))
    }

    override fun newImageLoader(): ImageLoader {
        return createCoilImageLoader(
            context = this,
            logEnabled = LogConfig.imageLoader,
        )
    }

    private fun loadNativeLibraries() {
        System.loadLibrary("TrustWalletCore")
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

        val buildData = AnalyticsHandlerBuilder.Data(
            application = application,
            config = config,
            isDebug = BuildConfig.DEBUG,
            logConfig = LogConfig.analyticsHandlers,
            jsonConverter = MoshiConverter.sdkMoshiConverter,
        )
        factory.build(Analytics, buildData)
        // ExceptionHandler.append(blockchainExceptionHandler) TODO: https://tangem.atlassian.net/browse/AND-4173
    }

    private fun initFeedbackManager(
        context: Context,
        preferencesStorage: PreferencesDataSource,
        foregroundActivityObserver: ForegroundActivityObserver,
        store: Store<AppState>,
    ) {
        fun initAdditionalFeedbackInfo(context: Context): AdditionalFeedbackInfo = AdditionalFeedbackInfo().apply {
            appVersion = try {
// [REDACTED_TODO_COMMENT]
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
