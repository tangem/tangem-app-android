package com.tangem.tap.features.customtoken.impl.presentation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.core.view.WindowCompat
import androidx.fragment.app.Fragment
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.transition.TransitionInflater
import com.tangem.core.ui.res.TangemTheme
import com.tangem.tap.features.customtoken.impl.presentation.ui.AddCustomTokenScreen
import com.tangem.tap.features.customtoken.impl.presentation.viewmodels.AddCustomTokenViewModel
import com.tangem.wallet.R
import dagger.hilt.android.AndroidEntryPoint

/**
 * Add custom token screen
 *
[REDACTED_AUTHOR]
 */
@AndroidEntryPoint
internal class AddCustomTokenFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        activity?.window?.let { WindowCompat.setDecorFitsSystemWindows(it, true) }

        with(TransitionInflater.from(requireContext())) {
            enterTransition = inflateTransition(R.transition.fade)
            exitTransition = inflateTransition(R.transition.fade)
        }

        return ComposeView(inflater.context).apply {
            setContent {
                isTransitionGroup = true

                val viewModel = hiltViewModel<AddCustomTokenViewModel>().apply {
                    LocalLifecycleOwner.current.lifecycle.addObserver(this)
                }

                TangemTheme {
                    AddCustomTokenScreen(stateHolder = viewModel.uiState)
                }
            }
        }
    }
}