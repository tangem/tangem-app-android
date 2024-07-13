package com.tangem.tap

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode
import androidx.compose.ui.graphics.toArgb
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import arrow.core.getOrElse
import by.kirich1409.viewbindingdelegate.viewBinding
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.snackbar.Snackbar
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.deeplink.DeepLinksRegistry
import com.tangem.core.navigation.AppScreen
import com.tangem.core.navigation.NavigationAction
import com.tangem.core.navigation.email.EmailSender
import com.tangem.core.ui.event.StateEvent
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.res.TangemColorPalette
import com.tangem.data.card.sdk.CardSdkOwner
import com.tangem.domain.apptheme.model.AppThemeMode
import com.tangem.domain.card.ScanCardUseCase
import com.tangem.domain.card.repository.CardSdkConfigRepository
import com.tangem.domain.settings.repositories.SettingsRepository
import com.tangem.domain.tokens.GetPolkadotCheckHasImmortalUseCase
import com.tangem.domain.tokens.GetPolkadotCheckHasResetUseCase
import com.tangem.domain.wallets.legacy.UserWalletsListManager
import com.tangem.feature.qrscanning.QrScanningRouter
import com.tangem.feature.wallet.presentation.wallet.analytics.WalletScreenAnalyticsEvent
import com.tangem.features.managetokens.navigation.ManageTokensUi
import com.tangem.features.send.api.navigation.SendRouter
import com.tangem.features.tester.api.TesterRouter
import com.tangem.features.tokendetails.navigation.TokenDetailsRouter
import com.tangem.features.wallet.navigation.WalletRouter
import com.tangem.operations.backup.BackupService
import com.tangem.sdk.extensions.init
import com.tangem.tap.common.ActivityResultCallbackHolder
import com.tangem.tap.common.DialogManager
import com.tangem.tap.common.OnActivityResultCallback
import com.tangem.tap.common.SnackbarHandler
import com.tangem.tap.common.apptheme.MutableAppThemeModeHolder
import com.tangem.tap.common.redux.NotificationsHandler
import com.tangem.tap.domain.sdk.TangemSdkManager
import com.tangem.tap.domain.walletconnect2.domain.WalletConnectInteractor
import com.tangem.tap.features.intentHandler.IntentProcessor
import com.tangem.tap.features.intentHandler.handlers.BackgroundScanIntentHandler
import com.tangem.tap.features.intentHandler.handlers.WalletConnectLinkIntentHandler
import com.tangem.tap.features.main.MainViewModel
import com.tangem.tap.features.main.model.Toast
import com.tangem.tap.features.onboarding.products.wallet.redux.BackupAction
import com.tangem.tap.features.welcome.ui.WelcomeFragment
import com.tangem.tap.proxy.AppStateHolder
import com.tangem.tap.proxy.redux.DaggerGraphAction
import com.tangem.utils.coroutines.FeatureCoroutineExceptionHandler
import com.tangem.wallet.BuildConfig
import com.tangem.wallet.R
import com.tangem.wallet.databinding.ActivityMainBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.lang.ref.WeakReference
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

lateinit var tangemSdkManager: TangemSdkManager
lateinit var backupService: BackupService
internal var lockUserWalletsTimer: LockUserWalletsTimer? = null
    private set
var notificationsHandler: NotificationsHandler? = null

private val coroutineContext: CoroutineContext
    get() = Job() + Dispatchers.IO + FeatureCoroutineExceptionHandler.create("scope")
val scope = CoroutineScope(coroutineContext)

private val mainCoroutineContext: CoroutineContext
    get() = Job() + Dispatchers.Main + FeatureCoroutineExceptionHandler.create("mainScope")
val mainScope = CoroutineScope(mainCoroutineContext)

@Suppress("LargeClass")
@AndroidEntryPoint
class MainActivity : AppCompatActivity(), SnackbarHandler, ActivityResultCallbackHolder {

    @Inject
    lateinit var appStateHolder: AppStateHolder

    /** Router for opening tester menu */
    @Inject
    lateinit var testerRouter: TesterRouter

    @Inject
    lateinit var cardSdkOwner: CardSdkOwner

    @Inject
    lateinit var cardSdkConfigRepository: CardSdkConfigRepository

    @Inject
    lateinit var injectedTangemSdkManager: TangemSdkManager

    @Inject
    lateinit var scanCardUseCase: ScanCardUseCase

    @Inject
    lateinit var walletRouter: WalletRouter

    @Inject
    lateinit var tokenDetailsRouter: TokenDetailsRouter

    @Inject
    lateinit var manageTokensUi: ManageTokensUi

    @Inject
    lateinit var walletConnectInteractor: WalletConnectInteractor

    @Inject
    lateinit var sendRouter: SendRouter

    @Inject
    lateinit var qrScanningRouter: QrScanningRouter

    @Inject
    lateinit var deepLinksRegistry: DeepLinksRegistry

    @Inject
    lateinit var settingsRepository: SettingsRepository

    @Inject
    lateinit var getPolkadotCheckHasResetUseCase: GetPolkadotCheckHasResetUseCase

    @Inject
    lateinit var getPolkadotCheckHasImmortalUseCase: GetPolkadotCheckHasImmortalUseCase

    @Inject
    lateinit var analyticsEventsHandler: AnalyticsEventHandler

    @Inject
    lateinit var userWalletsListManager: UserWalletsListManager

    @Inject
    lateinit var emailSender: EmailSender

    internal val viewModel: MainViewModel by viewModels()

    private lateinit var appThemeModeFlow: SharedFlow<AppThemeMode>

    // TODO: fixme: inject through DI
    private val intentProcessor: IntentProcessor = IntentProcessor()

    private var snackbar: Snackbar? = null
    private val dialogManager = DialogManager()
    private val binding: ActivityMainBinding by viewBinding(ActivityMainBinding::bind)

    private val onActivityResultCallbacks = mutableListOf<OnActivityResultCallback>()

    override fun onCreate(savedInstanceState: Bundle?) {
        // We need to call it before onCreate to prevent unnecessary activity recreation
        installAppTheme()

        val splashScreen = installSplashScreen()

        super.onCreate(savedInstanceState)

        splashScreen.setKeepOnScreenCondition { viewModel.isSplashScreenShown }

        installActivityDependencies()
        observeAppThemeModeUpdates()

        setContentView(R.layout.activity_main)
        initContent()

        checkForNotificationPermission()
        observeStateUpdates()
        observePolkadotAccountHealthCheck()

        if (intent != null) {
            deepLinksRegistry.launch(intent)
        }
    }

    private fun observeStateUpdates() {
        viewModel.state
            .flowWithLifecycle(lifecycle)
            .onEach { state ->
                if (state.toast is StateEvent.Triggered) {
                    showToast(state.toast.data)
                    state.toast.onConsume()
                }
            }
            .launchIn(lifecycleScope)
    }

    private fun showToast(toast: Toast) {
        dismissSnackbar()
        showSnackbar(toast.message, Snackbar.LENGTH_LONG, toast.action.text) {
            toast.action.onClick()
            dismissSnackbar()
        }
    }

    private fun installActivityDependencies() {
        store.dispatch(NavigationAction.ActivityCreated(WeakReference(this)))

        cardSdkOwner.register(activity = this)
        tangemSdkManager = injectedTangemSdkManager
        appStateHolder.tangemSdkManager = tangemSdkManager
        backupService = BackupService.init(cardSdkConfigRepository.sdk, this)
        lockUserWalletsTimer = LockUserWalletsTimer(
            owner = this,
            settingsRepository = settingsRepository,
            userWalletsListManager = userWalletsListManager,
        )

        initIntentHandlers()

        store.dispatch(
            DaggerGraphAction.SetActivityDependencies(
                testerRouter = testerRouter,
                scanCardUseCase = scanCardUseCase,
                walletRouter = walletRouter,
                walletConnectInteractor = walletConnectInteractor,
                tokenDetailsRouter = tokenDetailsRouter,
                manageTokensUi = manageTokensUi,
                cardSdkConfigRepository = cardSdkConfigRepository,
                sendRouter = sendRouter,
                qrScanningRouter = qrScanningRouter,
                emailSender = emailSender,
            ),
        )
    }

    private fun installAppTheme() {
        appThemeModeFlow = createAppThemeModeFlow()
        val mode = runBlocking { appThemeModeFlow.first() }

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
        WindowCompat.setDecorFitsSystemWindows(window, false)

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
        // TODO: RESEARCH! NotificationsHandler is created in onResume and destroyed in onStop
        notificationsHandler = NotificationsHandler(binding.fragmentContainer)

        navigateToInitialScreenIfNeeded(intent)
    }

    override fun onStop() {
        notificationsHandler = null
        dialogManager.onStop()
        super.onStop()
    }

    override fun onDestroy() {
        store.dispatch(NavigationAction.ActivityDestroyed(WeakReference(this)))
        intentProcessor.removeAll()
        super.onDestroy()
    }

    private fun initIntentHandlers() {
        val hasSavedWalletsProvider = { userWalletsListManager.hasUserWallets }
        intentProcessor.addHandler(BackgroundScanIntentHandler(hasSavedWalletsProvider, lifecycleScope))
        intentProcessor.addHandler(WalletConnectLinkIntentHandler())
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

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)

        /*
         * We need to manually change the background color of the activity when the UI mode changes to prevent
         * flickering when navigating between fragments.
         *

         * `android:configChanges="uiMode"` is set in the manifest.
         * */
        updateAppBackground()
    }

    private fun updateAppBackground() {
        val backgroundColor = if (isDarkTheme()) {
            TangemColorPalette.Dark6
        } else {
            TangemColorPalette.White
        }

        findViewById<CoordinatorLayout>(R.id.fragment_container).setBackgroundColor(backgroundColor.toArgb())
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
            intentProcessor.handleIntent(intent, true)
        }

        if (intent != null) {
            deepLinksRegistry.launch(intent)
        }
    }

    override fun showSnackbar(
        @StringRes text: Int,
        length: Int,
        @StringRes buttonTitle: Int?,
        action: View.OnClickListener?,
    ) {
        showSnackbar(getString(text), length, buttonTitle?.let(::getString), action)
    }

    override fun showSnackbar(
        text: TextReference,
        length: Int,
        buttonTitle: TextReference?,
        action: View.OnClickListener?,
    ) {
        showSnackbar(text.resolveReference(resources), length, buttonTitle?.resolveReference(resources), action)
    }

    override fun dismissSnackbar() {
        snackbar?.dismiss()
        snackbar = null
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

    private fun showSnackbar(text: String, length: Int, buttonTitle: String?, action: View.OnClickListener?) {
        if (snackbar != null) return

        snackbar = Snackbar.make(binding.fragmentContainer, text, length).apply {
            val textColor = getColor(R.color.text_primary_2)

            setBackgroundTint(getColor(R.color.button_primary))
            setActionTextColor(textColor)
            setTextColor(textColor)

            if (buttonTitle != null && action != null) {
                setAction(buttonTitle, action)
            }

            addCallback(
                object : BaseTransientBottomBar.BaseCallback<Snackbar>() {
                    override fun onDismissed(transientBottomBar: Snackbar?, event: Int) {
                        snackbar = null
                        removeCallback(this)
                    }
                },
            )
        }

        snackbar?.show()
    }

    private fun navigateToInitialScreenIfNeeded(intentWhichStartedActivity: Intent?) {
        val backStack = store.state.navigationState.backStack
        val isOnInitialScreen = backStack.all { it == AppScreen.Welcome || it == AppScreen.Home }
        val isNotScannedBefore = store.state.globalState.scanResponse == null
        val isOnboardingServiceNotActive = !store.state.globalState.onboardingState.onboardingStarted

        when {
            !isOnInitialScreen && isNotScannedBefore && isOnboardingServiceNotActive -> {
                navigateToInitialScreen(intentWhichStartedActivity)
            }
            backStack.isEmpty() -> {
                navigateToInitialScreen(intentWhichStartedActivity)
            }
            else -> Unit
        }
    }

    private fun navigateToInitialScreen(intentWhichStartedActivity: Intent?) {
        if (userWalletsListManager.isLockable && userWalletsListManager.hasUserWallets) {
            store.dispatch(
                NavigationAction.NavigateTo(
                    screen = AppScreen.Welcome,
                    bundle = intentWhichStartedActivity?.let {
                        bundleOf(WelcomeFragment.INITIAL_INTENT_KEY to it)
                    },
                ),
            )
        } else {
            store.dispatch(NavigationAction.NavigateTo(AppScreen.Home))
            lifecycleScope.launch {
                intentProcessor.handleIntent(intentWhichStartedActivity, false)
            }
        }

        store.dispatch(BackupAction.CheckForUnfinishedBackup)
    }

    private fun checkForNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            BuildConfig.LOG_ENABLED &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 0)
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
}