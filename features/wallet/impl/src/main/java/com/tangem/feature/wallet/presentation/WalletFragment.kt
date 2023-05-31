package com.tangem.feature.wallet.presentation

import android.os.Bundle
import android.transition.TransitionInflater
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.core.view.WindowCompat
import androidx.fragment.app.Fragment
import com.tangem.core.ui.res.TangemTheme
import com.tangem.feature.wallet.impl.R
import dagger.hilt.android.AndroidEntryPoint

/**
 * Wallet fragment
 *
 * @author Andrew Khokhlov on 29/05/2023
 */
@AndroidEntryPoint
class WalletFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        activity?.window?.let { WindowCompat.setDecorFitsSystemWindows(it, true) }

        with(TransitionInflater.from(requireContext())) {
            enterTransition = inflateTransition(R.transition.slide_right)
            exitTransition = inflateTransition(R.transition.fade)
        }

        return ComposeView(inflater.context).apply {
            setContent {
                isTransitionGroup = true

                TangemTheme {
                    // TODO: AND-3644 Call WalletScreen
                }
            }
        }
    }
}
