package com.tangem.tap

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import com.chuckerteam.chucker.api.ChuckerInterceptor
import com.tangem.Log
import com.tangem.TangemSdkLogger
import com.tangem.blockchain.network.BlockchainSdkRetrofitBuilder
import com.tangem.blockchainsdk.BlockchainSDKFactory
import com.tangem.common.routing.AppRouter
import com.tangem.core.analytics.Analytics
import com.tangem.core.analytics.filter.OneTimeEventFilter
import com.tangem.core.featuretoggle.manager.FeatureTogglesManager
import com.tangem.datasource.api.common.MoshiConverter
import com.tangem.datasource.api.common.createNetworkLoggingInterceptor
import com.tangem.datasource.asset.loader.AssetLoader
import com.tangem.datasource.config.ConfigManager
import com.tangem.datasource.config.models.Config
import com.tangem.datasource.connection.NetworkConnectionManager
import com.tangem.datasource.local.preferences.AppPreferencesStore
import com.tangem.domain.appcurrency.repository.AppCurrencyRepository
import com.tangem.domain.apptheme.GetAppThemeModeUseCase
import com.tangem.domain.apptheme.repository.AppThemeModeRepository
import com.tangem.domain.balancehiding.repositories.BalanceHidingRepository
import com.tangem.domain.card.ScanCardProcessor
import com.tangem.domain.card.repository.CardRepository
import com.tangem.domain.common.LogConfig
import com.tangem.domain.feedback.GetCardInfoUseCase
import com.tangem.domain.feedback.SaveBlockchainErrorUseCase
import com.tangem.domain.feedback.SendFeedbackEmailUseCase
import com.tangem.domain.onboarding.SaveTwinsOnboardingShownUseCase
import com.tangem.domain.onboarding.WasTwinsOnboardingShownUseCase
import com.tangem.domain.settings.repositories.SettingsRepository
import com.tangem.domain.settings.usercountry.GetUserCountryUseCase
import com.tangem.domain.tokens.repository.CurrenciesRepository
import com.tangem.domain.tokens.repository.NetworksRepository
import com.tangem.domain.walletmanager.WalletManagersFacade
import com.tangem.domain.wallets.legacy.UserWalletsListManager
import com.tangem.domain.wallets.repository.WalletsRepository
import com.tangem.domain.wallets.usecase.GenerateWalletNameUseCase
import com.tangem.tap.common.analytics.AnalyticsFactory
import com.tangem.tap.common.analytics.api.AnalyticsHandlerBuilder
import com.tangem.tap.common.analytics.handlers.amplitude.AmplitudeAnalyticsHandler
import com.tangem.tap.common.analytics.handlers.firebase.FirebaseAnalyticsHandler
import com.tangem.tap.common.images.createCoilImageLoader
import com.tangem.tap.common.log.TangemAppLoggerInitializer
import com.tangem.tap.common.redux.AppState
import com.tangem.tap.common.redux.appReducer
import com.tangem.tap.common.redux.global.GlobalAction
import com.tangem.tap.domain.scanCard.CardScanningFeatureToggles
import com.tangem.tap.domain.tasks.product.DerivationsFinder
import com.tangem.tap.domain.walletconnect2.domain.WalletConnectSessionsRepository
import com.tangem.tap.features.home.featuretoggles.HomeFeatureToggles
import com.tangem.tap.proxy.AppStateHolder
import com.tangem.tap.proxy.redux.DaggerGraphState
import com.tangem.utils.coroutines.AppCoroutineDispatcherProvider
import com.tangem.wallet.BuildConfig
import dagger.hilt.EntryPoints
import kotlinx.coroutines.runBlocking
import org.rekotlin.Store
import com.tangem.tap.domain.walletconnect2.domain.LegacyWalletConnectRepository as WalletConnect2Repository

lateinit var store: Store<AppState>

lateinit var foregroundActivityObserver: ForegroundActivityObserver
lateinit var activityResultCaller: ActivityResultCaller
internal lateinit var derivationsFinder: DerivationsFinder

abstract class TangemApplication : Application(), ImageLoaderFactory {

    // region DI
    private val entryPoint: ApplicationEntryPoint
        get() = EntryPoints.get(this, ApplicationEntryPoint::class.java)

    private val appStateHolder: AppStateHolder
        get() = entryPoint.getAppStateHolder()

    private val configManager: ConfigManager
        get() = entryPoint.getConfigManager()

    private val assetLoader: AssetLoader
        get() = entryPoint.getAssetLoader()

    private val featureTogglesManager: FeatureTogglesManager
        get() = entryPoint.getFeatureTogglesManager()

    private val networkConnectionManager: NetworkConnectionManager
        get() = entryPoint.getNetworkConnectionManager()

    private val cardScanningFeatureToggles: CardScanningFeatureToggles
        get() = entryPoint.getCardScanningFeatureToggles()

    private val walletConnect2Repository: WalletConnect2Repository
        get() = entryPoint.getWalletConnect2Repository()

    private val walletConnectSessionsRepository: WalletConnectSessionsRepository
        get() = entryPoint.getWalletConnectSessionsRepository()

    private val scanCardProcessor: ScanCardProcessor
        get() = entryPoint.getScanCardProcessor()

    private val appCurrencyRepository: AppCurrencyRepository
        get() = entryPoint.getAppCurrencyRepository()

    private val walletManagersFacade: WalletManagersFacade
        get() = entryPoint.getWalletManagersFacade()

    private val networksRepository: NetworksRepository
        get() = entryPoint.getNetworksRepository()

    private val currenciesRepository: CurrenciesRepository
        get() = entryPoint.getCurrenciesRepository()

    private val appThemeModeRepository: AppThemeModeRepository
        get() = entryPoint.getAppThemeModeRepository()

    private val balanceHidingRepository: BalanceHidingRepository
        get() = entryPoint.getBalanceHidingRepository()

    private val appPreferencesStore: AppPreferencesStore
        get() = entryPoint.getAppPreferencesStore()

    val getAppThemeModeUseCase: GetAppThemeModeUseCase
        get() = entryPoint.getGetAppThemeModeUseCase()

    private val walletsRepository: WalletsRepository
        get() = entryPoint.getWalletsRepository()

    private val oneTimeEventFilter: OneTimeEventFilter
        get() = entryPoint.getOneTimeEventFilter()

    private val generalUserWalletsListManager: UserWalletsListManager
        get() = entryPoint.getGeneralUserWalletsListManager()

    private val wasTwinsOnboardingShownUseCase: WasTwinsOnboardingShownUseCase
        get() = entryPoint.getWasTwinsOnboardingShownUseCase()

    private val saveTwinsOnboardingShownUseCase: SaveTwinsOnboardingShownUseCase
        get() = entryPoint.getSaveTwinsOnboardingShownUseCase()

    private val generateWalletNameUseCase: GenerateWalletNameUseCase
        get() = entryPoint.getWalletNameGenerateUseCase()

    private val cardRepository: CardRepository
        get() = entryPoint.getCardRepository()

    private val tangemSdkLogger: TangemSdkLogger
        get() = entryPoint.getTangemSdkLogger()

    private val settingsRepository: SettingsRepository
        get() = entryPoint.getSettingsRepository()

    private val blockchainSDKFactory: BlockchainSDKFactory
        get() = entryPoint.getBlockchainSDKFactory()

    private val sendFeedbackEmailUseCase: SendFeedbackEmailUseCase
        get() = entryPoint.getSendFeedbackEmailUseCase()

    private val saveBlockchainErrorUseCase: SaveBlockchainErrorUseCase
        get() = entryPoint.getSaveBlockchainErrorUseCase()

    private val getCardInfoUseCase: GetCardInfoUseCase
        get() = entryPoint.getGetCardInfoUseCase()

    private val urlOpener
        get() = entryPoint.getUrlOpener()

    private val shareManager
        get() = entryPoint.getShareManager()

    private val appRouter: AppRouter
        get() = entryPoint.getAppRouter()

    private val tangemAppLoggerInitializer: TangemAppLoggerInitializer
        get() = entryPoint.getTangemAppLogger()

    private val homeFeatureToggles: HomeFeatureToggles
        get() = entryPoint.getHomeFeatureToggles()

    private val getUserCountryUseCase: GetUserCountryUseCase
        get() = entryPoint.getGetUserCountryCodeUseCase()
    // endregion

    override fun onCreate() {
        super.onCreate()

        init()
    }

    fun init() {
        store = createReduxStore()

        tangemAppLoggerInitializer.initialize()

        foregroundActivityObserver = ForegroundActivityObserver()
        activityResultCaller = foregroundActivityObserver
        registerActivityLifecycleCallbacks(foregroundActivityObserver.callbacks)
// [REDACTED_TODO_COMMENT]
// [REDACTED_JIRA]
        runBlocking {
            featureTogglesManager.init()

            val config = configManager.initialize()
            store.dispatch(GlobalAction.SetConfigManager(configManager))

            initWithConfigDependency(config = config)
        }

        loadNativeLibraries()

        if (LogConfig.network.blockchainSdkNetwork) {
            BlockchainSdkRetrofitBuilder.interceptors = listOf(
                createNetworkLoggingInterceptor(),
                ChuckerInterceptor(this),
            )
        }

        derivationsFinder = DerivationsFinder(
            appPreferencesStore = appPreferencesStore,
            dispatchers = AppCoroutineDispatcherProvider(),
        )
        appStateHolder.mainStore = store

        walletConnect2Repository.init(projectId = configManager.getConfigSync().walletConnectProjectId)
    }

    private fun createReduxStore(): Store<AppState> {
        return Store(
            reducer = { action, state -> appReducer(action, state, appStateHolder) },
            middleware = AppState.getMiddleware(),
            state = AppState(
                daggerGraphState = DaggerGraphState(
                    networkConnectionManager = networkConnectionManager,
                    cardScanningFeatureToggles = cardScanningFeatureToggles,
                    walletConnectRepository = walletConnect2Repository,
                    walletConnectSessionsRepository = walletConnectSessionsRepository,
                    scanCardProcessor = scanCardProcessor,
                    appCurrencyRepository = appCurrencyRepository,
                    walletManagersFacade = walletManagersFacade,
                    appStateHolder = appStateHolder,
                    networksRepository = networksRepository,
                    currenciesRepository = currenciesRepository,
                    appThemeModeRepository = appThemeModeRepository,
                    balanceHidingRepository = balanceHidingRepository,
                    walletsRepository = walletsRepository,
                    generalUserWalletsListManager = generalUserWalletsListManager,
                    wasTwinsOnboardingShownUseCase = wasTwinsOnboardingShownUseCase,
                    saveTwinsOnboardingShownUseCase = saveTwinsOnboardingShownUseCase,
                    generateWalletNameUseCase = generateWalletNameUseCase,
                    cardRepository = cardRepository,
                    tangemSdkLogger = tangemSdkLogger,
                    settingsRepository = settingsRepository,
                    blockchainSDKFactory = blockchainSDKFactory,
                    saveBlockchainErrorUseCase = saveBlockchainErrorUseCase,
                    sendFeedbackEmailUseCase = sendFeedbackEmailUseCase,
                    getCardInfoUseCase = getCardInfoUseCase,
                    assetLoader = assetLoader,
                    urlOpener = urlOpener,
                    shareManager = shareManager,
                    appRouter = appRouter,
                    homeFeatureToggles = homeFeatureToggles,
                    getUserCountryUseCase = getUserCountryUseCase,
                ),
            ),
        )
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

    private fun initWithConfigDependency(config: Config) {
        initAnalytics(this, config)
        Log.addLogger(logger = tangemSdkLogger)
    }

    private fun initAnalytics(application: Application, config: Config) {
        val factory = AnalyticsFactory()
        factory.addHandlerBuilder(AmplitudeAnalyticsHandler.Builder())
        factory.addHandlerBuilder(FirebaseAnalyticsHandler.Builder())

        factory.addFilter(oneTimeEventFilter)

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
}
