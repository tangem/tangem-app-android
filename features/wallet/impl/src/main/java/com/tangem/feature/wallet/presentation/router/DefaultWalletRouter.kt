package com.tangem.feature.wallet.presentation.router

import android.annotation.SuppressLint
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.fragment.app.Fragment
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.extensions.compose.jetpack.subscribeAsState
import com.arkivanov.decompose.router.slot.ChildSlot
import com.arkivanov.decompose.router.slot.SlotNavigation
import com.arkivanov.decompose.router.slot.childSlot
import com.arkivanov.decompose.router.slot.dismiss
import com.arkivanov.decompose.value.Value
import com.tangem.common.routing.AppRoute
import com.tangem.common.routing.AppRoute.ManageTokens.Source
import com.tangem.common.routing.AppRouter
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.context.childByContext
import com.tangem.core.navigation.url.UrlOpener
import com.tangem.core.ui.decompose.ComposableDialogComponent
import com.tangem.domain.models.scan.ScanResponse
import com.tangem.domain.redux.ReduxStateHolder
import com.tangem.domain.redux.StateDialog
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.feature.wallet.presentation.WalletFragment
import com.tangem.feature.wallet.presentation.organizetokens.OrganizeTokensScreen
import com.tangem.feature.wallet.presentation.organizetokens.OrganizeTokensViewModel
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletDialogConfig
import com.tangem.feature.wallet.presentation.wallet.ui.WalletScreen
import com.tangem.feature.wallet.presentation.wallet.viewmodels.WalletViewModel
import com.tangem.feature.walletsettings.component.RenameWalletComponent
import com.tangem.features.markets.entry.MarketsEntryComponent
import com.tangem.features.onboarding.v2.OnboardingV2FeatureToggles
import javax.inject.Inject
import kotlin.properties.Delegates

/** Default implementation of wallet feature router */
internal class DefaultWalletRouter @Inject constructor(
    private val router: AppRouter,
    private val urlOpener: UrlOpener,
    private val reduxStateHolder: ReduxStateHolder,
    private val onboardingV2FeatureToggles: OnboardingV2FeatureToggles,
    private val marketsEntryComponentFactory: MarketsEntryComponent.Factory,
    private val renameWalletComponentFactory: RenameWalletComponent.Factory,
) : InnerWalletRouter {

    private var navController: NavHostController by Delegates.notNull()
    private var onFinish: () -> Unit = {}

    private lateinit var marketsEntryComponent: MarketsEntryComponent
    private lateinit var dialog: Value<ChildSlot<WalletDialogConfig, ComposableDialogComponent>>

    override val dialogNavigation: SlotNavigation<WalletDialogConfig> = SlotNavigation()

    override fun initializeResources(appComponentContext: AppComponentContext) {
        marketsEntryComponent = marketsEntryComponentFactory.create(appComponentContext)
        dialog = appComponentContext.childSlot(
            source = dialogNavigation,
            serializer = WalletDialogConfig.serializer(),
            handleBackButton = true,
            childFactory = { dialogConfig, componentContext ->
                dialogChild(
                    appContext = appComponentContext,
                    dialogConfig = dialogConfig,
                    componentContext = componentContext,
                )
            },
        )
    }

    override fun getEntryFragment(): Fragment = WalletFragment.create()

    @Composable
    override fun Initialize(onFinish: () -> Unit) {
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

                val dialog by dialog.subscribeAsState()

                WalletScreen(
                    state = viewModel.uiState.collectAsStateWithLifecycle().value,
                    marketsEntryComponent = marketsEntryComponent,
                )

                dialog.child?.instance?.Dialog()
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

    override fun openOnboardingScreen(scanResponse: ScanResponse, continueBackup: Boolean) {
        if (onboardingV2FeatureToggles.isOnboardingV2Enabled) {
            router.push(
                AppRoute.Onboarding(
                    scanResponse = scanResponse,
                    startFromBackup = true,
                    mode = if (continueBackup) {
                        AppRoute.Onboarding.Mode.AddBackup
                    } else {
                        AppRoute.Onboarding.Mode.Onboarding
                    },
                ),
            )
        } else {
            router.push(
                AppRoute.OnboardingWallet(canSkipBackup = false),
            )
        }
    }

    override fun openOnrampSuccessScreen(externalTxId: String) {
        // finish current onramp flow and show onramp success screen
        val replaceOnrampScreens = router.stack
            .filterNot { it is AppRoute.Onramp }
            .toMutableList()

        replaceOnrampScreens.add(AppRoute.OnrampSuccess(externalTxId))

        router.replaceAll(*replaceOnrampScreens.toTypedArray())
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

    override fun openManageTokensScreen(userWalletId: UserWalletId) {
        router.push(AppRoute.ManageTokens(Source.SETTINGS, userWalletId))
    }

    override fun openScanFailedDialog(onTryAgain: () -> Unit) {
        reduxStateHolder.dispatchDialogShow(StateDialog.ScanFailsDialog(StateDialog.ScanFailsSource.MAIN, onTryAgain))
    }

    private fun dialogChild(
        appContext: AppComponentContext,
        dialogConfig: WalletDialogConfig,
        componentContext: ComponentContext,
    ): ComposableDialogComponent = when (dialogConfig) {
        is WalletDialogConfig.RenameWallet -> {
            renameWalletComponentFactory.create(
                context = appContext.childByContext(componentContext),
                params = RenameWalletComponent.Params(
                    userWalletId = dialogConfig.userWalletId,
                    currentName = dialogConfig.currentName,
                    onDismiss = dialogNavigation::dismiss,
                ),
            )
        }
    }

    private companion object {
        const val BACKSTACK_ENTRY_COUNT_TO_CLOSE_WALLET_SCREEN = 2
    }
}