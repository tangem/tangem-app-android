package com.tangem.ui.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.tangem.App
import com.tangem.Constant
import com.tangem.di.Navigator
import com.tangem.wallet.BuildConfig
import com.tangem.wallet.R
import com.tangem.wallet.TangemContext
import kotlinx.android.synthetic.main.activity_logo.*
import javax.inject.Inject

class LogoActivity : AppCompatActivity() {
    companion object {
        fun callingIntent(context: Context, autoHide: Boolean): Intent {
            val intent = Intent(context, LogoActivity::class.java)
            intent.putExtra(Constant.EXTRA_AUTO_HIDE, autoHide)
            return intent
        }
    }

    @Inject
    internal lateinit var navigator: Navigator

    private val hideRunnable = Runnable { this.hide() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_logo)

        App.navigatorComponent.inject(this)

        ivLogo.setOnClickListener { hide() }
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)

        // set beta version name
        if (BuildConfig.DEBUG)
            tvAppVersion.text = String.format(getString(R.string.splash_version_name_debug), BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE)
        else
            tvAppVersion.text = String.format(getString(R.string.splash_version_name_release), BuildConfig.VERSION_NAME)

        // set flavor app name
        when (BuildConfig.FLAVOR) {
            Constant.FLAVOR_TANGEM_CARDANO -> {
                tvExtension.visibility = View.VISIBLE
                tvExtension.text = getString(R.string.splash_cardano)
            }
            else -> {
                tvExtension.visibility = View.GONE
            }
        }

        if (intent.getBooleanExtra(Constant.EXTRA_AUTO_HIDE, true))
            ivLogo.postDelayed(hideRunnable, Constant.MILLIS_AUTO_HIDE.toLong())
    }

    private fun hide() {
        when (BuildConfig.FLAVOR) {
            Constant.FLAVOR_TANGEM_CARDANO -> {
                navigator.showPrepareTransaction(this, TangemContext())
            }
            else -> {
                navigator.showMain(this)
            }
        }

        finish()
    }

}