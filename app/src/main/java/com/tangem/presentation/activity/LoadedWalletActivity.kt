package com.tangem.presentation.activity

import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import com.tangem.App
import com.tangem.presentation.fragment.LoadedWallet
import com.tangem.wallet.R
import retrofit2.Retrofit

class LoadedWalletActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_MODIFICATION = "modification"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_loaded_wallet)

        MainActivity.commonInit(applicationContext)

        if (intent.extras!!.containsKey(NfcAdapter.EXTRA_TAG)) {
            val tag = intent.getParcelableExtra<Tag>(NfcAdapter.EXTRA_TAG)
            if (tag != null) {
                val fragment = supportFragmentManager.findFragmentById(R.id.loaded_wallet_fragment) as LoadedWallet
                fragment.onTagDiscovered(tag)
            }
        }
    }

}