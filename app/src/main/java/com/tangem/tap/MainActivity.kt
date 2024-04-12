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
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import arrow.core.getOrElse
import by.kirich1409.viewbindingdelegate.viewBinding
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.snackbar.Snackbar
import com.tangem.core.deeplink.DeepLinksRegistry
import com.tangem.core.navigation.AppScreen
import com.tangem.core.navigation.NavigationAction
import com.tangem.core.ui.event.StateEvent
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.data.card.sdk.CardSdkLifecycleObserver
import com.tangem.domain.apptheme.model.AppThemeMode
import com.tangem.domain.card.ScanCardUseCase
import com.tangem.domain.card.repository.CardSdkConfigRepository
import com.tangem.domain.settings.repositories.SettingsRepository
import com.tangem.domain.wallets.legacy.UserWalletsListManager
import com.tangem.domain.wallets.legacy.asLockable
import com.tangem.feature.qrscanning.QrScanningRouter
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

// TODO: will be remove in this task [REDACTED_JIRA]
@Deprecated(message = "Provide UserWalletsListManager using DI")
val userWalletsListManagerSafe: UserWalletsListManager? get() = store.state.globalState.userWalletsListManager

// TODO: will be remove in this task [REDACTED_JIRA]
@Deprecated(message = "Provide UserWalletsListManager using DI")
val userWalletsListManager: UserWalletsListManager get() = userWalletsListManagerSafe!!

@AndroidEntryPoint
class MainActivity : AppCompatActivity(), SnackbarHandler, ActivityResultCallbackHolder {

    @Inject
    lateinit var appStateHolder: AppStateHolder

    /** Router for opening tester menu */
    @Inject
    lateinit var testerRouter: TesterRouter

    @Inject
    lateinit var cardSdkLifecycleObserver: CardSdkLifecycleObserver

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

    internal val viewModel: MainViewModel by viewModels()

    private lateinit var appThemeModeFlow: SharedFlow<AppThemeMode?>

    // TODO: fixme: inject through DI
    private val intentProcessor: IntentProcessor = IntentProcessor()

    private var snackbar: Snackbar? = null
    private val dialogManager = DialogManager()
    private val binding: ActivityMainBinding by viewBinding(ActivityMainBinding::bind)

    private val onActivityResultCallbacks = mutableListOf<OnActivityResultCallback>()

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()

        installAppTheme() // We need to call it before onCreate to prevent unnecessary activity recreation

        super.onCreate(savedInstanceState)

        splashScreen.setKeepOnScreenCondition { viewModel.isSplashScreenShown }

        installActivityDependencies()
        observeAppThemeModeUpdates()

        setContentView(R.layout.activity_main)
        initContent()

        checkForNotificationPermission()
        observeStateUpdates()

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

        cardSdkLifecycleObserver.onCreate(context = this)
        tangemSdkManager = injectedTangemSdkManager
        appStateHolder.tangemSdkManager = tangemSdkManager
        backupService = BackupService.init(cardSdkConfigRepository.sdk, this)
        lockUserWalletsTimer = LockUserWalletsTimer(owner = this, settingsRepository = settingsRepository)

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
            ),
        )
    }

    private fun installAppTheme() {
        appThemeModeFlow = createAppThemeModeFlow()
        val mode = runBlocking { appThemeModeFlow.filterNotNull().first() }

        updateAppTheme(mode)
    }

    private fun observeAppThemeModeUpdates() {
        appThemeModeFlow
            .filterNotNull()
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

    private fun createAppThemeModeFlow(): SharedFlow<AppThemeMode?> {
        val tangemApplication = application as TangemApplication

        return tangemApplication.getAppThemeModeUseCase()
            .map { maybeMode ->
                maybeMode.getOrElse { AppThemeMode.DEFAULT }
            }
            .shareIn(
                scope = lifecycleScope + Dispatchers.IO,
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
        cardSdkLifecycleObserver.onDestroy(this)
        super.onDestroy()
    }

    private fun initIntentHandlers() {
        val hasSavedWalletsProvider = { store.state.globalState.userWalletsListManager?.hasUserWallets == true }
        intentProcessor.addHandler(BackgroundScanIntentHandler(hasSavedWalletsProvider, lifecycleScope))
        intentProcessor.addHandler(WalletConnectLinkIntentHandler())
    }

    private fun updateAppTheme(appThemeMode: AppThemeMode) {
        MutableAppThemeModeHolder.value = appThemeMode
        MutableAppThemeModeHolder.isDarkThemeActive = isDarkTheme()

        val mode = when (appThemeMode) {
            AppThemeMode.FORCE_DARK -> AppCompatDelegate.MODE_NIGHT_YES
            AppThemeMode.FORCE_LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
            AppThemeMode.FOLLOW_SYSTEM -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        }

        setDefaultNightMode(mode)
        delegate.localNightMode = mode
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

        /*
         * FIXME: Test functionality. TangemSdk is null on some devices when HomeAction.Read is called
         *  inside IntentHandler.
         */
        cardSdkLifecycleObserver.onCreate(context = this)

        lifecycleScope.launch {
            intentProcessor.handleIntent(intent)
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
        val backStackIsEmpty = supportFragmentManager.backStackEntryCount == 0
        val isNotScannedBefore = store.state.globalState.scanResponse == null
        val isOnboardingServiceNotActive = !store.state.globalState.onboardingState.onboardingStarted

        when {
            !backStackIsEmpty && isNotScannedBefore && isOnboardingServiceNotActive -> {
                navigateToInitialScreen(intentWhichStartedActivity)
            }
            backStackIsEmpty -> {
                navigateToInitialScreen(intentWhichStartedActivity)
            }
            else -> Unit
        }
    }

    private fun navigateToInitialScreen(intentWhichStartedActivity: Intent?) {
        val canSaveWallets = runCatching { userWalletsListManager.asLockable()?.isLockedSync }
            .fold(onSuccess = { true }, onFailure = { false })

        if (canSaveWallets && userWalletsListManager.hasUserWallets) {
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
                intentProcessor.handleIntent(intentWhichStartedActivity)
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
}