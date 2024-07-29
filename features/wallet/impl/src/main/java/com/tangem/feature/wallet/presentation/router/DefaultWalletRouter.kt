package com.tangem.feature.wallet.presentation.router

import android.annotation.SuppressLint
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.fragment.app.Fragment
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.tangem.common.routing.AppRoute
import com.tangem.common.routing.AppRouter
import com.tangem.core.navigation.url.UrlOpener
import com.tangem.domain.redux.ReduxStateHolder
import com.tangem.domain.redux.StateDialog
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.feature.wallet.presentation.WalletFragment
import com.tangem.feature.wallet.presentation.organizetokens.OrganizeTokensScreen
import com.tangem.feature.wallet.presentation.organizetokens.OrganizeTokensViewModel
import com.tangem.feature.wallet.presentation.wallet.ui.WalletScreen
import com.tangem.feature.wallet.presentation.wallet.viewmodels.WalletViewModel
import com.tangem.features.markets.component.MarketsEntryComponent
import kotlin.properties.Delegates

/** Default implementation of wallet feature router */
internal class DefaultWalletRouter(
    private val router: AppRouter,
    private val urlOpener: UrlOpener,
    private val reduxStateHolder: ReduxStateHolder,
) : InnerWalletRouter {

    private var navController: NavHostController by Delegates.notNull()
    private var onFinish: () -> Unit = {}

    override fun getEntryFragment(): Fragment = WalletFragment.create()

    @Composable
    override fun Initialize(onFinish: () -> Unit, marketsEntryComponent: MarketsEntryComponent?) {
        this.onFinish = onFinish

        NavHost(
            navController = rememberNavController().apply { navController = this },
            startDestination = WalletRoute.Wallet.route,
        ) {
            composable(WalletRoute.Wallet.route) {
                val viewModel = hiltViewModel<WalletViewModel>().apply {
                    setWalletRouter(router = this@DefaultWalletRouter)
                    subscribeToLifecycle(LocalLifecycleOwner.current)
                }

                WalletScreen(
                    state = viewModel.uiState.collectAsStateWithLifecycle().value,
                    marketsEntryComponent = marketsEntryComponent,
                )
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
                    state = uiState,
                )
            }
        }
    }

    @SuppressLint("RestrictedApi")
    override fun popBackStack() {
        /*
         * It's hack that avoid issue with closing the wallet screen.
         * We are using NavGraph only inside feature so first backstack's element is entry of NavGraph and
         * next element is wallet screen entry.
         * If backstack contains only NavGraph entry and wallet screen entry then we close the wallet fragment.
         */
        if (navController.currentBackStack.value.size == BACKSTACK_ENTRY_COUNT_TO_CLOSE_WALLET_SCREEN) {
            onFinish.invoke()
        } else {
            navController.popBackStack()
        }
    }

    override fun openOrganizeTokensScreen(userWalletId: UserWalletId) {
        navController.navigate(WalletRoute.OrganizeTokens.createRoute(userWalletId))
    }

    override fun openDetailsScreen(selectedWalletId: UserWalletId) {
        router.push(
            AppRoute.Details(
                userWalletId = selectedWalletId,
            ),
        )
    }

    override fun openOnboardingScreen() {
        router.push(
            AppRoute.OnboardingWallet(canSkipBackup = false),
        )
    }

    override fun openUrl(url: String) {
        urlOpener.openUrl(url)
    }

    override fun openTokenDetails(userWalletId: UserWalletId, currencyStatus: CryptoCurrencyStatus) {
        val networkAddress = currencyStatus.value.networkAddress
        if (networkAddress != null && networkAddress.defaultAddress.value.isNotEmpty()) {
            router.push(
                AppRoute.CurrencyDetails(
                    userWalletId = userWalletId,
                    currency = currencyStatus.currency,
                ),
            )
        }
    }

    override fun openStoriesScreen() {
        router.push(AppRoute.Home)
    }

    override fun openSaveUserWalletScreen() {
        router.push(AppRoute.SaveWallet)
    }

    override fun isWalletLastScreen(): Boolean {
        return router.stack.lastOrNull() is AppRoute.Wallet
    }

    override fun openManageTokensScreen() {
        router.push(AppRoute.ManageTokens)
    }

    override fun openScanFailedDialog(onTryAgain: () -> Unit) {
        reduxStateHolder.dispatchDialogShow(StateDialog.ScanFailsDialog(StateDialog.ScanFailsSource.MAIN, onTryAgain))
    }

    private companion object {
        const val BACKSTACK_ENTRY_COUNT_TO_CLOSE_WALLET_SCREEN = 2
    }
}