package com.tangem.ui.activity

import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.findNavController
import com.scottyab.rootbeer.RootBeer
import com.tangem.App
import com.tangem.di.ToastHelper
import com.tangem.tangem_sdk.android.nfc.NfcLifecycleObserver
import com.tangem.tangem_sdk.android.reader.NfcManager
import com.tangem.ui.dialog.RootFoundDialog
import com.tangem.wallet.BuildConfig
import com.tangem.wallet.R
import javax.inject.Inject

class MainActivity : AppCompatActivity(), NfcAdapter.ReaderCallback {

    companion object {
        val TAG: String = MainActivity::class.java.simpleName
        fun callingIntent(context: Context) = Intent(context, MainActivity::class.java)
    }

    @Inject
    internal lateinit var toastHelper: ToastHelper
    lateinit var viewModel: GlobalViewModel
    lateinit var nfcManager: NfcManager

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        if (intent != null && (NfcAdapter.ACTION_TECH_DISCOVERED == intent.action || NfcAdapter.ACTION_NDEF_DISCOVERED == intent.action)) {
            val tag = intent.getParcelableExtra<Tag>(NfcAdapter.EXTRA_TAG)
            if (tag != null) {
                navigateSafelyToMainFragment()
                onTagDiscovered(tag)
            }
        }
    }

    private fun navigateSafelyToMainFragment() {
        try {
            findNavController(R.id.nav_host_fragment).popBackStack()
        } catch (e: IllegalArgumentException) {
            Log.w(this::class.java.simpleName, e.message)
        } catch (e: IllegalStateException) {
            Log.w(this::class.java.simpleName, e.message)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        viewModel = ViewModelProviders.of(this).get(GlobalViewModel::class.java)

        App.toastHelperComponent.inject(this)

        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

        nfcManager = NfcManager(this, this)
        lifecycle.addObserver(NfcLifecycleObserver(nfcManager))

        // check if root device
        val rootBeer = RootBeer(this)
        if (rootBeer.isRootedWithoutBusyBoxCheck && !BuildConfig.DEBUG)
            RootFoundDialog().show(supportFragmentManager, RootFoundDialog.TAG)
    }

    override fun onTagDiscovered(tag: Tag) {
        val activeFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment)
                ?.childFragmentManager?.primaryNavigationFragment
        if (activeFragment is NfcAdapter.ReaderCallback) {
            activeFragment.onTagDiscovered(tag)
        } else {
            nfcManager.ignoreTag(tag)
        }
    }

}