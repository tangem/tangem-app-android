@file:Suppress("ObsoleteExperimentalCoroutines")

package com.tangem.ui.activity

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import androidx.navigation.Navigation
import com.scottyab.rootbeer.RootBeer
import com.tangem.App
import com.tangem.Constant
import com.tangem.card_android.android.reader.NfcManager
import com.tangem.di.Navigator
import com.tangem.di.ToastHelper
import com.tangem.ui.dialog.RootFoundDialog
import com.tangem.wallet.BuildConfig
import com.tangem.wallet.R
import javax.inject.Inject

class MainActivity : AppCompatActivity() {

    companion object {
        val TAG: String = MainActivity::class.java.simpleName
        fun callingIntent(context: Context) = Intent(context, MainActivity::class.java)
    }

    lateinit var navController: NavController
    @Inject
    internal lateinit var navigator: Navigator
    @Inject
    internal lateinit var toastHelper: ToastHelper

//    private var onNfcReaderCallback: NfcAdapter.ReaderCallback? = null

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        if (intent != null && (NfcAdapter.ACTION_TECH_DISCOVERED == intent.action || NfcAdapter.ACTION_NDEF_DISCOVERED == intent.action)) {
            val tag = intent.getParcelableExtra<Tag>(NfcAdapter.EXTRA_TAG)
            if (tag != null) {
                val activeFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment)
                                ?.childFragmentManager?.primaryNavigationFragment
                (activeFragment as? NfcAdapter.ReaderCallback)?.onTagDiscovered(tag)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        navController = Navigation.findNavController(this, R.id.nav_host_fragment)

        App.navigatorComponent.inject(this)
        App.toastHelperComponent.inject(this)

        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

        verifyPermissions()

//        // NFC
//        val intent = intent
//        if (intent != null && (NfcAdapter.ACTION_TECH_DISCOVERED == intent.action || NfcAdapter.ACTION_NDEF_DISCOVERED == intent.action)) {
//            val tag = intent.getParcelableExtra<Tag>(NfcAdapter.EXTRA_TAG)
//            if (tag != null && onNfcReaderCallback != null) {
//                onNfcReaderCallback?.onTagDiscovered(tag)
//            }
//        }

        // check if root device
        val rootBeer = RootBeer(this)
        if (rootBeer.isRootedWithoutBusyBoxCheck && !BuildConfig.DEBUG)
            RootFoundDialog().show(supportFragmentManager, RootFoundDialog.TAG)
    }

    private fun verifyPermissions() {
        NfcManager.verifyPermissions(this)
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), Constant.REQUEST_CODE_REQUEST_CAMERA_PERMISSIONS)
        }
    }

}