package com.tangem.tap

import android.app.Application
import android.os.StrictMode
import android.os.StrictMode.ThreadPolicy
import android.os.StrictMode.VmPolicy
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import coil.ImageLoader
import coil.ImageLoaderFactory
import com.tangem.blockchain.common.ExceptionHandler
import com.tangem.core.abtests.manager.ABTestsManager
import com.tangem.core.analytics.Analytics
import com.tangem.core.analytics.filter.AppsFlyerEventFilter
import com.tangem.core.analytics.filter.OneTimeEventFilter
import com.tangem.core.configtoggle.blockchain.ExcludedBlockchainsManager
import com.tangem.core.configtoggle.feature.FeatureTogglesManager
import com.tangem.datasource.api.common.MoshiConverter
import com.tangem.datasource.api.common.config.managers.ApiConfigsManager
import com.tangem.datasource.local.config.environment.EnvironmentConfig
import com.tangem.domain.apptheme.GetAppThemeModeUseCase
import com.tangem.domain.common.LogConfig
import com.tangem.domain.wallets.repository.WalletsRepository
import com.tangem.tap.common.analytics.AnalyticsFactory
import com.tangem.tap.common.analytics.api.AnalyticsHandlerBuilder
import com.tangem.tap.common.analytics.handlers.BlockchainExceptionHandler
import com.tangem.tap.common.analytics.handlers.amplitude.AmplitudeAnalyticsHandler
import com.tangem.tap.common.analytics.handlers.appsflyer.AppsFlyerAnalyticsHandler
import com.tangem.tap.common.analytics.handlers.appsflyer.AppsFlyerClient
import com.tangem.tap.common.analytics.handlers.customerio.CustomerIoAnalyticsHandler
import com.tangem.tap.common.analytics.handlers.firebase.FirebaseAnalyticsHandler
import com.tangem.tap.common.images.createCoilImageLoader
import com.tangem.tap.common.log.TangemLoggingInitializer
import com.tangem.tap.init.WireMockOverride
import com.tangem.utils.logging.TangemLogger
import com.tangem.wallet.BuildConfig
import dagger.hilt.EntryPoints
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

lateinit var walletsRepository: WalletsRepository

val foregroundActivityObserver = ForegroundActivityObserver

open class TangemApplication : Application(), ImageLoaderFactory, Configuration.Provider {

    // region DI
    private val entryPoint: ApplicationEntryPoint
        get() = EntryPoints.get(this, ApplicationEntryPoint::class.java)

    private val environmentConfig: EnvironmentConfig
        get() = entryPoint.getEnvironmentConfig()

    private val featureTogglesManager: FeatureTogglesManager
        get() = entryPoint.getFeatureTogglesManager()

    private val excludedBlockchainsManager: ExcludedBlockchainsManager
        get() = entryPoint.getExcludedBlockchainsManager()

    val getAppThemeModeUseCase: GetAppThemeModeUseCase
        get() = entryPoint.getGetAppThemeModeUseCase()

    private val oneTimeEventFilter: OneTimeEventFilter
        get() = entryPoint.getOneTimeEventFilter()

    private val tangemLoggingInitializer: TangemLoggingInitializer
        get() = entryPoint.getTangemLoggingInitializer()

    private val blockchainExceptionHandler: BlockchainExceptionHandler
        get() = entryPoint.getBlockchainExceptionHandler()

    private val workerFactory: HiltWorkerFactory
        get() = entryPoint.getWorkerFactory()

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    private val apiConfigsManager: ApiConfigsManager
        get() = entryPoint.getApiConfigsManager()

    private val wcInitializeUseCase
        get() = entryPoint.getWcInitializeUseCase()

    private val abTestsManager: ABTestsManager
        get() = entryPoint.getABTestsManager()

    private val appsFlyerClientFactory: AppsFlyerClient.Factory
        get() = entryPoint.getAppsFlyerClientFactory()

    private val sendTransactionSignerInfoInterceptor
        get() = entryPoint.getSendTransactionSignerInfoInterceptor()

    // endregion

    private val appScope = MainScope()

    override fun onCreate() {
        enableStrictModeInDebug()
        preInit()
        super.onCreate()
        init()
    }

    private fun enableStrictModeInDebug() {
        if (BuildConfig.DEBUG) {
            StrictMode.setThreadPolicy(
                ThreadPolicy.Builder()
                    .detectDiskReads()
                    .detectDiskWrites()
                    .detectAll()
                    .penaltyLog()
                    .build(),
            )
            StrictMode.setVmPolicy(
                VmPolicy.Builder()
                    .detectLeakedSqlLiteObjects()
                    .detectLeakedClosableObjects()
                    .penaltyLog()
                    .build(),
            )
        }
    }

    /**
     * Initialize components that need to be initialized before [super.onCreate] is called
     */
    fun preInit() {
        tangemLoggingInitializer.initAppLogging()
        // Mocked buildType only: redirect wiremock.tests-d.com to local WireMock
        // for non-instrumentation launches (Maestro). No-op in every other buildType.
        WireMockOverride.apply()
        registerActivityLifecycleCallbacks(foregroundActivityObserver.callbacks)
    }

    fun init() {
        walletsRepository = entryPoint.getWalletsRepository()

        apiConfigsManager.initialize()

        TangemLogger.i("APP STARTED")
        if (BuildConfig.TESTER_MENU_ENABLED) {
            TangemLogger.i(featureTogglesManager.toString())
            TangemLogger.i(excludedBlockchainsManager.toString())
        }

        initAnalytics(application = this, environmentConfig = environmentConfig)

        abTestsManager.init()

        appScope.launch {
            launch(Dispatchers.IO) {
                loadNativeLibraries()
            }
        }

        ExceptionHandler.append(blockchainExceptionHandler)

        tangemLoggingInitializer.initSdkLogging(this)

        wcInitializeUseCase.init(
            projectId = environmentConfig.walletConnectProjectId,
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

    private fun initAnalytics(application: Application, environmentConfig: EnvironmentConfig) {
        val factory = AnalyticsFactory()
        factory.addHandlerBuilder(AmplitudeAnalyticsHandler.Builder())
        factory.addHandlerBuilder(FirebaseAnalyticsHandler.Builder())
        factory.addHandlerBuilder(AppsFlyerAnalyticsHandler.Builder(appsFlyerClientFactory))

        factory.addHandlerBuilder(CustomerIoAnalyticsHandler.Builder())

        factory.addFilter(oneTimeEventFilter)
        factory.addFilter(AppsFlyerEventFilter())

        val buildData = AnalyticsHandlerBuilder.Data(
            application = application,
            config = environmentConfig,
            isDebug = BuildConfig.DEBUG,
            logConfig = LogConfig.analyticsHandlers,
            jsonConverter = MoshiConverter.sdkMoshiConverter,
        )

        Analytics.addParamsInterceptor(interceptor = sendTransactionSignerInfoInterceptor)

        factory.build(Analytics, buildData)
    }
}