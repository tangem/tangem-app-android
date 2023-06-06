package com.tangem.tap

import android.app.*
import android.content.*
import android.content.pm.*
import coil.*
import com.tangem.*
import com.tangem.blockchain.common.*
import com.tangem.blockchain.network.*
import com.tangem.core.analytics.*
import com.tangem.core.featuretoggle.manager.*
import com.tangem.data.source.preferences.*
import com.tangem.datasource.api.common.*
import com.tangem.datasource.asset.*
import com.tangem.datasource.config.*
import com.tangem.datasource.config.models.*
import com.tangem.datasource.connection.*
import com.tangem.domain.*
import com.tangem.domain.common.*
import com.tangem.features.wallet.featuretoggles.*
import com.tangem.tap.common.*
import com.tangem.tap.common.analytics.*
import com.tangem.tap.common.analytics.api.*
import com.tangem.tap.common.analytics.handlers.amplitude.*
import com.tangem.tap.common.analytics.handlers.appsFlyer.*
import com.tangem.tap.common.analytics.handlers.firebase.*
import com.tangem.tap.common.analytics.topup.*
import com.tangem.tap.common.chat.*
import com.tangem.tap.common.feedback.*
import com.tangem.tap.common.images.*
import com.tangem.tap.common.log.*
import com.tangem.tap.common.redux.*
import com.tangem.tap.common.redux.global.*
import com.tangem.tap.common.shop.*
import com.tangem.tap.domain.configurable.warningMessage.*
import com.tangem.tap.domain.tokens.*
import com.tangem.tap.domain.totalBalance.*
import com.tangem.tap.domain.totalBalance.di.*
import com.tangem.tap.domain.walletCurrencies.*
import com.tangem.tap.domain.walletCurrencies.di.*
import com.tangem.tap.domain.walletStores.*
import com.tangem.tap.domain.walletStores.di.*
import com.tangem.tap.domain.walletStores.repository.*
import com.tangem.tap.domain.walletStores.repository.di.*
import com.tangem.tap.domain.walletconnect.WalletConnectRepository
import com.tangem.tap.domain.walletconnect2.domain.*
import com.tangem.tap.features.customtoken.api.featuretoggles.*
import com.tangem.tap.proxy.*
import com.tangem.tap.proxy.redux.*
import com.tangem.wallet.BuildConfig
import dagger.hilt.android.*
import kotlinx.coroutines.*
import okhttp3.logging.*
import org.rekotlin.*
import timber.log.*
import javax.inject.*
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
                ),
            ),
        )

        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
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
                HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY },
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

        scope.launch {
            featureTogglesManager.init()
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
    }

    private fun initFeedbackManager(
        context: Context,
        preferencesStorage: PreferencesDataSource,
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