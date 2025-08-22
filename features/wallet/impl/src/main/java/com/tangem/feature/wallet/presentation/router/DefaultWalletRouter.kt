package com.tangem.feature.wallet.presentation.router

import com.arkivanov.decompose.router.slot.SlotNavigation
import com.arkivanov.decompose.router.slot.activate
import com.tangem.common.routing.AppRoute
import com.tangem.common.routing.AppRoute.ManageTokens.Source
import com.tangem.common.routing.AppRouter
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.navigation.url.UrlOpener
import com.tangem.domain.models.TokenReceiveConfig
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.scan.ScanResponse
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.redux.ReduxStateHolder
import com.tangem.domain.redux.StateDialog
import com.tangem.feature.wallet.navigation.WalletRoute
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletDialogConfig
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import javax.inject.Inject

/** Default implementation of wallet feature router */
@ModelScoped
internal class DefaultWalletRouter @Inject constructor(
    private val router: AppRouter,
    private val urlOpener: UrlOpener,
    private val reduxStateHolder: ReduxStateHolder,
) : InnerWalletRouter {

    override val dialogNavigation: SlotNavigation<WalletDialogConfig> = SlotNavigation()

    override val navigateToFlow = MutableSharedFlow<WalletRoute>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_LATEST,
    )

    override fun openOrganizeTokensScreen(userWalletId: UserWalletId) {
        navigateToFlow.tryEmit(WalletRoute.OrganizeTokens(userWalletId))
    }

    override fun openDetailsScreen(selectedWalletId: UserWalletId) {
        router.push(
            AppRoute.Details(
                userWalletId = selectedWalletId,
            ),
        )
    }

    override fun openOnboardingScreen(scanResponse: ScanResponse, continueBackup: Boolean) {
        router.push(
            AppRoute.Onboarding(
                scanResponse = scanResponse,
                mode = if (continueBackup) {
                    AppRoute.Onboarding.Mode.AddBackupWallet1
                } else {
                    AppRoute.Onboarding.Mode.Onboarding
                },
            ),
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
        router.push(AppRoute.Home())
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

    override fun openNFT(userWallet: UserWallet) {
        router.push(
            AppRoute.NFT(
                userWalletId = userWallet.walletId,
                walletName = userWallet.name,
            ),
        )
    }

    override fun openTokenReceiveBottomSheet(tokenReceiveConfig: TokenReceiveConfig) {
        dialogNavigation.activate(
            configuration = WalletDialogConfig.TokenReceive(tokenReceiveConfig),
        )
    }
}