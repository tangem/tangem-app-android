package com.tangem.tap

import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import coil.ImageLoader
import coil.ImageLoaderFactory
import com.chuckerteam.chucker.api.ChuckerInterceptor
import com.orhanobut.logger.AndroidLogAdapter
import com.orhanobut.logger.Logger
import com.tangem.Log
import com.tangem.LogFormat
import com.tangem.TangemSdkLogger
import com.tangem.blockchain.network.BlockchainSdkRetrofitBuilder
import com.tangem.blockchainsdk.BlockchainSDKFactory
import com.tangem.core.analytics.Analytics
import com.tangem.core.analytics.filter.OneTimeEventFilter
import com.tangem.core.featuretoggle.manager.FeatureTogglesManager
import com.tangem.data.source.preferences.PreferencesDataSource
import com.tangem.datasource.api.common.MoshiConverter
import com.tangem.datasource.api.common.createNetworkLoggingInterceptor
import com.tangem.datasource.asset.AssetReader
import com.tangem.datasource.config.ConfigManager
import com.tangem.datasource.config.FeaturesLocalLoader
import com.tangem.datasource.config.models.Config
import com.tangem.datasource.connection.NetworkConnectionManager
import com.tangem.datasource.local.token.UserTokensStore
import com.tangem.domain.appcurrency.repository.AppCurrencyRepository
import com.tangem.domain.apptheme.GetAppThemeModeUseCase
import com.tangem.domain.apptheme.repository.AppThemeModeRepository
import com.tangem.domain.balancehiding.repositories.BalanceHidingRepository
import com.tangem.domain.card.ScanCardProcessor
import com.tangem.domain.card.repository.CardRepository
import com.tangem.domain.common.LogConfig
import com.tangem.domain.feedback.FeedbackManagerFeatureToggles
import com.tangem.domain.onboarding.SaveTwinsOnboardingShownUseCase
import com.tangem.domain.onboarding.WasTwinsOnboardingShownUseCase
import com.tangem.domain.settings.repositories.SettingsRepository
import com.tangem.domain.tokens.repository.CurrenciesRepository
import com.tangem.domain.tokens.repository.NetworksRepository
import com.tangem.domain.walletmanager.WalletManagersFacade
import com.tangem.domain.wallets.legacy.UserWalletsListManager
import com.tangem.domain.wallets.legacy.UserWalletsListManagerFeatureToggles
import com.tangem.domain.wallets.repository.WalletsRepository
import com.tangem.features.managetokens.featuretoggles.ManageTokensFeatureToggles
import com.tangem.features.send.api.featuretoggles.SendFeatureToggles
import com.tangem.tap.common.analytics.AnalyticsFactory
import com.tangem.tap.common.analytics.api.AnalyticsHandlerBuilder
import com.tangem.tap.common.analytics.handlers.amplitude.AmplitudeAnalyticsHandler
import com.tangem.tap.common.analytics.handlers.appsFlyer.AppsFlyerAnalyticsHandler
import com.tangem.tap.common.analytics.handlers.firebase.FirebaseAnalyticsHandler
import com.tangem.tap.common.chat.ChatManager
import com.tangem.tap.common.feedback.AdditionalFeedbackInfo
import com.tangem.tap.common.feedback.FeedbackManager
import com.tangem.tap.common.images.createCoilImageLoader
import com.tangem.tap.common.log.TangemLogCollector
import com.tangem.tap.common.log.TimberFormatStrategy
import com.tangem.tap.common.redux.AppState
import com.tangem.tap.common.redux.appReducer
import com.tangem.tap.common.redux.global.GlobalAction
import com.tangem.tap.domain.configurable.warningMessage.WarningMessagesManager
import com.tangem.tap.domain.tasks.product.DerivationsFinder
import com.tangem.tap.domain.userWalletList.di.provideBiometricImplementation
import com.tangem.tap.domain.userWalletList.di.provideRuntimeImplementation
import com.tangem.tap.domain.walletconnect.WalletConnectRepository
import com.tangem.tap.domain.walletconnect2.domain.WalletConnectSessionsRepository
import com.tangem.tap.features.customtoken.api.featuretoggles.CustomTokenFeatureToggles
import com.tangem.tap.proxy.AppStateHolder
import com.tangem.tap.proxy.redux.DaggerGraphState
import com.tangem.utils.coroutines.AppCoroutineDispatcherProvider
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
internal lateinit var derivationsFinder: DerivationsFinder

@HiltAndroidApp
internal class TapApplication : Application(), ImageLoaderFactory {

    // region Injected
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
    lateinit var walletConnect2Repository: WalletConnect2Repository

    @Inject
    lateinit var walletConnectSessionsRepository: WalletConnectSessionsRepository

    @Inject
    lateinit var manageTokensFeatureToggles: ManageTokensFeatureToggles

    @Inject
    lateinit var scanCardProcessor: ScanCardProcessor

    @Inject
    lateinit var appCurrencyRepository: AppCurrencyRepository

    @Inject
    lateinit var walletManagersFacade: WalletManagersFacade

    @Inject
    lateinit var networksRepository: NetworksRepository

    @Inject
    lateinit var currenciesRepository: CurrenciesRepository

    @Inject
    lateinit var appThemeModeRepository: AppThemeModeRepository

    @Inject
    lateinit var balanceHidingRepository: BalanceHidingRepository

    @Inject
    lateinit var userTokensStore: UserTokensStore

    @Inject
    lateinit var getAppThemeModeUseCase: GetAppThemeModeUseCase

    @Inject
    lateinit var walletsRepository: WalletsRepository

    @Inject
    lateinit var sendFeatureToggles: SendFeatureToggles

    @Inject
    lateinit var oneTimeEventFilter: OneTimeEventFilter

    @Inject
    lateinit var userWalletsListManagerFeatureToggles: UserWalletsListManagerFeatureToggles

    @Inject
    lateinit var generalUserWalletsListManager: UserWalletsListManager

    @Inject
    lateinit var wasTwinsOnboardingShownUseCase: WasTwinsOnboardingShownUseCase

    @Inject
    lateinit var saveTwinsOnboardingShownUseCase: SaveTwinsOnboardingShownUseCase

    @Inject
    lateinit var cardRepository: CardRepository

    @Inject
    lateinit var feedbackManagerFeatureToggles: FeedbackManagerFeatureToggles

    @Inject
    lateinit var tangemSdkLogger: TangemSdkLogger

    @Inject
    lateinit var settingsRepository: SettingsRepository

    @Inject
    lateinit var blockchainSDKFactory: BlockchainSDKFactory
    // endregion Injected

    override fun onCreate() {
        super.onCreate()

        store = createReduxStore()

        if (BuildConfig.LOG_ENABLED) {
            Logger.addLogAdapter(AndroidLogAdapter(TimberFormatStrategy()))
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

        preferencesStorage = preferencesDataSource
        walletConnectRepository = WalletConnectRepository(this)
// [REDACTED_TODO_COMMENT]
// [REDACTED_JIRA]
        runBlocking {
            featureTogglesManager.init()

            if (userWalletsListManagerFeatureToggles.isGeneralManagerEnabled) {
                store.dispatch(GlobalAction.UpdateUserWalletsListManager(generalUserWalletsListManager))
            } else {
                initUserWalletsListManager()
            }

            blockchainSDKFactory.init()
        }

        val configLoader = FeaturesLocalLoader(assetReader, MoshiConverter.sdkMoshi, BuildConfig.ENVIRONMENT)
        initConfigManager(configLoader, ::initWithConfigDependency)
        initWarningMessagesManager()

        loadNativeLibraries()

        if (LogConfig.network.blockchainSdkNetwork) {
            BlockchainSdkRetrofitBuilder.interceptors = listOf(
                createNetworkLoggingInterceptor(),
                ChuckerInterceptor(this),
            )
        }

        derivationsFinder = DerivationsFinder(
            newTokensStore = userTokensStore,
            dispatchers = AppCoroutineDispatcherProvider(),
        )
        appStateHolder.mainStore = store

        walletConnect2Repository.init(projectId = configManager.config.walletConnectProjectId)
    }

    private fun createReduxStore(): Store<AppState> {
        return Store(
            reducer = { action, state -> appReducer(action, state, appStateHolder) },
            middleware = AppState.getMiddleware(),
            state = AppState(
                daggerGraphState = DaggerGraphState(
                    networkConnectionManager = networkConnectionManager,
                    customTokenFeatureToggles = customTokenFeatureToggles,
                    walletConnectRepository = walletConnect2Repository,
                    walletConnectSessionsRepository = walletConnectSessionsRepository,
                    manageTokensFeatureToggles = manageTokensFeatureToggles,
                    scanCardProcessor = scanCardProcessor,
                    appCurrencyRepository = appCurrencyRepository,
                    walletManagersFacade = walletManagersFacade,
                    appStateHolder = appStateHolder,
                    networksRepository = networksRepository,
                    currenciesRepository = currenciesRepository,
                    appThemeModeRepository = appThemeModeRepository,
                    balanceHidingRepository = balanceHidingRepository,
                    walletsRepository = walletsRepository,
                    sendFeatureToggles = sendFeatureToggles,
                    userWalletsListManagerFeatureToggles = userWalletsListManagerFeatureToggles,
                    generalUserWalletsListManager = generalUserWalletsListManager,
                    wasTwinsOnboardingShownUseCase = wasTwinsOnboardingShownUseCase,
                    saveTwinsOnboardingShownUseCase = saveTwinsOnboardingShownUseCase,
                    cardRepository = cardRepository,
                    feedbackManagerFeatureToggles = feedbackManagerFeatureToggles,
                    tangemSdkLogger = tangemSdkLogger,
                    settingsRepository = settingsRepository,
                    blockchainSDKFactory = blockchainSDKFactory,
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

    private fun initConfigManager(loader: FeaturesLocalLoader, onComplete: (Config) -> Unit) {
        configManager.load(loader) { config ->
            store.dispatch(GlobalAction.SetConfigManager(configManager))
            onComplete(config)
        }
    }

    private fun initWithConfigDependency(config: Config) {
        initAnalytics(this, config)
        initFeedbackManager(this, foregroundActivityObserver, store)
    }

    private fun initAnalytics(application: Application, config: Config) {
        val factory = AnalyticsFactory()
        factory.addHandlerBuilder(AmplitudeAnalyticsHandler.Builder())
        factory.addHandlerBuilder(AppsFlyerAnalyticsHandler.Builder())
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

    private fun initFeedbackManager(
        context: Context,
        foregroundActivityObserver: ForegroundActivityObserver,
        store: Store<AppState>,
    ) {
        fun initAdditionalFeedbackInfo(context: Context): AdditionalFeedbackInfo {
            return AdditionalFeedbackInfo().apply {
                appVersion = try {
// [REDACTED_TODO_COMMENT]
                    val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
                    pInfo.versionName
                } catch (e: PackageManager.NameNotFoundException) {
                    e.printStackTrace()
                    "x.y.z"
                }
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
                Log.Level.Biometric,
            )
            return TangemLogCollector(logLevels, LogFormat.StairsFormatter())
        }

        val additionalFeedbackInfo = initAdditionalFeedbackInfo(context)
        val tangemLogCollector = initTangemLogCollector()

        Log.addLogger(
            logger = if (feedbackManagerFeatureToggles.isLocalLogsEnabled) tangemSdkLogger else tangemLogCollector,
        )

        val feedbackManager = FeedbackManager(
            infoHolder = additionalFeedbackInfo,
            logCollector = tangemLogCollector,
            chatManager = ChatManager(foregroundActivityObserver),
        )
        store.dispatch(GlobalAction.SetFeedbackManager(feedbackManager))
    }

    private fun initWarningMessagesManager() {
        store.dispatch(GlobalAction.SetWarningManager(WarningMessagesManager()))
    }

    private suspend fun initUserWalletsListManager() {
        val manager = if (walletsRepository.shouldSaveUserWalletsSync()) {
            UserWalletsListManager.provideBiometricImplementation(applicationContext)
        } else {
            UserWalletsListManager.provideRuntimeImplementation()
        }

        store.dispatch(GlobalAction.UpdateUserWalletsListManager(manager))
    }
}
