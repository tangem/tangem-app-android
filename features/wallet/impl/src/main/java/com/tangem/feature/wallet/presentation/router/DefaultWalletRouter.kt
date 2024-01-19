package com.tangem.feature.wallet.presentation.router

import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.tangem.core.navigation.AppScreen
import com.tangem.core.navigation.NavigationAction
import com.tangem.core.navigation.ReduxNavController
import com.tangem.core.navigation.StateDialog
import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.feature.onboarding.navigation.OnboardingRouter
import com.tangem.feature.wallet.presentation.WalletFragment
import com.tangem.feature.wallet.presentation.organizetokens.OrganizeTokensScreen
import com.tangem.feature.wallet.presentation.organizetokens.OrganizeTokensViewModel
import com.tangem.feature.wallet.presentation.wallet.ui.WalletScreen
import com.tangem.feature.wallet.presentation.wallet.ui.WalletScreenV2
import com.tangem.feature.wallet.presentation.wallet.viewmodels.WalletViewModel
import com.tangem.feature.wallet.presentation.wallet.viewmodels.WalletViewModelV2
import com.tangem.features.tokendetails.navigation.TokenDetailsRouter
import com.tangem.features.wallet.featuretoggles.WalletFeatureToggles
import kotlin.properties.Delegates

/** Default implementation of wallet feature router */
internal class DefaultWalletRouter(
    private val reduxNavController: ReduxNavController,
    private val walletFeatureToggles: WalletFeatureToggles,
) : InnerWalletRouter {

    private var navController: NavHostController by Delegates.notNull()
    private var onFinish: () -> Unit = {}

    override fun getEntryFragment(): Fragment = WalletFragment.create()

    @Composable
    override fun Initialize(onFinish: () -> Unit) {
        this.onFinish = onFinish

        NavHost(
            navController = rememberNavController().apply { navController = this },
            startDestination = WalletRoute.Wallet.route,
        ) {
            composable(WalletRoute.Wallet.route) {
                if (walletFeatureToggles.isWalletsScrollingPreviewEnabled) {
                    val viewModel = hiltViewModel<WalletViewModelV2>().apply {
                        setWalletRouter(router = this@DefaultWalletRouter)
                        subscribeToLifecycle(LocalLifecycleOwner.current)
                    }

                    WalletScreenV2(state = viewModel.uiState.collectAsStateWithLifecycle().value)
                } else {
                    val viewModel = hiltViewModel<WalletViewModel>().apply {
                        router = this@DefaultWalletRouter
                    }
                    LocalLifecycleOwner.current.lifecycle.addObserver(viewModel)

                    WalletScreen(state = viewModel.uiState)
                }
            }

            composable(
                WalletRoute.OrganizeTokens.route,
                arguments = listOf(navArgument(WalletRoute.userWalletIdKey) { type = NavType.StringType }),
            ) {
                val viewModel = hiltViewModel<OrganizeTokensViewModel>().apply {
                    router = this@DefaultWalletRouter
                }
                LocalLifecycleOwner.current.lifecycle.addObserver(viewModel)

                val uiState by viewModel.uiState.collectAsStateWithLifecycle()

                OrganizeTokensScreen(
                    modifier = Modifier.statusBarsPadding(),
                    state = uiState,
                )
            }
        }
    }

    override fun popBackStack(screen: AppScreen?) {
        /*
         * It's hack that avoid issue with closing the wallet screen.
         * We are using NavGraph only inside feature so first backstack's element is entry of NavGraph and
         * next element is wallet screen entry.
         * If backstack contains only NavGraph entry and wallet screen entry then we close the wallet fragment.
         */
        if (navController.currentBackStack.value.size == BACKSTACK_ENTRY_COUNT_TO_CLOSE_WALLET_SCREEN) {
            if (screen != null) {
                reduxNavController.navigate(action = NavigationAction.PopBackTo(screen))
            } else {
                onFinish.invoke()
            }
        } else {
            navController.popBackStack()
        }
    }

    override fun openOrganizeTokensScreen(userWalletId: UserWalletId) {
        navController.navigate(WalletRoute.OrganizeTokens.createRoute(userWalletId))
    }

    override fun openDetailsScreen() {
        reduxNavController.navigate(action = NavigationAction.NavigateTo(AppScreen.Details))
    }

    override fun openOnboardingScreen() {
        reduxNavController.navigate(
            action = NavigationAction.NavigateTo(
                screen = AppScreen.OnboardingWallet,
                bundle = bundleOf(OnboardingRouter.CAN_SKIP_BACKUP to false),
            ),
        )
    }

    override fun openUrl(url: String) {
        reduxNavController.navigate(action = NavigationAction.OpenUrl(url))
    }

    override fun openTokenDetails(userWalletId: UserWalletId, currency: CryptoCurrency) {
        reduxNavController.navigate(
            action = NavigationAction.NavigateTo(
                screen = AppScreen.WalletDetails,
                bundle = bundleOf(
                    TokenDetailsRouter.USER_WALLET_ID_KEY to userWalletId.stringValue,
                    TokenDetailsRouter.CRYPTO_CURRENCY_KEY to currency,
                ),
            ),
        )
    }

    override fun openStoriesScreen() {
        reduxNavController.navigate(action = NavigationAction.NavigateTo(screen = AppScreen.Home))
    }

    override fun openSaveUserWalletScreen() {
        reduxNavController.navigate(action = NavigationAction.NavigateTo(AppScreen.SaveWallet))
    }

    override fun isWalletLastScreen(): Boolean = reduxNavController.getBackStack().lastOrNull() == AppScreen.Wallet

    override fun openManageTokensScreen() {
        reduxNavController.navigate(action = NavigationAction.NavigateTo(AppScreen.ManageTokens))
    }

    override fun openScanFailedDialog() {
        reduxNavController.navigate(action = NavigationAction.OpenDialog(StateDialog.ScanFailsDialog))
    }

    private companion object {
        const val BACKSTACK_ENTRY_COUNT_TO_CLOSE_WALLET_SCREEN = 2
    }
}