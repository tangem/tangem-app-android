package com.tangem.feature.wallet.presentation.router

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.fragment.app.Fragment
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.tangem.core.ui.res.TangemTheme
import com.tangem.feature.wallet.presentation.WalletFragment
import com.tangem.feature.wallet.presentation.organizetokens.OrganizeTokensScreen
import com.tangem.feature.wallet.presentation.organizetokens.OrganizeTokensViewModel
import com.tangem.feature.wallet.presentation.wallet.ui.WalletScreen
import com.tangem.feature.wallet.presentation.wallet.viewmodels.WalletViewModel
import kotlin.properties.Delegates

/** Default implementation of wallet feature router */
internal class DefaultWalletRouter : InnerWalletRouter {

    private var navController: NavHostController by Delegates.notNull()

    override fun getEntryFragment(): Fragment = WalletFragment.create()

    @Composable
    override fun Initialize() {
        TangemTheme {
            NavHost(
                navController = rememberNavController().apply { navController = this },
                startDestination = WalletScreens.WALLET.name,
            ) {
                composable(WalletScreens.WALLET.name) {
                    val viewModel = hiltViewModel<WalletViewModel>().apply { router = this@DefaultWalletRouter }
                    WalletScreen(state = viewModel.uiState)
                }

                composable(WalletScreens.ORGANIZE_TOKENS.name) {
                    BackHandler(onBack = ::popBackStack)

                    val viewModel: OrganizeTokensViewModel = hiltViewModel<OrganizeTokensViewModel>()
                        .apply { router = this@DefaultWalletRouter }

                    OrganizeTokensScreen(
                        modifier = Modifier.systemBarsPadding(),
                        state = viewModel.uiState,
                    )
                }
            }
        }
    }

    override fun popBackStack() {
        navController.popBackStack()
    }

    override fun openOrganizeTokensScreen() {
        navController.navigate(WalletScreens.ORGANIZE_TOKENS.name)
    }
}