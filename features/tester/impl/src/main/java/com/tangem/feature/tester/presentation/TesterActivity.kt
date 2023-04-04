package com.tangem.feature.tester.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.tangem.core.ui.components.SystemBarsEffect
import com.tangem.core.ui.res.TangemTheme
import com.tangem.feature.tester.presentation.actions.TesterActionsScreen
import com.tangem.feature.tester.presentation.actions.TesterActionsViewModel
import com.tangem.feature.tester.presentation.featuretoggles.ui.FeatureTogglesScreen
import com.tangem.feature.tester.presentation.featuretoggles.viewmodels.FeatureTogglesViewModel
import com.tangem.feature.tester.presentation.menu.state.TesterMenuContentState
import com.tangem.feature.tester.presentation.menu.ui.TesterMenuScreen
import com.tangem.feature.tester.presentation.navigation.InnerTesterRouter
import com.tangem.feature.tester.presentation.navigation.TesterScreen
import com.tangem.features.tester.api.TesterRouter
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/** Activity for testers */
@AndroidEntryPoint
internal class TesterActivity : ComponentActivity() {

    /** Router for inner feature navigation */
    @Inject
    lateinit var testerRouter: TesterRouter

    private val innerTesterRouter: InnerTesterRouter
        get() = requireNotNull(testerRouter as? InnerTesterRouter) {
            "TesterRouter must be InnerTesterRouter for tester feature"
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            TangemTheme {
                val statusBarColor = TangemTheme.colors.background.secondary
                SystemBarsEffect {
                    setStatusBarColor(color = statusBarColor)
                }

                TesterNavHost()
            }
        }
    }

    @Suppress("TopLevelComposableFunctions")
    @Composable
    private fun TesterNavHost() {
        val navController = rememberNavController().also { innerTesterRouter.setNavController(it) }

        NavHost(navController = navController, startDestination = TesterScreen.MENU.name) {
            composable(route = TesterScreen.MENU.name) {
                TesterMenuScreen(
                    state = TesterMenuContentState(
                        onBackClick = innerTesterRouter::back,
                        onFeatureTogglesClick = { innerTesterRouter.open(TesterScreen.FEATURE_TOGGLES) },
                        onTesterActionsClick = { innerTesterRouter.open(TesterScreen.TESTER_ACTIONS) },
                    ),
                )
            }

            composable(route = TesterScreen.FEATURE_TOGGLES.name) {
                val viewModel = hiltViewModel<FeatureTogglesViewModel>().apply {
                    setupNavigation(innerTesterRouter)
                }

                FeatureTogglesScreen(state = viewModel.uiState)
            }

            composable(route = TesterScreen.TESTER_ACTIONS.name) {
                val viewModel = hiltViewModel<TesterActionsViewModel>().apply {
                    setupNavigation(innerTesterRouter)
                }

                TesterActionsScreen(state = viewModel.uiState)
            }
        }
    }
}
