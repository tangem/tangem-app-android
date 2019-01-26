package com.tangem.presentation.activity

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.tangem.App
import com.tangem.Constant
import com.tangem.di.Navigator
import com.tangem.wallet.BuildConfig
import com.tangem.wallet.R
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

        App.getNavigatorComponent().inject(this)

        ivLogo.setOnClickListener { hide() }
    }

    @SuppressLint("SetTextI18n")
    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)

        if (BuildConfig.DEBUG)
            tvAppVersion.text = "BETA v." + BuildConfig.VERSION_NAME + "\n" + "dev" + "\n" + "build " + BuildConfig.VERSION_CODE
        else
            tvAppVersion.text = "BETA v." + BuildConfig.VERSION_NAME

        if (intent.getBooleanExtra(Constant.EXTRA_AUTO_HIDE, true))
            ivLogo.postDelayed(hideRunnable, Constant.MILLIS_AUTO_HIDE.toLong())
    }

    private fun hide() {
        navigator.showMain(this)
        finish()
    }

}