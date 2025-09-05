package com.tangem.tap

import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.WindowManager
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.net.toUri
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import arrow.core.getOrElse
import com.tangem.common.routing.AppRoute
import com.tangem.common.routing.deeplink.DeeplinkConst.WEBLINK_KEY
import com.tangem.common.routing.deeplink.PayloadToDeeplinkConverter
import com.tangem.common.routing.entity.SerializableIntent
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.di.RootAppComponentContext
import com.tangem.core.navigation.url.UrlOpener
import com.tangem.data.balancehiding.DefaultDeviceFlipDetector
import com.tangem.data.card.sdk.CardSdkOwner
import com.tangem.domain.apptheme.model.AppThemeMode
import com.tangem.domain.card.ScanCardUseCase
import com.tangem.domain.card.repository.CardRepository
import com.tangem.domain.card.repository.CardSdkConfigRepository
import com.tangem.domain.core.wallets.UserWalletsListRepository
import com.tangem.domain.models.wallet.isLocked
import com.tangem.domain.settings.SetGooglePayAvailabilityUseCase
import com.tangem.domain.settings.SetGoogleServicesAvailabilityUseCase
import com.tangem.domain.settings.ShouldInitiallyAskPermissionUseCase
import com.tangem.domain.settings.repositories.SettingsRepository
import com.tangem.domain.staking.SendUnsubmittedHashesUseCase
import com.tangem.domain.tokens.GetPolkadotCheckHasImmortalUseCase
import com.tangem.domain.tokens.GetPolkadotCheckHasResetUseCase
import com.tangem.domain.wallets.legacy.UserWalletsListManager
import com.tangem.feature.wallet.presentation.wallet.analytics.WalletScreenAnalyticsEvent
import com.tangem.features.hotwallet.HotWalletFeatureToggles
import com.tangem.features.tangempay.TangemPayFeatureToggles
import com.tangem.features.tester.api.TesterMenuLauncher
import com.tangem.google.GoogleServicesHelper
import com.tangem.operations.backup.BackupService
import com.tangem.sdk.api.BackupServiceHolder
import com.tangem.sdk.api.TangemSdkManager
import com.tangem.tap.common.ActivityResultCallbackHolder
import com.tangem.tap.common.DialogManager
import com.tangem.tap.common.OnActivityResultCallback
import com.tangem.tap.common.apptheme.MutableAppThemeModeHolder
import com.tangem.tap.common.extensions.dispatchNavigationAction
import com.tangem.tap.features.intentHandler.IntentProcessor
import com.tangem.tap.features.intentHandler.handlers.BackgroundScanIntentHandler
import com.tangem.tap.features.intentHandler.handlers.OnPushClickedIntentHandler
import com.tangem.tap.features.main.MainViewModel
import com.tangem.tap.proxy.redux.DaggerGraphAction
import com.tangem.tap.routing.component.RoutingComponent
import com.tangem.tap.routing.configurator.AppRouterConfig
import com.tangem.tap.routing.utils.DeepLinkFactory
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.coroutines.FeatureCoroutineExceptionHandler
import com.tangem.utils.extensions.uriValidate
import com.tangem.wallet.BuildConfig
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import timber.log.Timber
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext
import kotlin.time.Duration.Companion.seconds

lateinit var tangemSdkManager: TangemSdkManager
lateinit var backupService: BackupService
internal var lockUserWalletsTimer: LockUserWalletsTimer? = null
    private set

private val coroutineContext: CoroutineContext
    get() = SupervisorJob() + Dispatchers.IO + FeatureCoroutineExceptionHandler.create("scope")
val scope = CoroutineScope(coroutineContext)

private val mainCoroutineContext: CoroutineContext
    get() = SupervisorJob() + Dispatchers.Main + FeatureCoroutineExceptionHandler.create("mainScope")
val mainScope = CoroutineScope(mainCoroutineContext)

@Suppress("LargeClass")
@AndroidEntryPoint
class MainActivity : AppCompatActivity(), ActivityResultCallbackHolder {

    /** Router for opening tester menu */
    @Inject
    lateinit var cardSdkOwner: CardSdkOwner

    @Inject
    lateinit var cardSdkConfigRepository: CardSdkConfigRepository

    @Inject
    lateinit var injectedTangemSdkManager: TangemSdkManager

    @Inject
    lateinit var scanCardUseCase: ScanCardUseCase

    @Inject
    lateinit var settingsRepository: SettingsRepository

    @Inject
    lateinit var sendUnsubmittedHashesUseCase: SendUnsubmittedHashesUseCase

    @Inject
    lateinit var getPolkadotCheckHasResetUseCase: GetPolkadotCheckHasResetUseCase

    @Inject
    lateinit var getPolkadotCheckHasImmortalUseCase: GetPolkadotCheckHasImmortalUseCase

    @Inject
    lateinit var analyticsEventsHandler: AnalyticsEventHandler

    @Inject
    lateinit var userWalletsListManager: UserWalletsListManager

    @Inject
    @RootAppComponentContext
    internal lateinit var rootComponentContext: AppComponentContext

    @Inject
    internal lateinit var appRouterConfig: AppRouterConfig

    @Inject
    internal lateinit var routingComponentFactory: RoutingComponent.Factory

    @Inject
    lateinit var cardRepository: CardRepository

    @Inject
    lateinit var shouldInitiallyAskPermissionUseCase: ShouldInitiallyAskPermissionUseCase

    @Inject
    lateinit var backupServiceHolder: BackupServiceHolder

    @Inject
    lateinit var setGoogleServicesAvailabilityUseCase: SetGoogleServicesAvailabilityUseCase

    @Inject
    lateinit var setGooglePayAvailabilityUseCase: SetGooglePayAvailabilityUseCase

    @Inject
    lateinit var dispatchers: CoroutineDispatcherProvider

    @Inject
    internal lateinit var defaultDeviceFlipDetector: DefaultDeviceFlipDetector

    @Inject
    internal lateinit var deeplinkFactory: DeepLinkFactory

    @Inject
    internal lateinit var urlOpener: UrlOpener

    @Inject
    internal lateinit var testerMenuLauncher: TesterMenuLauncher

    @Inject
    internal lateinit var intentProcessor: IntentProcessor

    @Inject
    internal lateinit var onPushClickedIntentHandler: OnPushClickedIntentHandler

    @Inject
    internal lateinit var backgroundScanIntentHandler: BackgroundScanIntentHandler

    @Inject
    internal lateinit var userWalletsListRepository: UserWalletsListRepository

    @Inject
    internal lateinit var hotWalletFeatureToggles: HotWalletFeatureToggles

    @Inject
    internal lateinit var tangemPayFeatureToggles: TangemPayFeatureToggles

    private val viewModel: MainViewModel by viewModels()

    private lateinit var appThemeModeFlow: SharedFlow<AppThemeMode>

    private val dialogManager = DialogManager()

    private val onActivityResultCallbacks = mutableListOf<OnActivityResultCallback>()

    override fun onCreate(savedInstanceState: Bundle?) {
        // We need to call it before onCreate to prevent unnecessary activity recreation
        installAppTheme()

        val splashScreen = installSplashScreen()

        enableEdgeToEdge(
            navigationBarStyle = SystemBarStyle.auto(
                Color.Transparent.toArgb(),
                Color.Transparent.toArgb(),
            ),
        )

        super.onCreate(savedInstanceState)

        // We have to allow adjust_resize (can only be specified in the manifest) for Android <=10,
        // in order for imePadding to work correctly
        // for Android 11+ we set SOFT_INPUT_ADJUST_NOTHING to prevent resizing the layout
        // so that we don't have any distortions in the layout when displaying the keyboard
        // https://issuetracker.google.com/issues/266331465
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.Q) {
            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            window.setHideOverlayWindows(true)
        }

        splashScreen.setKeepOnScreenCondition { viewModel.isSplashScreenShown }

        installActivityDependencies()
        observeAppThemeModeUpdates()

        setRootContent()

        initContent()

        observePolkadotAccountHealthCheck()
        sendStakingUnsubmittedHashes()
        checkGoogleServicesAvailability()

        lifecycle.addObserver(WindowObscurationObserver)
        lifecycle.addObserver(defaultDeviceFlipDetector)

        if (BuildConfig.TESTER_MENU_ENABLED) {
            lifecycle.addObserver(testerMenuLauncher.launchOnKeyEventObserver)
        }
    }

    private fun setRootContent() {
        // for now activity is singleTop and after going to ChromeCustomTab it calls onCreate but onDestroy
        // doesn't calls. It lead to issue that decompose nav stack is not saved in bundle and to restore it
        // we try to init component with previous stack
        val routingComponent = routingComponentFactory.create(
            context = rootComponentContext,
            initialStack = appRouterConfig.stack,
        )

        setContent {
            routingComponent.Content(Modifier.fillMaxSize())
        }
    }

    private fun installActivityDependencies() {
        cardSdkOwner.register(activity = this)
        tangemSdkManager = injectedTangemSdkManager

        backupServiceHolder.createAndSetService(cardSdkConfigRepository.sdk, this)
        backupService = backupServiceHolder.backupService.get()!! // will be deleted eventually

        lockUserWalletsTimer = LockUserWalletsTimer(
            context = this,
            settingsRepository = settingsRepository,
            userWalletsListManager = userWalletsListManager,
            coroutineScope = mainScope,
            userWalletsListRepository = userWalletsListRepository,
            hotWalletFeatureToggles = hotWalletFeatureToggles,
        )

        initIntentHandlers()

        store.dispatch(
            DaggerGraphAction.SetActivityDependencies(
                scanCardUseCase = scanCardUseCase,
                cardSdkConfigRepository = cardSdkConfigRepository,
            ),
        )
    }

    private fun installAppTheme() {
        appThemeModeFlow = createAppThemeModeFlow()
        val mode = runBlocking {
            withTimeoutOrNull(APP_THEME_LOAD_TIMEOUT.seconds) {
                appThemeModeFlow.first()
            } ?: AppThemeMode.DEFAULT
        }

        updateAppTheme(mode)
    }

    private fun observeAppThemeModeUpdates() {
        appThemeModeFlow
            .flowWithLifecycle(lifecycle)
            .onEach(::updateAppTheme)
            .launchIn(lifecycleScope)
    }

    @SuppressLint("SourceLockedOrientationActivity")
    private fun initContent() {
        supportFragmentManager.registerFragmentLifecycleCallbacks(
            NavBarInsetsFragmentLifecycleCallback(),
            true,
        )

        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
    }

    private fun createAppThemeModeFlow(): SharedFlow<AppThemeMode> {
        val tangemApplication = application as TangemApplication

        return tangemApplication.getAppThemeModeUseCase()
            .filterNotNull()
            .distinctUntilChanged()
            .map { maybeMode ->
                maybeMode.getOrElse { AppThemeMode.DEFAULT }
            }
            .shareIn(
                scope = lifecycleScope,
                started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
            )
    }

    override fun onStart() {
        super.onStart()
        dialogManager.onStart(this)
    }

    override fun onResume() {
        super.onResume()
        navigateToInitialScreenIfNeeded(intent)
    }

    override fun onStop() {
        dialogManager.onStop()
        super.onStop()
    }

    override fun onDestroy() {
        intentProcessor.removeAll()
        // workaround: kill process when activity destroy to avoid state when lock() wallets
        // and navigation to unlock screen was skipped because system kills activity but not process
        if (BuildConfig.BUILD_TYPE != MOCKED_BUILD_TYPE) {
            android.os.Process.killProcess(android.os.Process.myPid())
        }
        super.onDestroy()
    }

    private fun initIntentHandlers() {
        intentProcessor.addHandler(onPushClickedIntentHandler)
    }

    private fun updateAppTheme(appThemeMode: AppThemeMode) {
        val mode = when (appThemeMode) {
            AppThemeMode.FORCE_DARK -> AppCompatDelegate.MODE_NIGHT_YES
            AppThemeMode.FORCE_LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
            AppThemeMode.FOLLOW_SYSTEM -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        }

        setDefaultNightMode(mode)

        MutableAppThemeModeHolder.value = appThemeMode
        MutableAppThemeModeHolder.isDarkThemeActive = isDarkTheme()
    }

    private fun isDarkTheme(): Boolean {
        return when (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) {
            Configuration.UI_MODE_NIGHT_YES -> true
            Configuration.UI_MODE_NIGHT_NO -> false
            Configuration.UI_MODE_NIGHT_UNDEFINED -> false
            else -> false
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)

        lifecycleScope.launch {
            intentProcessor.handleIntent(intent = intent, isFromForeground = true)
        }

        if (intent != null) {
            handleDeepLink(intent = intent, isFromOnNewIntent = true)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        onActivityResultCallbacks.forEach { it(requestCode, resultCode, data) }
    }

    override fun addOnActivityResultCallback(callback: OnActivityResultCallback) {
        onActivityResultCallbacks.remove(callback)
        onActivityResultCallbacks.add(callback)
    }

    override fun removeOnActivityResultCallback(callback: OnActivityResultCallback) {
        onActivityResultCallbacks.remove(callback)
    }

    override fun onUserInteraction() {
        super.onUserInteraction()

        lockUserWalletsTimer?.restart()
    }

    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        val result = WindowObscurationObserver.dispatchTouchEvent(event, analyticsEventsHandler)
        return if (result) super.dispatchTouchEvent(event) else false
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        return if (BuildConfig.TESTER_MENU_ENABLED) {
            testerMenuLauncher.launchOnKeyEventObserver.dispatchKeyEvent(event) || super.dispatchKeyEvent(event)
        } else {
            super.dispatchKeyEvent(event)
        }
    }

    private fun navigateToInitialScreenIfNeeded(intentWhichStartedActivity: Intent?) {
        // TODO refactor this method to return a route instead of navigating directly
        if (hotWalletFeatureToggles.isHotWalletEnabled) {
            navigateToInitialScreenIfNeededNew(intentWhichStartedActivity)
            return
        }

        val backStack = appRouterConfig.stack ?: emptyList()
        // TODO move inital navigation to navigation component ([REDACTED_JIRA])
        val isOnlyInitialRoute = backStack.all { it is AppRoute.Initial }
        val isOnInitialScreen = backStack.all { it is AppRoute.Welcome || it is AppRoute.Home }
        val isNotScannedBefore = store.state.globalState.scanResponse == null
        val isOnboardingServiceNotActive = !store.state.globalState.onboardingState.onboardingStarted

        when {
            !isOnInitialScreen && isNotScannedBefore && isOnboardingServiceNotActive -> {
                navigateToInitialScreen(intentWhichStartedActivity)
            }
            backStack.isEmpty() -> {
                navigateToInitialScreen(intentWhichStartedActivity)
            }
            isOnlyInitialRoute -> navigateToInitialScreen(intentWhichStartedActivity)
            else -> Unit
        }
    }

    @Deprecated("Refactor this method to return a route instead of navigating directly")
    private fun navigateToInitialScreenIfNeededNew(intentWhichStartedActivity: Intent?) {
        lifecycleScope.launch {
            val userWallets = userWalletsListRepository.userWalletsSync()
            val launchMode = backgroundScanIntentHandler.getInitScreenLaunchMode(intentWhichStartedActivity)
            if (userWallets.isEmpty()) {
                val shouldShowTos = !cardRepository.isTangemTOSAccepted()

                val route = if (shouldShowTos) {
                    AppRoute.Disclaimer(isTosAccepted = false)
                } else {
                    AppRoute.Home(launchMode = launchMode)
                }

                store.dispatchNavigationAction { replaceAll(route) }
                intentProcessor.handleIntent(
                    intent = intentWhichStartedActivity,
                    isFromForeground = false,
                    skipNavigationHandlers = false,
                )
            } else {
                if (userWallets.any { it.isLocked }) {
                    store.dispatchNavigationAction {
                        replaceAll(
                            AppRoute.Welcome(
                                launchMode = launchMode,
                                intent = intentWhichStartedActivity?.let(::SerializableIntent),
                            ),
                        )
                    }
                } else {
                    store.dispatchNavigationAction {
                        replaceAll(AppRoute.Wallet)
                    }
                }

                intentProcessor.handleIntent(
                    intent = intentWhichStartedActivity,
                    isFromForeground = false,
                    skipNavigationHandlers = true,
                )
            }

            if (intent != null) {
                handleDeepLink(intent = intent, isFromOnNewIntent = false)
            }

            viewModel.checkForUnfinishedBackup()
        }
    }

    private fun navigateToInitialScreen(intentWhichStartedActivity: Intent?) {
        val launchMode = backgroundScanIntentHandler.getInitScreenLaunchMode(intentWhichStartedActivity)

        // Workaround to navigate to TangemPayDetails screen. Will be deleted in next PRs
        if (tangemPayFeatureToggles.isTangemPayEnabled) {
            lifecycleScope.launch {
                val selectedUserWalledId = userWalletsListRepository.selectedUserWalletSync()?.walletId
                store.dispatchNavigationAction {
                    replaceAll(AppRoute.TangemPayDetails(requireNotNull(selectedUserWalledId)))
                }
            }
        } else if (userWalletsListManager.isLockable && userWalletsListManager.hasUserWallets) {
            store.dispatchNavigationAction {
                replaceAll(
                    AppRoute.Welcome(
                        launchMode = launchMode,
                        intent = intentWhichStartedActivity?.let(::SerializableIntent),
                    ),
                )
            }
            intentProcessor.handleIntent(
                intent = intentWhichStartedActivity,
                isFromForeground = false,
                skipNavigationHandlers = true,
            )
        } else {
            lifecycleScope.launch {
                val shouldShowTos = !cardRepository.isTangemTOSAccepted()

                val route = if (shouldShowTos) {
                    AppRoute.Disclaimer(isTosAccepted = false)
                } else {
                    AppRoute.Home(launchMode = launchMode)
                }

                store.dispatchNavigationAction { replaceAll(route) }
                intentProcessor.handleIntent(
                    intent = intentWhichStartedActivity,
                    isFromForeground = false,
                    skipNavigationHandlers = false,
                )
            }
        }

        if (intent != null) {
            handleDeepLink(intent = intent, isFromOnNewIntent = false)
        }

        viewModel.checkForUnfinishedBackup()
    }

    private fun handleDeepLink(intent: Intent, isFromOnNewIntent: Boolean) {
        val deepLinkExtras = PayloadToDeeplinkConverter.convertBundle(intent.extras)?.toUri()
        val webLink = intent.getStringExtra(WEBLINK_KEY)

        val receivedDeepLink = intent.data ?: deepLinkExtras

        when {
            receivedDeepLink != null -> {
                deeplinkFactory.handleDeeplink(
                    deeplinkUri = receivedDeepLink,
                    coroutineScope = lifecycleScope,
                    isFromOnNewIntent = isFromOnNewIntent,
                )
            }
            webLink?.uriValidate() == true -> {
                urlOpener.openUrl(webLink)
            }
        }
    }

    private fun observePolkadotAccountHealthCheck() {
        lifecycleScope.launch {
            getPolkadotCheckHasResetUseCase()
                .flowWithLifecycle(lifecycle, minActiveState = Lifecycle.State.CREATED)
                .distinctUntilChanged()
                .collect {
                    analyticsEventsHandler.send(WalletScreenAnalyticsEvent.Token.PolkadotAccountReset(it.second))
                }
        }
        lifecycleScope.launch {
            getPolkadotCheckHasImmortalUseCase()
                .flowWithLifecycle(lifecycle, minActiveState = Lifecycle.State.CREATED)
                .distinctUntilChanged()
                .collect {
                    analyticsEventsHandler.send(
                        WalletScreenAnalyticsEvent.Token.PolkadotImmortalTransactions(it.second),
                    )
                }
        }
    }

    private fun sendStakingUnsubmittedHashes() {
        lifecycleScope.launch {
            sendUnsubmittedHashesUseCase.invoke()
                .onLeft { Timber.e(it.toString()) }
                .onRight { Timber.d("Submitting hashes succeeded") }
        }
    }

    private fun checkGoogleServicesAvailability() {
        val isGoogleServicesAvailable = GoogleServicesHelper.checkGoogleServicesAvailability(this)

        lifecycleScope.launch {
            setGoogleServicesAvailabilityUseCase(isGoogleServicesAvailable)

            if (isGoogleServicesAvailable) {
                val paymentsClient = GoogleServicesHelper.createPaymentsClient(this@MainActivity)
                val isGooglePayAvailable = GoogleServicesHelper.checkGooglePayAvailability(paymentsClient)
                setGooglePayAvailabilityUseCase(isGooglePayAvailable)
            } else {
                setGooglePayAvailabilityUseCase(false)
            }
        }
    }

    companion object {
        private const val APP_THEME_LOAD_TIMEOUT = 2
        private const val MOCKED_BUILD_TYPE = "mocked"
    }
}