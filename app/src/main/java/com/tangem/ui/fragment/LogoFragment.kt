package com.tangem.ui.fragment

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import com.tangem.Constant
import com.tangem.wallet.BuildConfig
import com.tangem.wallet.R
import com.tangem.wallet.TangemContext
import kotlinx.android.synthetic.main.fragment_logo.*

class LogoFragment : BaseFragment() {
    companion object {
        fun callingIntent(context: Context, autoHide: Boolean): Intent {
            val intent = Intent(context, LogoFragment::class.java)
            intent.putExtra(Constant.EXTRA_AUTO_HIDE, autoHide)
            return intent
        }
    }

    override val layoutId = R.layout.fragment_logo

    private val hideRunnable = Runnable { this.hide() }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        clLogoContainer.setOnClickListener { hide() }
        // set beta version name
        if (BuildConfig.DEBUG)
            tvAppVersion.text = String.format(getString(R.string.version_name_debug), BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE)
        else
            tvAppVersion.text = String.format(getString(R.string.version_name_release), BuildConfig.VERSION_NAME)

        // set flavor app name
        when (BuildConfig.FLAVOR) {
            Constant.FLAVOR_TANGEM_CARDANO -> {
                tvExtension.visibility = View.VISIBLE
                tvExtension.text = getString(R.string.cardano)
            }
            else -> {
                tvExtension.visibility = View.GONE
            }
        }

        if (arguments?.getBoolean(Constant.EXTRA_AUTO_HIDE, true) != false)
            ivLogo.postDelayed(hideRunnable, Constant.MILLIS_AUTO_HIDE.toLong())
    }

    private fun hide() {
        when (BuildConfig.FLAVOR) {
            Constant.FLAVOR_TANGEM_CARDANO -> {
                navigateToDestination(R.id.action_logoFragment_to_prepareTransactionFragment,
                        Bundle().apply { TangemContext().saveToBundle(this) })
            }
            else -> {
                navigateToDestination(R.id.action_logoFragment_to_main)
            }
        }
    }

}