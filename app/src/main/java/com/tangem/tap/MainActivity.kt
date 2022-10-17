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
import com.tangem.domain.common.FeatureCoroutineExceptionHandler
import com.tangem.operations.backup.BackupService
import com.tangem.tangem_sdk_new.extensions.init
import com.tangem.tap.common.ActivityResultCallbackHolder
import com.tangem.tap.common.DialogManager
import com.tangem.tap.common.IntentHandler
import com.tangem.tap.common.OnActivityResultCallback
import com.tangem.tap.common.SnackbarHandler
import com.tangem.tap.common.redux.NotificationsHandler
import com.tangem.tap.common.redux.global.AndroidResources
import com.tangem.tap.common.redux.global.GlobalAction
import com.tangem.tap.common.redux.navigation.AppScreen
import com.tangem.tap.common.redux.navigation.NavigationAction
import com.tangem.tap.common.shop.GooglePayService
import com.tangem.tap.common.shop.GooglePayService.Companion.LOAD_PAYMENT_DATA_REQUEST_CODE
import com.tangem.tap.common.shop.googlepay.GooglePayUtil.createPaymentsClient
import com.tangem.tap.domain.TangemSdkManager
import com.tangem.tap.features.shop.redux.ShopAction
import com.tangem.wallet.R
import com.tangem.wallet.databinding.ActivityMainBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import java.lang.ref.WeakReference
import kotlin.coroutines.CoroutineContext

lateinit var tangemSdk: TangemSdk
lateinit var tangemSdkManager: TangemSdkManager
lateinit var backupService: BackupService
var notificationsHandler: NotificationsHandler? = null

private val coroutineContext: CoroutineContext
    get() = Job() + Dispatchers.IO + FeatureCoroutineExceptionHandler.create("scope")
val scope = CoroutineScope(coroutineContext)

private val mainCoroutineContext: CoroutineContext
    get() = Job() + Dispatchers.Main + FeatureCoroutineExceptionHandler.create("mainScope")
val mainScope = CoroutineScope(mainCoroutineContext)

class MainActivity : AppCompatActivity(), SnackbarHandler, ActivityResultCallbackHolder {

    private var snackbar: Snackbar? = null
    private val dialogManager = DialogManager()
    private val intentHandler = IntentHandler()
    private val binding: ActivityMainBinding by viewBinding(ActivityMainBinding::bind)

    private val onActivityResultCallbacks = mutableListOf<OnActivityResultCallback>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        systemActions()
        store.dispatch(NavigationAction.ActivityCreated(WeakReference(this)))

        tangemSdk = TangemSdk.init(this, TangemSdkManager.config)
        tangemSdkManager = TangemSdkManager(tangemSdk, this)
        backupService = BackupService.init(tangemSdk, this)

        store.dispatch(GlobalAction.SetResources(getAndroidResources()))
        store.dispatch(
            ShopAction.CheckIfGooglePayAvailable(
                GooglePayService(createPaymentsClient(this), this),
            ),
        )
    }

    private fun getAndroidResources(): AndroidResources {
        return AndroidResources(
            AndroidResources.RString(
                R.string.copy_toast_msg,
                R.string.details_notification_erase_wallet_not_possible,
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

        val backStackIsEmpty = supportFragmentManager.backStackEntryCount == 0
        val isScannedBefore = store.state.globalState.scanResponse != null
        val isOnboardingServiceActive = store.state.globalState.onboardingState.onboardingStarted
        val shopOpened = store.state.shopState.total != null
        if (backStackIsEmpty || (!isOnboardingServiceActive && !isScannedBefore && !shopOpened)) {
            store.dispatch(NavigationAction.NavigateTo(AppScreen.Home))
        }
        intentHandler.handleIntent(intent)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        intentHandler.handleIntent(intent)
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
        store.dispatch(NavigationAction.ActivityDestroyed)
        super.onDestroy()
    }

    override fun showSnackbar(text: Int, buttonTitle: Int?, action: View.OnClickListener?) {
        if (snackbar != null) return

        snackbar = Snackbar.make(
            binding.fragmentContainer, getString(text), Snackbar.LENGTH_INDEFINITE,
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
}
