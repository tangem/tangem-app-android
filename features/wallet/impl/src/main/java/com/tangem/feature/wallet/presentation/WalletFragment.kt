package com.tangem.feature.wallet.presentation

import android.os.Bundle
import android.transition.TransitionInflater
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import com.tangem.core.ui.components.SystemBarsEffect
import com.tangem.core.ui.res.TangemTheme
import com.tangem.feature.wallet.impl.R
import com.tangem.feature.wallet.presentation.router.InnerWalletRouter
import com.tangem.features.wallet.navigation.WalletRouter
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Wallet fragment
 *
[REDACTED_AUTHOR]
 */
@AndroidEntryPoint
internal class WalletFragment : Fragment() {

    /** Feature router */
    @Inject
    internal lateinit var walletRouter: WalletRouter

    private val _walletRouter: InnerWalletRouter
        get() = requireNotNull(walletRouter as? InnerWalletRouter) {
            "_walletRouter should be instance of InnerWalletRouter"
        }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        with(TransitionInflater.from(requireContext())) {
            enterTransition = inflateTransition(R.transition.slide_right)
            exitTransition = inflateTransition(R.transition.fade)
        }

        return ComposeView(inflater.context).apply {
            setContent {
                TangemTheme {
                    val systemBarsColor = TangemTheme.colors.background.secondary
                    SystemBarsEffect {
                        setSystemBarsColor(systemBarsColor)
                    }

                    isTransitionGroup = true
                    _walletRouter.Initialize(fragmentManager = requireActivity().supportFragmentManager)
                }
            }
        }
    }

    companion object {

        /** Create wallet fragment instance */
        fun create(): WalletFragment = WalletFragment()
    }
}