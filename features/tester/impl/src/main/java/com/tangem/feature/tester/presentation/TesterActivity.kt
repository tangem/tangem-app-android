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
import com.tangem.feature.tester.presentation.menu.state.TesterMenuUM
import com.tangem.feature.tester.presentation.menu.state.TesterMenuUM.ButtonUM
import com.tangem.feature.tester.presentation.menu.ui.TesterMenuScreen
import com.tangem.feature.tester.presentation.navigation.InnerTesterRouter
import com.tangem.feature.tester.presentation.navigation.TesterScreen
import com.tangem.feature.tester.presentation.providers.ui.BlockchainProvidersScreen
import com.tangem.feature.tester.presentation.providers.viewmodel.BlockchainProvidersViewModel
import com.tangem.feature.tester.presentation.testpush.ui.TestPushScreen
import com.tangem.feature.tester.presentation.testpush.viewmodel.TestPushViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.collections.immutable.persistentSetOf
import javax.inject.Inject

/** Activity for testers */
@AndroidEntryPoint
internal class TesterActivity : ComposeActivity() {

    @Inject
    override lateinit var uiDependencies: UiDependencies

    @Inject
    lateinit var innerTesterRouter: InnerTesterRouter

    @Inject
    lateinit var appFinisher: AppFinisher

    @Inject
    lateinit var appRouter: AppRouter

    @Composable
    override fun ScreenContent(modifier: Modifier) {
        val systemBarsColor = TangemTheme.colors.background.secondary
        val systemUiController = rememberSystemUiController()
        systemUiController.setSystemBarsColor(systemBarsColor)

        TesterNavHost()
    }

    @Suppress("TopLevelComposableFunctions", "LongMethod")
    @Composable
    private fun TesterNavHost() {
        val navController = rememberNavController().also { innerTesterRouter.setNavController(it) }

        NavHost(navController = navController, startDestination = TesterScreen.MENU.name) {
            composable(route = TesterScreen.MENU.name) {
                TesterMenuScreen(
                    state = TesterMenuUM(
                        onBackClick = { appRouter.pop { finish() } },
                        buttons = persistentSetOf(
                            ButtonUM.FEATURE_TOGGLES,
                            ButtonUM.EXCLUDED_BLOCKCHAINS,
                            ButtonUM.ENVIRONMENT_TOGGLES,
                            ButtonUM.BLOCKCHAIN_PROVIDERS,
                            ButtonUM.TESTER_ACTIONS,
                            ButtonUM.TEST_PUSHES,
                        ),
                        onButtonClick = {
                            val route = when (it) {
                                ButtonUM.FEATURE_TOGGLES -> TesterScreen.FEATURE_TOGGLES
                                ButtonUM.EXCLUDED_BLOCKCHAINS -> TesterScreen.EXCLUDED_BLOCKCHAINS
                                ButtonUM.ENVIRONMENT_TOGGLES -> TesterScreen.ENVIRONMENTS_TOGGLES
                                ButtonUM.BLOCKCHAIN_PROVIDERS -> TesterScreen.BLOCKCHAIN_PROVIDERS
                                ButtonUM.TESTER_ACTIONS -> TesterScreen.TESTER_ACTIONS
                                ButtonUM.TEST_PUSHES -> TesterScreen.TEST_PUSHES
                            }

                            innerTesterRouter.open(route)
                        },
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

            composable(route = TesterScreen.BLOCKCHAIN_PROVIDERS.name) {
                val viewModel = hiltViewModel<BlockchainProvidersViewModel>().apply {
                    setupNavigation(innerTesterRouter, appFinisher)
                }

                val state by viewModel.state.collectAsStateWithLifecycle()
                BlockchainProvidersScreen(state)
            }

            composable(route = TesterScreen.TEST_PUSHES.name) {
                val viewModel = hiltViewModel<TestPushViewModel>().apply {
                    setupNavigation(innerTesterRouter)
                }
                val state by viewModel.uiState.collectAsStateWithLifecycle()

                TestPushScreen(state, viewModel)
            }
        }
    }
}