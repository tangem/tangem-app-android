package com.tangem.tap

import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import by.kirich1409.viewbindingdelegate.viewBinding
import com.google.android.material.snackbar.Snackbar
import com.tangem.TangemSdk
import com.tangem.operations.backup.BackupService
import com.tangem.tangem_sdk_new.extensions.init
import com.tangem.tap.common.ActivityResultCallbackHolder
import com.tangem.tap.common.DialogManager
import com.tangem.tap.common.OnActivityResultCallback
import com.tangem.tap.common.SnackbarHandler
import com.tangem.tap.common.extensions.dispatchOnMain
import com.tangem.tap.common.redux.NotificationsHandler
import com.tangem.tap.common.redux.navigation.AppScreen
import com.tangem.tap.common.redux.navigation.NavigationAction
import com.tangem.tap.common.shop.googlepay.GooglePayService
import com.tangem.tap.common.shop.googlepay.GooglePayService.Companion.LOAD_PAYMENT_DATA_REQUEST_CODE
import com.tangem.tap.common.shop.googlepay.GooglePayUtil.createPaymentsClient
import com.tangem.tap.domain.TangemSdkManager
import com.tangem.tap.domain.userWalletList.UserWalletsListManager
import com.tangem.tap.domain.userWalletList.di.provideBiometricImplementation
import com.tangem.tap.features.onboarding.products.wallet.redux.BackupAction
import com.tangem.tap.features.shop.redux.ShopAction
import com.tangem.tap.features.welcome.redux.WelcomeAction
import com.tangem.tap.proxy.AppStateHolder
import com.tangem.utils.coroutines.FeatureCoroutineExceptionHandler
import com.tangem.wallet.R
import com.tangem.wallet.databinding.ActivityMainBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import java.lang.ref.WeakReference
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

lateinit var tangemSdk: TangemSdk
lateinit var tangemSdkManager: TangemSdkManager
lateinit var backupService: BackupService
lateinit var userWalletsListManager: UserWalletsListManager
internal var lockUserWalletsTimer: LockUserWalletsTimer? = null
    private set
var userWalletsListManagerSafe: UserWalletsListManager? = null
    private set
var notificationsHandler: NotificationsHandler? = null

private val coroutineContext: CoroutineContext
    get() = Job() + Dispatchers.IO + FeatureCoroutineExceptionHandler.create("scope")
val scope = CoroutineScope(coroutineContext)

private val mainCoroutineContext: CoroutineContext
    get() = Job() + Dispatchers.Main + FeatureCoroutineExceptionHandler.create("mainScope")
val mainScope = CoroutineScope(mainCoroutineContext)

@AndroidEntryPoint
class MainActivity : AppCompatActivity(), SnackbarHandler, ActivityResultCallbackHolder {

    @Inject
    lateinit var appStateHolder: AppStateHolder

    private var snackbar: Snackbar? = null
    private val dialogManager = DialogManager()
    private val binding: ActivityMainBinding by viewBinding(ActivityMainBinding::bind)

    private val onActivityResultCallbacks = mutableListOf<OnActivityResultCallback>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        systemActions()
        store.dispatch(NavigationAction.ActivityCreated(WeakReference(this)))

        tangemSdk = TangemSdk.init(this, TangemSdkManager.config)
        tangemSdkManager = TangemSdkManager(tangemSdk, this)
        appStateHolder.tangemSdkManager = tangemSdkManager
        appStateHolder.tangemSdk = tangemSdk
        backupService = BackupService.init(tangemSdk, this)
        userWalletsListManager = UserWalletsListManager.provideBiometricImplementation(
            context = applicationContext,
            tangemSdkManager = tangemSdkManager,
        )
        appStateHolder.userWalletsListManager = userWalletsListManager
        userWalletsListManagerSafe = userWalletsListManager
        lockUserWalletsTimer = LockUserWalletsTimer(owner = this)

        store.dispatch(
            ShopAction.CheckIfGooglePayAvailable(
                GooglePayService(createPaymentsClient(this), this),
            ),
        )
    }

    private fun systemActions() {
        WindowCompat.setDecorFitsSystemWindows(window, false)

        val windowInsetsController = WindowInsetsControllerCompat(window, binding.root)
        windowInsetsController.isAppearanceLightStatusBars = true
        windowInsetsController.isAppearanceLightNavigationBars = true

        supportFragmentManager.registerFragmentLifecycleCallbacks(
            NavBarInsetsFragmentLifecycleCallback(),
            true,
        )

        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
    }

    override fun onResume() {
        super.onResume()
        notificationsHandler = NotificationsHandler(binding.fragmentContainer)

        navigateToInitialScreenIfNeeded()

        intentHandler.handleBackgroundScan(intent)
        intentHandler.handleSellCurrencyCallback(intent)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        intentHandler.handleBackgroundScan(intent)
        intentHandler.handleSellCurrencyCallback(intent)
        intentHandler.handleWalletConnectLink(intent)
    }

    override fun onStart() {
        super.onStart()
        dialogManager.onStart(this)
    }

    override fun onStop() {
        notificationsHandler = null
        dialogManager.onStop()
        super.onStop()
    }

    override fun onDestroy() {
        store.dispatch(NavigationAction.ActivityDestroyed(WeakReference(this)))
        super.onDestroy()
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

    private fun navigateToInitialScreenIfNeeded() {
        val backStackIsEmpty = supportFragmentManager.backStackEntryCount == 0
        val isNotScannedBefore = store.state.globalState.scanResponse == null
        val isOnboardingServiceNotActive = store.state.globalState.onboardingState.onboardingStarted
        val isShopNotOpened = store.state.shopState.total != null
        when {
            !backStackIsEmpty && isNotScannedBefore && isOnboardingServiceNotActive && isShopNotOpened -> {
                navigateToInitialScreen()
            }
            backStackIsEmpty -> {
                navigateToInitialScreen()
            }
        }
    }

    private fun navigateToInitialScreen() {
        if (userWalletsListManager.hasSavedUserWallets) {
            store.dispatchOnMain(WelcomeAction.HandleDeepLink(intent))
            store.dispatchOnMain(NavigationAction.NavigateTo(AppScreen.Welcome))
            store.dispatchOnMain(WelcomeAction.ProceedWithBiometrics)
        } else {
            store.dispatchOnMain(NavigationAction.NavigateTo(AppScreen.Home))
            intentHandler.handleWalletConnectLink(intent)
        }
        store.dispatch(BackupAction.CheckForUnfinishedBackup)
    }
}
