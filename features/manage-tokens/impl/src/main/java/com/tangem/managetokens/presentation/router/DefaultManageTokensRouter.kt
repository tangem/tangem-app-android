package com.tangem.managetokens.presentation.router

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.fragment.app.Fragment
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModelStoreOwner
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navigation
import com.tangem.core.navigation.AppScreen
import com.tangem.core.navigation.NavigationAction
import com.tangem.core.navigation.ReduxNavController
import com.tangem.managetokens.ManageTokensFragment
import com.tangem.managetokens.presentation.customtokens.viewmodels.CustomTokensViewModel
import com.tangem.managetokens.presentation.managetokens.ui.ManageTokensScreen
import com.tangem.managetokens.presentation.managetokens.viewmodels.ManageTokensViewModel
import kotlin.properties.Delegates

internal class DefaultManageTokensRouter(
    private val reduxNavController: ReduxNavController,
) : InnerManageTokensRouter {

    private var navController: NavHostController by Delegates.notNull()

    override fun getEntryFragment(): Fragment = ManageTokensFragment()

    @Composable
    override fun Initialize(viewModelStoreOwner: ViewModelStoreOwner) {
        NavHost(
            navController = rememberNavController().apply { navController = this },
            startDestination = ManageTokensRoute.ManageTokens.route,
        ) {
            composable(ManageTokensRoute.ManageTokens.route) {
                val viewModel = hiltViewModel<ManageTokensViewModel>().apply { router = this@DefaultManageTokensRouter }
                LocalLifecycleOwner.current.lifecycle.addObserver(viewModel)

                ManageTokensScreen(state = viewModel.uiState)
            }

            navigation(
                startDestination = ManageTokensRoute.CustomTokens.Main.route,
                route = ManageTokensRoute.CustomTokens.route,
            ) {
                composable(
                    ManageTokensRoute.CustomTokens.Main.route,
                ) {
                    val viewModel = hiltViewModel<CustomTokensViewModel>(viewModelStoreOwner).apply {
                        router = this@DefaultManageTokensRouter
                    }
                    // CustomTokensScreen(state = viewModel.uiState)
                    // TODO: enable in [REDACTED_JIRA]
                }
                composable(
                    ManageTokensRoute.CustomTokens.ChooseNetwork.route,
                ) {
                    val viewModel = hiltViewModel<CustomTokensViewModel>(viewModelStoreOwner).apply {
                        router = this@DefaultManageTokensRouter
                    }
                    // ChooseNetworkCustomScreen(
                    //     state = viewModel.uiState.chooseNetworkState,
                    // )
                    // TODO: enable in [REDACTED_JIRA]
                }
                composable(
                    ManageTokensRoute.CustomTokens.ChooseDerivation.route,
                ) {
                    val viewModel = hiltViewModel<CustomTokensViewModel>(viewModelStoreOwner).apply {
                        router = this@DefaultManageTokensRouter
                    }
                    // ChooseDerivationScreen(
                    //     state = requireNotNull(viewModel.uiState.chooseDerivationState),
                    // )
                    // TODO: enable in [REDACTED_JIRA]
                }
                composable(
                    ManageTokensRoute.CustomTokens.ChooseWallet.route,
                ) {
                    val viewModel = hiltViewModel<CustomTokensViewModel>(viewModelStoreOwner).apply {
                        router = this@DefaultManageTokensRouter
                    }
                    // CustomTokensChooseWalletScreen(
                    //     state = viewModel.uiState.chooseWalletState as ChooseWalletState.Choose,
                    // )
                    // TODO: enable in [REDACTED_JIRA]
                }
            }
        }
    }

    override fun popBackStack(screen: AppScreen?) {
        if (screen != null) {
            reduxNavController.navigate(action = NavigationAction.PopBackTo(screen))
        } else {
            navController.popBackStack()
        }
    }

    override fun openManageTokensScreen() {
        navController.navigate(ManageTokensRoute.ManageTokens.route)
    }

    override fun openCustomTokensScreen() {
        navController.navigate(ManageTokensRoute.CustomTokens.route)
    }

    override fun openCustomTokensChooseNetwork() {
        navController.navigate(ManageTokensRoute.CustomTokens.ChooseNetwork.route)
    }

    override fun openCustomTokensChooseDerivation() {
        navController.navigate(ManageTokensRoute.CustomTokens.ChooseDerivation.route)
    }

    override fun openCustomTokensChooseWallet() {
        navController.navigate(ManageTokensRoute.CustomTokens.ChooseWallet.route)
    }
}