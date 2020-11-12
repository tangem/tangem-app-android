package com.tangem.tap

import android.content.Intent
import android.content.pm.ActivityInfo
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.tangem.CardFilter
import com.tangem.Config
import com.tangem.Log
import com.tangem.TangemSdk
import com.tangem.common.extensions.CardType
import com.tangem.tangem_sdk_new.extensions.init
import com.tangem.tap.common.redux.NotificationsHandler
import com.tangem.tap.common.redux.navigation.AppScreen
import com.tangem.tap.common.redux.navigation.NavigationAction
import com.tangem.tap.domain.TangemSdkManager
import com.tangem.tap.features.home.redux.HomeAction
import com.tangem.wallet.R
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import java.io.PrintWriter
import java.io.StringWriter
import java.lang.ref.WeakReference
import java.util.*
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
        Log.e("TangemSdk", exceptionAsString)
        throw throwable
    }
}

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
        store.dispatch(NavigationAction.ActivityCreated(WeakReference(this)))

        tangemSdk = TangemSdk.init(
                this, Config(cardFilter = CardFilter(EnumSet.allOf(CardType::class.java)))
        )
        tangemSdkManager = TangemSdkManager(this)
    }

    override fun onResume() {
        super.onResume()
        notificationsHandler = NotificationsHandler(fragment_container)
        if (supportFragmentManager.backStackEntryCount == 0 ||
                store.state.globalState.scanNoteResponse == null) {
            store.dispatch(HomeAction.CheckIfFirstLaunch)
            store.dispatch(NavigationAction.NavigateTo(AppScreen.Home))
        }
        handleBackgroundScan(intent)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        handleBackgroundScan(intent)
    }

    private fun handleBackgroundScan(intent: Intent?) {
        if (intent != null && (NfcAdapter.ACTION_TECH_DISCOVERED == intent.action ||
                        NfcAdapter.ACTION_NDEF_DISCOVERED == intent.action
                        )) {
            val tag = intent.getParcelableExtra<Tag>(NfcAdapter.EXTRA_TAG)
            if (tag != null) {
                intent.action = null
                store.dispatch(NavigationAction.NavigateTo(AppScreen.Home, false))
                store.dispatch(HomeAction.ReadCard)
            }
        }
    }

    override fun onStop() {
        notificationsHandler = null
        super.onStop()
    }

    override fun onDestroy() {
        store.dispatch(NavigationAction.ActivityDestroyed)
        super.onDestroy()
    }
}