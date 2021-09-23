package com.tangem.tap

import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.snackbar.Snackbar
import com.tangem.TangemSdk
import com.tangem.tangem_sdk_new.extensions.init
import com.tangem.tap.common.DialogManager
import com.tangem.tap.common.IntentHandler
import com.tangem.tap.common.SnackbarHandler
import com.tangem.tap.common.redux.NotificationsHandler
import com.tangem.tap.common.redux.navigation.AppScreen
import com.tangem.tap.common.redux.navigation.NavigationAction
import com.tangem.tap.domain.TangemSdkManager
import com.tangem.tap.features.details.redux.walletconnect.WalletConnectAction
import com.tangem.wallet.R
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import java.io.PrintWriter
import java.io.StringWriter
import java.lang.ref.WeakReference
import kotlin.coroutines.CoroutineContext

lateinit var tangemSdk: TangemSdk
lateinit var tangemSdkManager: TangemSdkManager
var notificationsHandler: NotificationsHandler? = null

private val coroutineContext: CoroutineContext
    get() = Job() + Dispatchers.IO + initCoroutineExceptionHandler()
val scope = CoroutineScope(coroutineContext)

private val mainCoroutineContext: CoroutineContext
    get() = Job() + Dispatchers.Main
val mainScope = CoroutineScope(mainCoroutineContext)

private fun initCoroutineExceptionHandler(): CoroutineExceptionHandler {
    return CoroutineExceptionHandler { _, throwable ->
        val sw = StringWriter()
        throwable.printStackTrace(PrintWriter(sw))
        val exceptionAsString: String = sw.toString()
        Log.e("Coroutine", exceptionAsString)
        throw throwable
    }
}

class MainActivity : AppCompatActivity(), SnackbarHandler {

    private var snackbar: Snackbar? = null
    private val dialogManager = DialogManager()
    private val intentHandler = IntentHandler()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        store.state.globalState.feedbackManager?.updateAcivity(this)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
        store.dispatch(NavigationAction.ActivityCreated(WeakReference(this)))

        tangemSdk = TangemSdk.init(this, TangemSdkManager.config)
        tangemSdkManager = TangemSdkManager(tangemSdk, this)

        store.dispatch(WalletConnectAction.RestoreSessions)
    }

    override fun onResume() {
        super.onResume()
        notificationsHandler = NotificationsHandler(fragment_container)

        val backStackIsEmpty = supportFragmentManager.backStackEntryCount == 0
        val isScannedBefore = store.state.globalState.scanNoteResponse != null
        val isOnboardingStarted = store.state.onboardingState.onboardingData?.scanNoteResponse != null
        if ((backStackIsEmpty || isScannedBefore) && !isOnboardingStarted) {
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
                fragment_container, getString(text), Snackbar.LENGTH_INDEFINITE
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
}