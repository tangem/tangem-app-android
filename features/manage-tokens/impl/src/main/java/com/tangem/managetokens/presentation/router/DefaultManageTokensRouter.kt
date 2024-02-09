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
import com.tangem.managetokens.presentation.common.state.ChooseWalletState
import com.tangem.managetokens.presentation.addcustomtoken.ui.ChooseDerivationScreen
import com.tangem.managetokens.presentation.addcustomtoken.ui.ChooseNetworkCustomScreen
import com.tangem.managetokens.presentation.addcustomtoken.ui.CustomTokensChooseWalletScreen
import com.tangem.managetokens.presentation.addcustomtoken.ui.AddCustomTokenScreen
import com.tangem.managetokens.presentation.addcustomtoken.viewmodels.AddCustomTokenViewModel
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
                startDestination = ManageTokensRoute.AddCustomToken.Main.route,
                route = ManageTokensRoute.AddCustomToken.route,
            ) {
                composable(
                    ManageTokensRoute.AddCustomToken.Main.route,
                ) {
                    val viewModel = hiltViewModel<AddCustomTokenViewModel>(viewModelStoreOwner).apply {
                        router = this@DefaultManageTokensRouter
                    }
                    LocalLifecycleOwner.current.lifecycle.addObserver(viewModel)
                    AddCustomTokenScreen(state = viewModel.uiState)
                }
                composable(
                    ManageTokensRoute.AddCustomToken.ChooseNetwork.route,
                ) {
                    val viewModel = hiltViewModel<AddCustomTokenViewModel>(viewModelStoreOwner).apply {
                        router = this@DefaultManageTokensRouter
                    }
                    ChooseNetworkCustomScreen(
                        state = viewModel.uiState.chooseNetworkState,
                    )
                }
                composable(
                    ManageTokensRoute.AddCustomToken.ChooseDerivation.route,
                ) {
                    val viewModel = hiltViewModel<AddCustomTokenViewModel>(viewModelStoreOwner).apply {
                        router = this@DefaultManageTokensRouter
                    }
                    ChooseDerivationScreen(
                        state = requireNotNull(viewModel.uiState.chooseDerivationState),
                    )
                }
                composable(
                    ManageTokensRoute.AddCustomToken.ChooseWallet.route,
                ) {
                    val viewModel = hiltViewModel<AddCustomTokenViewModel>(viewModelStoreOwner).apply {
                        router = this@DefaultManageTokensRouter
                    }
                    CustomTokensChooseWalletScreen(
                        state = viewModel.uiState.chooseWalletState as ChooseWalletState.Choose,
                    )
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

    override fun openAddCustomTokenScreen() {
        navController.navigate(ManageTokensRoute.AddCustomToken.route)
    }

    override fun openCustomTokenChooseNetwork() {
        navController.navigate(ManageTokensRoute.AddCustomToken.ChooseNetwork.route)
    }

    override fun openCustomTokenChooseDerivation() {
        navController.navigate(ManageTokensRoute.AddCustomToken.ChooseDerivation.route)
    }

    override fun openCustomTokenChooseWallet() {
        navController.navigate(ManageTokensRoute.AddCustomToken.ChooseWallet.route)
    }
}