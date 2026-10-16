package com.tangem.tap

import android.app.Application
import android.os.Build
import android.os.StrictMode
import android.os.StrictMode.ThreadPolicy
import android.os.StrictMode.VmPolicy
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import coil.ImageLoader
import coil.ImageLoaderFactory
import com.tangem.blockchain.common.ExceptionHandler
import com.tangem.core.abtests.manager.ABTestsManager
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.analytics.models.event.TechAnalyticsEvent
import com.tangem.core.ui.components.background.northernlights.NorthernLightsCrashGuard
import com.tangem.core.ui.components.background.northernlights.NorthernLightsCrashGuardPreferences
import com.tangem.core.configtoggle.blockchain.ExcludedBlockchainsManager
import com.tangem.core.configtoggle.feature.FeatureTogglesManager
import com.tangem.core.remote.config.managers.ApiConfigsManager
import com.tangem.domain.apptheme.GetAppThemeModeUseCase
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.wallets.repository.WalletsRepository
import com.tangem.lib.auth.AuthFeatureToggles
import com.tangem.lib.auth.devicekey.DeviceKeyManager
import com.tangem.lib.auth.session.DeviceRegistrar
import com.tangem.tap.common.analytics.handlers.BlockchainExceptionHandler
import com.tangem.tap.common.analytics.handlers.opentelemetry.OpenTelemetryMetricsHolder
import com.tangem.tap.common.images.createCoilImageLoader
import com.tangem.tap.common.log.TangemLoggingInitializer
import com.tangem.tap.domain.walletregistration.WalletRegistrationLauncher
import com.tangem.utils.coroutines.runSuspendCatching
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

    private val featureTogglesManager: FeatureTogglesManager
        get() = entryPoint.getFeatureTogglesManager()

    private val excludedBlockchainsManager: ExcludedBlockchainsManager
        get() = entryPoint.getExcludedBlockchainsManager()

    val getAppThemeModeUseCase: GetAppThemeModeUseCase
        get() = entryPoint.getGetAppThemeModeUseCase()

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

    private val deviceKeyManager: DeviceKeyManager
        get() = entryPoint.getDeviceKeyManager()

    private val deviceRegistrar: DeviceRegistrar
        get() = entryPoint.getDeviceRegistrar()

    private val authFeatureToggles: AuthFeatureToggles
        get() = entryPoint.getAuthFeatureToggles()

    private val walletRegistrationLauncher: WalletRegistrationLauncher
        get() = entryPoint.getWalletRegistrationLauncher()

    private val userWalletsListRepository: UserWalletsListRepository
        get() = entryPoint.getUserWalletsListRepository()

    private val openTelemetryMetricsHolder: OpenTelemetryMetricsHolder
        get() = entryPoint.getOpenTelemetryMetricsHolder()

    private val analyticsEventHandler: AnalyticsEventHandler
        get() = entryPoint.getAnalyticsEventHandler()

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
        NorthernLightsCrashGuard.init(
            store = NorthernLightsCrashGuardPreferences(context = this),
            appVersion = BuildConfig.VERSION_CODE,
        )
        registerActivityLifecycleCallbacks(foregroundActivityObserver.callbacks)
    }

    fun init() {
        if (authFeatureToggles.isBackendAuthenticationEnabled) {
            appScope.launch {
                // Order matters: registration reads the device public key, so it must wait for
                // generation to complete. Running them concurrently on first launch would race —
                // register() would see `DeviceKeyUnavailable` and defer to the next app launch.
                deviceKeyManager.generateIfMissing()
                deviceRegistrar.register()
                    .onLeft { error -> TangemLogger.w("Device registration deferred: $error") }
                    .onRight {
                        // Safety net: register any MOBILE wallets that missed it (created before the
                        // toggle was on, or after a transient failure). Requires the DPoP session from
                        // device registration, so it runs only once that succeeded. Best-effort —
                        // loading the wallet list (userWalletsSync) can throw, which must not crash
                        // startup, so the whole retry is guarded.
                        runSuspendCatching {
                            walletRegistrationLauncher.retryMobileRegistrations(
                                userWalletsListRepository.userWalletsSync(),
                            )
                        }.onFailure { error -> TangemLogger.e("Mobile wallet registration retry failed", error) }
                    }
            }
        }
        walletsRepository = entryPoint.getWalletsRepository()

        apiConfigsManager.initialize()

        TangemLogger.i("APP STARTED")
        if (BuildConfig.TESTER_MENU_ENABLED) {
            TangemLogger.i(featureTogglesManager.toString())
            TangemLogger.i(excludedBlockchainsManager.toString())
        }

        initAnalyticsHandlers()

        reportShaderCrashIfDetected()

        initOpenTelemetry()

        abTestsManager.init()

        appScope.launch {
            launch(Dispatchers.IO) {
                loadNativeLibraries()
            }
        }

        ExceptionHandler.append(blockchainExceptionHandler)

        tangemLoggingInitializer.initSdkLogging(this)

        wcInitializeUseCase()
    }

    override fun newImageLoader(): ImageLoader = createCoilImageLoader(context = this)

    private fun loadNativeLibraries() {
        System.loadLibrary("TrustWalletCore")
    }

    private fun reportShaderCrashIfDetected() {
        if (!NorthernLightsCrashGuard.hasDetectedCrash) return

        analyticsEventHandler.send(
            TechAnalyticsEvent.ShaderCrashDetected(
                model = Build.MODEL,
                manufacturer = Build.MANUFACTURER,
                hardware = Build.HARDWARE,
                androidVersion = Build.VERSION.SDK_INT.toString(),
                crashCount = NorthernLightsCrashGuard.crashCount.toString(),
            ),
        )
    }

    // SDK clients are created together with their handlers, so the set is resolved eagerly at startup
    private fun initAnalyticsHandlers() {
        entryPoint.getAnalyticsHandlers()
    }

    private fun initOpenTelemetry() {
        openTelemetryMetricsHolder.initialize()
    }
}