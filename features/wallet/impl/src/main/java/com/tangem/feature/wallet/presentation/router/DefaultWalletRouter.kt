package com.tangem.feature.wallet.presentation.router

import com.arkivanov.decompose.router.slot.SlotNavigation
import com.tangem.common.routing.AppRoute
import com.tangem.common.routing.AppRoute.ManageTokens.Source
import com.tangem.common.routing.AppRouter
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.navigation.url.UrlOpener
import com.tangem.domain.models.scan.ScanResponse
import com.tangem.domain.redux.ReduxStateHolder
import com.tangem.domain.redux.StateDialog
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.feature.wallet.navigation.WalletRoute
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletDialogConfig
import com.tangem.features.biometry.BiometryFeatureToggles
import com.tangem.features.onboarding.v2.OnboardingV2FeatureToggles
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import javax.inject.Inject

/** Default implementation of wallet feature router */
@ModelScoped
internal class DefaultWalletRouter @Inject constructor(
    private val router: AppRouter,
    private val urlOpener: UrlOpener,
    private val reduxStateHolder: ReduxStateHolder,
    private val onboardingV2FeatureToggles: OnboardingV2FeatureToggles,
    private val biometryFeatureToggles: BiometryFeatureToggles,
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
        if (onboardingV2FeatureToggles.isOnboardingV2Enabled) {
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
        if (biometryFeatureToggles.isAskForBiometryEnabled.not()) {
            router.push(AppRoute.SaveWallet)
        }
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

    override fun openNFT(userWalletId: UserWalletId) {
        router.push(AppRoute.NFT(userWalletId))
    }
}