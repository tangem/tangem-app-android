package com.tangem.feature.swap.presentation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.animation.Crossfade
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.tangem.feature.swap.router.CustomTabsManager
import com.tangem.feature.swap.router.SwapNavScreen
import com.tangem.feature.swap.router.SwapRouter
import com.tangem.feature.swap.ui.SwapScreen
import com.tangem.feature.swap.ui.SwapSelectTokenScreen
import com.tangem.feature.swap.ui.SwapSuccessScreen
import com.tangem.feature.swap.viewmodels.SwapViewModel
import dagger.hilt.android.AndroidEntryPoint
import java.lang.ref.WeakReference

@AndroidEntryPoint
class SwapFragment : Fragment() {

    private val viewModel by viewModels<SwapViewModel>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        viewModel.setRouter(
            SwapRouter(
                fragmentManager = WeakReference(parentFragmentManager),
                customTabsManager = CustomTabsManager(WeakReference(context)),
            ),
        )
        viewModel.onScreenOpened()

        return ComposeView(inflater.context).apply {
            setContent {
                ScreenContent(viewModel = viewModel)
            }
        }
    }

    @Suppress("TopLevelComposableFunctions")
    @Composable
    private fun ScreenContent(viewModel: SwapViewModel) {
        Crossfade(targetState = viewModel.currentScreen) { screen ->
            when (screen) {
                SwapNavScreen.Main -> SwapScreen(stateHolder = viewModel.uiState)
                SwapNavScreen.Success -> {
                    val successState = viewModel.uiState.successState
                    if (successState != null) {
                        SwapSuccessScreen(state = successState, viewModel.uiState.onBackClicked)
                    } else {
                        SwapScreen(stateHolder = viewModel.uiState)
                    }
                }
                SwapNavScreen.SelectToken -> {
                    val tokenState = viewModel.uiState.selectTokenState
                    if (tokenState != null) {
                        SwapSelectTokenScreen(
                            state = tokenState,
                            onSearchFocusChange = viewModel.uiState.onSearchFocusChange,
                            onBack = viewModel.uiState.onBackClicked,
                        )
                    } else {
                        SwapScreen(stateHolder = viewModel.uiState)
                    }
                }
            }
        }
    }

    companion object {
        const val CURRENCY_BUNDLE_KEY = "swap_currency"
        const val DERIVATION_PATH = "DERIVATION_STYLE"
    }
}
