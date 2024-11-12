package com.tangem.tap

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import com.chuckerteam.chucker.api.ChuckerInterceptor
import com.tangem.Log
import com.tangem.TangemSdkLogger
import com.tangem.blockchain.network.BlockchainSdkRetrofitBuilder
import com.tangem.blockchainsdk.BlockchainSDKFactory
import com.tangem.blockchainsdk.signer.TransactionSignerFactory
import com.tangem.common.routing.AppRouter
import com.tangem.core.analytics.Analytics
import com.tangem.core.analytics.api.ParamsInterceptor
import com.tangem.core.analytics.filter.OneTimeEventFilter
import com.tangem.core.analytics.models.AnalyticsEvent
import com.tangem.core.analytics.models.AnalyticsParam
import com.tangem.core.analytics.models.Basic
import com.tangem.core.analytics.models.Basic.TransactionSent.WalletForm
import com.tangem.core.featuretoggle.manager.FeatureTogglesManager
import com.tangem.datasource.api.common.MoshiConverter
import com.tangem.datasource.api.common.createNetworkLoggingInterceptor
import com.tangem.datasource.connection.NetworkConnectionManager
import com.tangem.datasource.local.config.environment.EnvironmentConfig
import com.tangem.datasource.local.config.environment.EnvironmentConfigStorage
import com.tangem.datasource.local.config.issuers.IssuersConfigStorage
import com.tangem.datasource.local.preferences.AppPreferencesStore
import com.tangem.domain.appcurrency.repository.AppCurrencyRepository
import com.tangem.domain.apptheme.GetAppThemeModeUseCase
import com.tangem.domain.apptheme.repository.AppThemeModeRepository
import com.tangem.domain.balancehiding.repositories.BalanceHidingRepository
import com.tangem.domain.card.ScanCardProcessor
import com.tangem.domain.card.repository.CardRepository
import com.tangem.domain.common.LogConfig
import com.tangem.domain.feedback.GetCardInfoUseCase
import com.tangem.domain.feedback.SendFeedbackEmailUseCase
import com.tangem.domain.onboarding.SaveTwinsOnboardingShownUseCase
import com.tangem.domain.onboarding.WasTwinsOnboardingShownUseCase
import com.tangem.domain.settings.repositories.SettingsRepository
import com.tangem.domain.settings.usercountry.GetUserCountryUseCase
import com.tangem.domain.walletmanager.WalletManagersFacade
import com.tangem.domain.wallets.legacy.UserWalletsListManager
import com.tangem.domain.wallets.repository.WalletsRepository
import com.tangem.domain.wallets.usecase.GenerateWalletNameUseCase
import com.tangem.features.onboarding.v2.OnboardingV2FeatureToggles
import com.tangem.features.onramp.OnrampFeatureToggles
import com.tangem.tap.common.analytics.AnalyticsFactory
import com.tangem.tap.common.analytics.api.AnalyticsHandlerBuilder
import com.tangem.tap.common.analytics.handlers.amplitude.AmplitudeAnalyticsHandler
import com.tangem.tap.common.analytics.handlers.firebase.FirebaseAnalyticsHandler
import com.tangem.tap.common.images.createCoilImageLoader
import com.tangem.tap.common.log.TangemAppLoggerInitializer
import com.tangem.tap.common.redux.AppState
import com.tangem.tap.common.redux.appReducer
import com.tangem.tap.domain.scanCard.CardScanningFeatureToggles
import com.tangem.tap.domain.tasks.product.DerivationsFinder
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

    private val environmentConfigStorage: EnvironmentConfigStorage
        get() = entryPoint.getEnvironmentConfigStorage()

    private val issuersConfigStorage: IssuersConfigStorage
        get() = entryPoint.getIssuersConfigStorage()

    private val featureTogglesManager: FeatureTogglesManager
        get() = entryPoint.getFeatureTogglesManager()

    private val networkConnectionManager: NetworkConnectionManager
        get() = entryPoint.getNetworkConnectionManager()

    private val cardScanningFeatureToggles: CardScanningFeatureToggles
        get() = entryPoint.getCardScanningFeatureToggles()

    private val walletConnect2Repository: WalletConnect2Repository
        get() = entryPoint.getWalletConnect2Repository()

    private val scanCardProcessor: ScanCardProcessor
        get() = entryPoint.getScanCardProcessor()

    private val appCurrencyRepository: AppCurrencyRepository
        get() = entryPoint.getAppCurrencyRepository()

    private val walletManagersFacade: WalletManagersFacade
        get() = entryPoint.getWalletManagersFacade()

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

    private val transactionSignerFactory: TransactionSignerFactory
        get() = entryPoint.getTransactionSignerFactory()

    private val homeFeatureToggles: HomeFeatureToggles
        get() = entryPoint.getHomeFeatureToggles()

    private val getUserCountryUseCase: GetUserCountryUseCase
        get() = entryPoint.getGetUserCountryCodeUseCase()

    private val onrampFeatureToggles: OnrampFeatureToggles
        get() = entryPoint.getOnrampFeatureToggles()

    private val onboardingV2FeatureToggles: OnboardingV2FeatureToggles
        get() = entryPoint.getOnboardingV2FeatureToggles()
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

        // TODO: Try to performance and user experience.
        //  [REDACTED_JIRA]
        runBlocking {
            featureTogglesManager.init()

            initWithConfigDependency(environmentConfig = environmentConfigStorage.initialize())
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

        walletConnect2Repository.init(projectId = environmentConfigStorage.getConfigSync().walletConnectProjectId)
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
                    scanCardProcessor = scanCardProcessor,
                    appCurrencyRepository = appCurrencyRepository,
                    walletManagersFacade = walletManagersFacade,
                    appStateHolder = appStateHolder,
                    appThemeModeRepository = appThemeModeRepository,
                    balanceHidingRepository = balanceHidingRepository,
                    walletsRepository = walletsRepository,
                    generalUserWalletsListManager = generalUserWalletsListManager,
                    wasTwinsOnboardingShownUseCase = wasTwinsOnboardingShownUseCase,
                    saveTwinsOnboardingShownUseCase = saveTwinsOnboardingShownUseCase,
                    generateWalletNameUseCase = generateWalletNameUseCase,
                    cardRepository = cardRepository,
                    settingsRepository = settingsRepository,
                    blockchainSDKFactory = blockchainSDKFactory,
                    sendFeedbackEmailUseCase = sendFeedbackEmailUseCase,
                    getCardInfoUseCase = getCardInfoUseCase,
                    issuersConfigStorage = issuersConfigStorage,
                    urlOpener = urlOpener,
                    shareManager = shareManager,
                    appRouter = appRouter,
                    transactionSignerFactory = transactionSignerFactory,
                    homeFeatureToggles = homeFeatureToggles,
                    getUserCountryUseCase = getUserCountryUseCase,
                    onrampFeatureToggles = onrampFeatureToggles,
                    environmentConfigStorage = environmentConfigStorage,
                    onboardingV2FeatureToggles = onboardingV2FeatureToggles,
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

    private fun initWithConfigDependency(environmentConfig: EnvironmentConfig) {
        initAnalytics(this, environmentConfig)
        Log.addLogger(logger = tangemSdkLogger)
    }

    private fun initAnalytics(application: Application, environmentConfig: EnvironmentConfig) {
        val factory = AnalyticsFactory()
        factory.addHandlerBuilder(AmplitudeAnalyticsHandler.Builder())
        factory.addHandlerBuilder(FirebaseAnalyticsHandler.Builder())

        factory.addFilter(oneTimeEventFilter)

        val buildData = AnalyticsHandlerBuilder.Data(
            application = application,
            config = environmentConfig,
            isDebug = BuildConfig.DEBUG,
            logConfig = LogConfig.analyticsHandlers,
            jsonConverter = MoshiConverter.sdkMoshiConverter,
        )

        Analytics.addParamsInterceptor(
            interceptor = object : ParamsInterceptor {
                override fun id(): String = "SendTransactionSignerInfoInterceptor"

                override fun canBeAppliedTo(event: AnalyticsEvent): Boolean = event is Basic.TransactionSent

                override fun intercept(params: MutableMap<String, String>) {
                    val isLastSignWithRing = store.state.globalState.isLastSignWithRing

                    params[AnalyticsParam.WALLET_FORM] = if (isLastSignWithRing) {
                        WalletForm.Ring.name
                    } else {
                        WalletForm.Card.name
                    }
                }
            },
        )

        factory.build(Analytics, buildData)
        // ExceptionHandler.append(blockchainExceptionHandler) TODO: [REDACTED_JIRA]
    }
}