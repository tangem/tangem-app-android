package com.tangem.presentation.activity

import android.content.Context
import android.content.Intent
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.tangem.App
import com.tangem.Constant
import com.tangem.di.Navigator
import com.tangem.domain.wallet.TangemContext
import com.tangem.presentation.fragment.LoadedWallet
import com.tangem.wallet.R
import javax.inject.Inject

class LoadedWalletActivity : AppCompatActivity() {

    @Inject
    lateinit var navigator: Navigator

    companion object {
        fun callingIntent(context: Context, lastTag: Tag, ctx: TangemContext): Intent {
            val intent = Intent(context, LoadedWalletActivity::class.java)
            intent.putExtra(Constant.EXTRA_LAST_DISCOVERED_TAG, lastTag)
            ctx.saveToIntent(intent)
            return intent
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_loaded_wallet)

        App.navigatorComponent?.inject(this)

        if (intent.extras!!.containsKey(NfcAdapter.EXTRA_TAG)) {
            val tag = intent.getParcelableExtra<Tag>(NfcAdapter.EXTRA_TAG)
            if (tag != null) {
                val fragment = supportFragmentManager.findFragmentById(R.id.loaded_wallet_fragment) as LoadedWallet
                fragment.onTagDiscovered(tag)
            }
        }
    }

}