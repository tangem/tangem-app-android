package com.tangem.feature.swap.presentation

import android.os.Bundle
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.fragment.app.viewModels

import com.tangem.core.ui.UiDependencies
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.screen.ComposeFragment
import com.tangem.feature.swap.router.SwapNavScreen
import com.tangem.feature.swap.ui.SwapScreen
import com.tangem.feature.swap.ui.SwapSelectTokenScreen
import com.tangem.feature.swap.ui.SwapStoriesScreen
import com.tangem.feature.swap.ui.SwapSuccessScreen
import com.tangem.feature.swap.viewmodels.SwapViewModel
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class SwapFragment : ComposeFragment() {

    @Inject
    override lateinit var uiDependencies: UiDependencies

    private val viewModel by viewModels<SwapViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lifecycle.addObserver(viewModel)
    }

    @Composable
    override fun ScreenContent(modifier: Modifier) {
        viewModel.onScreenOpened()
        ScreenContent(viewModel = viewModel)
    }

    @Suppress("TopLevelComposableFunctions")
    @Composable
    private fun ScreenContent(viewModel: SwapViewModel) {
        Crossfade(
            modifier = Modifier.background(TangemTheme.colors.background.secondary),
            targetState = viewModel.currentScreen,
            label = "",
        ) { screen ->
            when (screen) {
                SwapNavScreen.PromoStories -> {
                    val storiesConfig = viewModel.uiState.storiesConfig
                    if (storiesConfig != null) {
                        SwapStoriesScreen(config = storiesConfig)
                    } else {
                        SwapScreen(stateHolder = viewModel.uiState)
                    }
                }
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
                        SwapSelectTokenScreen(state = tokenState, onBack = viewModel.uiState.onBackClicked)
                    } else {
                        SwapScreen(stateHolder = viewModel.uiState)
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        lifecycle.removeObserver(viewModel)
        super.onDestroy()
    }
}