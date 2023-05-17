package com.tangem.tap.features.tokens.impl.presentation

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
import com.tangem.tap.features.tokens.impl.presentation.ui.TokensListScreen
import com.tangem.tap.features.tokens.impl.presentation.viewmodels.TokensListViewModel
import com.tangem.wallet.R
import dagger.hilt.android.AndroidEntryPoint

/**
 * Fragment with list of tokens
 *
[REDACTED_AUTHOR]
 */
@AndroidEntryPoint
internal class TokensListFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        activity?.window?.let { WindowCompat.setDecorFitsSystemWindows(it, true) }

        with(TransitionInflater.from(requireContext())) {
            enterTransition = inflateTransition(R.transition.fade)
            exitTransition = inflateTransition(R.transition.fade)
        }

        return ComposeView(inflater.context).apply {
            setContent {
                isTransitionGroup = true

                val viewModel = hiltViewModel<TokensListViewModel>().apply {
                    LocalLifecycleOwner.current.lifecycle.addObserver(this)
                }

                TangemTheme {
                    TokensListScreen(stateHolder = viewModel.uiState)
                }
            }
        }
    }
}