package com.tangem.feature.tester.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.tangem.common.routing.AppRouter
import com.tangem.core.navigation.finisher.AppFinisher
import com.tangem.core.ui.UiDependencies
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.screen.ComposeActivity
import com.tangem.feature.tester.presentation.actions.TesterActionsScreen
import com.tangem.feature.tester.presentation.actions.TesterActionsViewModel
import com.tangem.feature.tester.presentation.environments.ui.EnvironmentTogglesScreen
import com.tangem.feature.tester.presentation.environments.viewmodels.EnvironmentsTogglesViewModel
import com.tangem.feature.tester.presentation.excludedblockchains.ExcludedBlockchainsScreen
import com.tangem.feature.tester.presentation.excludedblockchains.ExcludedBlockchainsViewModel
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
internal class TesterActivity : ComposeActivity() {

    @Inject
    override lateinit var uiDependencies: UiDependencies

    /** Router for inner feature navigation */
    @Inject
    lateinit var testerRouter: TesterRouter

    @Inject
    lateinit var appFinisher: AppFinisher

    @Inject
    lateinit var appRouter: AppRouter

    private val innerTesterRouter: InnerTesterRouter
        get() = requireNotNull(testerRouter as? InnerTesterRouter) {
            "TesterRouter must be InnerTesterRouter for tester feature"
        }

    @Composable
    override fun ScreenContent(modifier: Modifier) {
        val systemBarsColor = TangemTheme.colors.background.secondary
        val systemUiController = rememberSystemUiController()
        systemUiController.setSystemBarsColor(systemBarsColor)

        TesterNavHost()
    }

    @Suppress("TopLevelComposableFunctions")
    @Composable
    private fun TesterNavHost() {
        val navController = rememberNavController().also { innerTesterRouter.setNavController(it) }

        NavHost(navController = navController, startDestination = TesterScreen.MENU.name) {
            composable(route = TesterScreen.MENU.name) {
                TesterMenuScreen(
                    state = TesterMenuContentState(
                        onBackClick = { appRouter.pop { finish() } },
                        onFeatureTogglesClick = { innerTesterRouter.open(TesterScreen.FEATURE_TOGGLES) },
                        onEnvironmentTogglesClick = { innerTesterRouter.open(TesterScreen.ENVIRONMENTS_TOGGLES) },
                        onExcludedBlockchainsClick = {
                            innerTesterRouter.open(TesterScreen.EXCLUDED_BLOCKCHAINS)
                        },
                        onTesterActionsClick = { innerTesterRouter.open(TesterScreen.TESTER_ACTIONS) },
                    ),
                )
            }

            composable(route = TesterScreen.FEATURE_TOGGLES.name) {
                val viewModel = hiltViewModel<FeatureTogglesViewModel>().apply {
                    setupInteractions(innerTesterRouter, appFinisher)
                }

                FeatureTogglesScreen(state = viewModel.uiState)
            }

            composable(route = TesterScreen.ENVIRONMENTS_TOGGLES.name) {
                val viewModel = hiltViewModel<EnvironmentsTogglesViewModel>().apply {
                    setupNavigation(innerTesterRouter)
                }

                EnvironmentTogglesScreen(
                    uiModel = viewModel.uiState.collectAsState().value,
                )
            }

            composable(route = TesterScreen.TESTER_ACTIONS.name) {
                val viewModel = hiltViewModel<TesterActionsViewModel>().apply {
                    setupNavigation(innerTesterRouter)
                }

                TesterActionsScreen(state = viewModel.uiState)
            }

            composable(route = TesterScreen.EXCLUDED_BLOCKCHAINS.name) {
                val viewModel = hiltViewModel<ExcludedBlockchainsViewModel>().apply {
                    setupNavigation(innerTesterRouter, appFinisher)
                }

                val state by viewModel.state.collectAsStateWithLifecycle()
                ExcludedBlockchainsScreen(state = state)
            }
        }
    }
}