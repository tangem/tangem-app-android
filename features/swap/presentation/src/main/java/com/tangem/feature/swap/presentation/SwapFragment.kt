package com.tangem.feature.swap.presentation

import android.os.Bundle
import androidx.compose.animation.Crossfade
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.fragment.app.viewModels
import com.tangem.core.navigation.ReduxNavController
import com.tangem.core.ui.components.SystemBarsEffect
import com.tangem.core.ui.haptic.HapticManager
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.screen.ComposeFragment
import com.tangem.core.ui.theme.AppThemeModeHolder
import com.tangem.feature.swap.router.CustomTabsManager
import com.tangem.feature.swap.router.SwapNavScreen
import com.tangem.feature.swap.router.SwapRouter
import com.tangem.feature.swap.ui.SwapScreen
import com.tangem.feature.swap.ui.SwapSelectTokenScreen
import com.tangem.feature.swap.ui.SwapSuccessScreen
import com.tangem.feature.swap.viewmodels.SwapViewModel
import dagger.hilt.android.AndroidEntryPoint
import java.lang.ref.WeakReference
import javax.inject.Inject

@AndroidEntryPoint
class SwapFragment : ComposeFragment() {

    @Inject
    override lateinit var appThemeModeHolder: AppThemeModeHolder

    @Inject
    override lateinit var hapticManager: HapticManager

    @Inject
    lateinit var reduxNavController: ReduxNavController

    private val viewModel by viewModels<SwapViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lifecycle.addObserver(viewModel)
        viewModel.setRouter(
            SwapRouter(
                fragmentManager = WeakReference(parentFragmentManager),
                customTabsManager = CustomTabsManager(WeakReference(context)),
                reduxNavController = reduxNavController,
            ),
        )
    }

    @Composable
    override fun ScreenContent(modifier: Modifier) {
        viewModel.onScreenOpened()

        val backgroundColor = TangemTheme.colors.background.secondary
        SystemBarsEffect { setSystemBarsColor(backgroundColor) }

        ScreenContent(viewModel = viewModel)
    }

    @Suppress("TopLevelComposableFunctions")
    @Composable
    private fun ScreenContent(viewModel: SwapViewModel) {
        Crossfade(targetState = viewModel.currentScreen, label = "") { screen ->
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

    companion object {
        const val CURRENCY_BUNDLE_KEY = "swap_currency"
    }
}