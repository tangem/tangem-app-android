package com.tangem.feature.swap.presentation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.animation.Crossfade
import androidx.compose.ui.platform.ComposeView
import androidx.core.view.WindowCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.tangem.feature.swap.router.SwapRouter
import com.tangem.feature.swap.router.SwapScreen
import com.tangem.feature.swap.ui.SwapScreen
import com.tangem.feature.swap.ui.SwapSuccessScreen
import com.tangem.feature.swap.viewmodels.SwapViewModel
import dagger.hilt.android.AndroidEntryPoint
import java.lang.ref.WeakReference

@AndroidEntryPoint
class SwapFragment : Fragment() {

    private val viewModel by viewModels<SwapViewModel>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        activity?.window?.let { WindowCompat.setDecorFitsSystemWindows(it, true) }
        viewModel.setRouter(SwapRouter(fragmentManager = WeakReference(parentFragmentManager)))
        return ComposeView(inflater.context).apply {
            setContent {
                Crossfade(targetState = viewModel.currentScreen) { screen ->
                    when (screen) {
                        SwapScreen.Main -> SwapScreen(stateHolder = viewModel.uiState)
                        SwapScreen.Success -> {
                            val successState = viewModel.uiState.successState
                            if (successState != null) {
                                SwapSuccessScreen(state = successState, viewModel.uiState.onBackClicked)
                            } else {
                                SwapScreen(stateHolder = viewModel.uiState)
                            }
                        }
                        SwapScreen.SelectToken -> SwapScreen(stateHolder = viewModel.uiState)// TODO: use proper screen
                    }
                }

            }
        }
    }
}