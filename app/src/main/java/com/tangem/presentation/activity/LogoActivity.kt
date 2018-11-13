package com.tangem.presentation.activity

import android.annotation.SuppressLint
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import com.tangem.App
import com.tangem.di.Navigator
import com.tangem.wallet.BuildConfig
import com.tangem.wallet.R
import kotlinx.android.synthetic.main.activity_logo.*
import javax.inject.Inject

class LogoActivity : AppCompatActivity() {

    companion object {
        val TAG: String = LogoActivity::class.java.simpleName

        const val EXTRA_AUTO_HIDE = "extra_auto_hide"
        const val MILLIS_AUTO_HIDE = 1000
    }

    private val hideRunnable = Runnable { this.hide() }

    @Inject
    internal lateinit var navigator: Navigator

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

        if (intent.getBooleanExtra(EXTRA_AUTO_HIDE, true))
            ivLogo.postDelayed(hideRunnable, MILLIS_AUTO_HIDE.toLong())
    }

    private fun hide() {
        navigator.showMain(this)
        finish()
    }

}