package com.tangem.tap

import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode
import androidx.core.os.bundleOf
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import arrow.core.getOrElse
import by.kirich1409.viewbindingdelegate.viewBinding
import com.google.android.material.snackbar.Snackbar
import com.tangem.core.navigation.AppScreen
import com.tangem.core.navigation.NavigationAction
import com.tangem.data.card.sdk.CardSdkLifecycleObserver
import com.tangem.domain.apptheme.model.AppThemeMode
import com.tangem.domain.card.ScanCardUseCase
import com.tangem.domain.card.repository.CardSdkConfigRepository
import com.tangem.domain.wallets.legacy.UserWalletsListManager
import com.tangem.features.managetokens.navigation.ManageTokensRouter
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
import com.tangem.tap.common.shop.googlepay.GooglePayService
import com.tangem.tap.common.shop.googlepay.GooglePayService.Companion.LOAD_PAYMENT_DATA_REQUEST_CODE
import com.tangem.tap.common.shop.googlepay.GooglePayUtil.createPaymentsClient
import com.tangem.tap.domain.TangemSdkManager
import com.tangem.tap.domain.walletconnect2.domain.WalletConnectInteractor
import com.tangem.tap.features.intentHandler.IntentProcessor
import com.tangem.tap.features.intentHandler.handlers.BackgroundScanIntentHandler
import com.tangem.tap.features.intentHandler.handlers.BuyCurrencyIntentHandler
import com.tangem.tap.features.intentHandler.handlers.SellCurrencyIntentHandler
import com.tangem.tap.features.intentHandler.handlers.WalletConnectLinkIntentHandler
import com.tangem.tap.features.onboarding.products.wallet.redux.BackupAction
import com.tangem.tap.features.shop.redux.ShopAction
import com.tangem.tap.features.welcome.ui.WelcomeFragment
import com.tangem.tap.proxy.AppStateHolder
import com.tangem.tap.proxy.redux.DaggerGraphAction
import com.tangem.utils.coroutines.FeatureCoroutineExceptionHandler
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
// [REDACTED_TODO_COMMENT]
val userWalletsListManagerSafe: UserWalletsListManager?
    get() = store.state.globalState.userWalletsListManager
val userWalletsListManager: UserWalletsListManager
    get() = userWalletsListManagerSafe!!

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
    lateinit var manageTokensRouter: ManageTokensRouter

    @Inject
    lateinit var walletConnectInteractor: WalletConnectInteractor

    private lateinit var appThemeModeFlow: SharedFlow<AppThemeMode?>
// [REDACTED_TODO_COMMENT]
    private val intentProcessor: IntentProcessor = IntentProcessor()

    private var snackbar: Snackbar? = null
    private val dialogManager = DialogManager()
    private val binding: ActivityMainBinding by viewBinding(ActivityMainBinding::bind)

    private val onActivityResultCallbacks = mutableListOf<OnActivityResultCallback>()

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        installAppTheme() // We need to call it before onCreate to prevent unnecessary activity recreation

        super.onCreate(savedInstanceState)

        installActivityDependencies()
        observeAppThemeModeUpdates()

        setContentView(R.layout.activity_main)
        initContent()

        checkGooglePayAvailability()
    }

    private fun installActivityDependencies() {
        store.dispatch(NavigationAction.ActivityCreated(WeakReference(this)))

        cardSdkLifecycleObserver.onCreate(context = this)
        tangemSdkManager = injectedTangemSdkManager
        appStateHolder.tangemSdkManager = tangemSdkManager
        backupService = BackupService.init(cardSdkConfigRepository.sdk, this)
        lockUserWalletsTimer = LockUserWalletsTimer(owner = this)

        initIntentHandlers()

        store.dispatch(
            DaggerGraphAction.SetActivityDependencies(
                testerRouter = testerRouter,
                scanCardUseCase = scanCardUseCase,
                walletRouter = walletRouter,
                walletConnectInteractor = walletConnectInteractor,
                tokenDetailsRouter = tokenDetailsRouter,
                manageTokensRouter = manageTokensRouter,
                cardSdkConfigRepository = cardSdkConfigRepository,
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

    private fun checkGooglePayAvailability() {
        store.dispatch(
            ShopAction.CheckIfGooglePayAvailable(
                GooglePayService(createPaymentsClient(this), this),
            ),
        )
    }

    private fun createAppThemeModeFlow(): SharedFlow<AppThemeMode?> {
        val tapApplication = application as TapApplication
        val featureToggle = tapApplication.darkThemeFeatureToggle

        return if (featureToggle.isDarkThemeEnabled) {
            tapApplication.getAppThemeModeUseCase()
                .map { maybeMode ->
                    maybeMode.getOrElse { AppThemeMode.DEFAULT }
                }
                .shareIn(
                    scope = lifecycleScope + Dispatchers.IO,
                    started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
                )
        } else {
            MutableStateFlow(AppThemeMode.FORCE_LIGHT)
        }
    }

    override fun onStart() {
        super.onStart()
        dialogManager.onStart(this)
    }

    override fun onResume() {
        super.onResume()
// [REDACTED_TODO_COMMENT]
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
        intentProcessor.addHandler(BuyCurrencyIntentHandler())
        intentProcessor.addHandler(SellCurrencyIntentHandler())
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
* [REDACTED_TODO_COMMENT]
         *  inside IntentHandler.
         */
        cardSdkLifecycleObserver.onCreate(context = this)

        lifecycleScope.launch {
            intentProcessor.handleIntent(intent)
        }
    }

    override fun showSnackbar(text: Int, buttonTitle: Int?, action: View.OnClickListener?) {
        if (snackbar != null) return

        snackbar = Snackbar.make(
            binding.fragmentContainer,
            getString(text),
            Snackbar.LENGTH_INDEFINITE,
        )
        if (buttonTitle != null && action != null) {
            snackbar?.setAction(getString(buttonTitle), action)
        }
        snackbar?.show()
    }

    override fun dismissSnackbar() {
        snackbar?.dismiss()
        snackbar = null
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        onActivityResultCallbacks.forEach { it(requestCode, resultCode, data) }
        when (requestCode) {
            LOAD_PAYMENT_DATA_REQUEST_CODE -> {
                store.dispatch(
                    ShopAction.BuyWithGooglePay.HandleGooglePayResponse(resultCode, data),
                )
            }
        }
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

    private fun navigateToInitialScreenIfNeeded(intentWhichStartedActivity: Intent?) {
        val backStackIsEmpty = supportFragmentManager.backStackEntryCount == 0
        val isNotScannedBefore = store.state.globalState.scanResponse == null
        val isOnboardingServiceNotActive = store.state.globalState.onboardingState.onboardingStarted
        val isShopNotOpened = store.state.shopState.total != null

        when {
            !backStackIsEmpty && isNotScannedBefore && isOnboardingServiceNotActive && isShopNotOpened -> {
                navigateToInitialScreen(intentWhichStartedActivity)
            }
            backStackIsEmpty -> {
                navigateToInitialScreen(intentWhichStartedActivity)
            }
            else -> Unit
        }
    }

    private fun navigateToInitialScreen(intentWhichStartedActivity: Intent?) {
        if (store.state.globalState.userWalletsListManager?.hasUserWallets == true) {
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
}
