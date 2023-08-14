package com.tangem.feature.wallet.presentation.router

import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.tangem.core.navigation.AppScreen
import com.tangem.core.navigation.NavigationAction
import com.tangem.core.navigation.NavigationStateHolder
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.feature.wallet.presentation.WalletFragment
import com.tangem.feature.wallet.presentation.organizetokens.OrganizeTokensScreen
import com.tangem.feature.wallet.presentation.organizetokens.OrganizeTokensViewModel
import com.tangem.feature.wallet.presentation.wallet.ui.WalletScreen
import com.tangem.feature.wallet.presentation.wallet.viewmodels.WalletViewModel
import kotlin.properties.Delegates

/** Default implementation of wallet feature router */
internal class DefaultWalletRouter(private val navigationStateHolder: NavigationStateHolder) : InnerWalletRouter {

    private var navController: NavHostController by Delegates.notNull()
    private var fragmentManager: FragmentManager by Delegates.notNull()

    override fun getEntryFragment(): Fragment = WalletFragment.create()

    @Composable
    override fun Initialize(fragmentManager: FragmentManager) {
        this.fragmentManager = fragmentManager

        NavHost(
            navController = rememberNavController().apply { navController = this },
            startDestination = WalletRoute.Wallet.route,
        ) {
            composable(WalletRoute.Wallet.route) {
                val viewModel = hiltViewModel<WalletViewModel>().apply { router = this@DefaultWalletRouter }
                LocalLifecycleOwner.current.lifecycle.addObserver(observer = viewModel)

                WalletScreen(state = viewModel.uiState)
            }

            composable(
                WalletRoute.OrganizeTokens.route,
                arguments = listOf(navArgument(WalletRoute.userWalletIdKey) { type = NavType.StringType }),
            ) {
                val viewModel: OrganizeTokensViewModel = hiltViewModel<OrganizeTokensViewModel>()
                    .apply {
                        router = this@DefaultWalletRouter
                    }

                OrganizeTokensScreen(
                    modifier = Modifier.systemBarsPadding(),
                    state = viewModel.uiState,
                )
            }
        }
    }

    override fun popBackStack() {
        /*
         * It's hack that avoid issue with closing the wallet screen.
         * We are using NavGraph only inside feature so first backstack's element is entry of NavGraph and
         * next element is wallet screen entry.
         * If backstack contains only NavGraph entry and wallet screen entry then we close the wallet fragment.
         */
        if (navController.backQueue.size == BACKSTACK_ENTRY_COUNT_TO_CLOSE_WALLET_SCREEN) {
            fragmentManager.popBackStack()
        } else {
            navController.popBackStack()
        }
    }

    override fun openOrganizeTokensScreen(userWalletId: UserWalletId) {
        navController.navigate(WalletRoute.OrganizeTokens.createRoute(userWalletId))
    }

    override fun openDetailsScreen() {
        navigationStateHolder.navigate(action = NavigationAction.NavigateTo(AppScreen.Details))
    }

    override fun openOnboardingScreen() {
        navigationStateHolder.navigate(action = NavigationAction.NavigateTo(AppScreen.OnboardingWallet))
    }

    override fun openTxHistoryWebsite(url: String) {
        navigationStateHolder.navigate(action = NavigationAction.OpenUrl(url))
    }

    private companion object {
        const val BACKSTACK_ENTRY_COUNT_TO_CLOSE_WALLET_SCREEN = 2
    }
}