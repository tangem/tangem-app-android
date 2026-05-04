package com.tangem.feature.wallet.presentation.router

import androidx.compose.ui.geometry.Offset
import com.arkivanov.decompose.router.slot.SlotNavigation
import com.arkivanov.decompose.router.slot.activate
import com.arkivanov.decompose.router.slot.dismiss
import com.tangem.common.routing.AppRoute
import com.tangem.common.routing.AppRouter
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.navigation.url.UrlOpener
import com.tangem.core.ui.DesignFeatureToggles
import com.tangem.core.ui.ds.row.token.TangemTokenRowUM
import com.tangem.domain.models.TokenReceiveConfig
import com.tangem.domain.models.account.AccountId
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.scan.ScanResponse
import com.tangem.domain.qrscanning.models.QrSendTarget
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.TangemPayDetailsConfig
import com.tangem.domain.tokens.model.details.NavigationAction
import com.tangem.domain.tokens.model.details.TokenAction
import com.tangem.feature.wallet.child.organizetokens.OrganizeTokensComponent
import com.tangem.feature.wallet.navigation.WalletRoute
import com.tangem.feature.wallet.presentation.wallet.state.model.TokenActionButtonUM
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletDialogConfig
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import javax.inject.Inject

/** Default implementation of wallet feature router */
@ModelScoped
internal class DefaultWalletRouter @Inject constructor(
    private val router: AppRouter,
    private val urlOpener: UrlOpener,
    private val designFeatureToggles: DesignFeatureToggles,
) : InnerWalletRouter {

    override val dialogNavigation: SlotNavigation<WalletDialogConfig> = SlotNavigation()

    override val navigateToFlow = MutableSharedFlow<WalletRoute>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_LATEST,
    )

    override val organizeCallbacks: OrganizeTokensComponent.Callback
        get() = OrganizeCallbacks()

    override fun openOrganizeTokensScreen(userWalletId: UserWalletId) {
        if (designFeatureToggles.isRedesignEnabled) {
            dialogNavigation.activate(
                configuration = WalletDialogConfig.OrganizeTokens(userWalletId),
            )
        } else {
            navigateToFlow.tryEmit(WalletRoute.OrganizeTokens(userWalletId))
        }
    }

    override fun openDetailsScreen(selectedWalletId: UserWalletId) {
        router.push(
            AppRoute.Details(
                userWalletId = selectedWalletId,
            ),
        )
    }

    override fun openManageTokensScreen(accountId: AccountId) {
        val route = AppRoute.ManageTokens(
            source = AppRoute.ManageTokens.Source.ACCOUNT,
            accountId = accountId,
        )
        router.push(route)
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

    override fun openTokenDetails(
        userWalletId: UserWalletId,
        currencyStatus: CryptoCurrencyStatus,
        navigationAction: NavigationAction?,
    ) {
        val networkAddress = currencyStatus.value.networkAddress
        if (networkAddress != null && networkAddress.defaultAddress.value.isNotEmpty()) {
            router.push(
                AppRoute.CurrencyDetails(
                    userWalletId = userWalletId,
                    currency = currencyStatus.currency,
                    navigationAction = navigationAction,
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

    override fun openTangemPayOnboarding(mode: AppRoute.TangemPayOnboarding.Mode) {
        router.push(route = AppRoute.TangemPayOnboarding(mode = mode))
    }

    override fun openTangemPayDetails(userWalletId: UserWalletId, config: TangemPayDetailsConfig) {
        router.push(AppRoute.TangemPayDetails(userWalletId = userWalletId, config = config))
    }

    override fun openYieldSupplyBottomSheet(
        cryptoCurrency: CryptoCurrency,
        tokenAction: TokenAction,
        onWarningAcknowledged: (TokenAction) -> Unit,
    ) {
        dialogNavigation.activate(
            configuration = WalletDialogConfig.YieldSupplyWarning(
                cryptoCurrency = cryptoCurrency,
                tokenAction = tokenAction,
                onWarningAcknowledged = onWarningAcknowledged,
            ),
        )
    }

    override fun openYieldSupplyEntryScreen(userWalletId: UserWalletId, cryptoCurrency: CryptoCurrency, apy: String) {
        router.push(
            AppRoute.YieldSupplyEntry(
                userWalletId = userWalletId,
                cryptoCurrency = cryptoCurrency,
                apy = apy,
            ),
        )
    }

    override fun openTokenActionSheet(
        userWallet: UserWallet,
        tokenActionList: ImmutableList<TokenActionButtonUM>,
        offset: Offset,
        tokenRowUM: TangemTokenRowUM?,
    ) {
        dialogNavigation.activate(
            configuration = WalletDialogConfig.TokenActionList(
                actionList = tokenActionList,
                offsetX = offset.x,
                offsetY = offset.y,
                tokenRowUM = tokenRowUM,
            ),
        )
    }

    override fun openQrScanner() {
        router.push(AppRoute.QrScanning(source = AppRoute.QrScanning.Source.MainScreen))
    }

    override fun openSend(
        userWalletId: UserWalletId,
        currency: CryptoCurrency,
        address: String,
        amount: String?,
        tag: String?,
        entryType: AppRoute.Send.EntryType,
    ) {
        router.push(
            AppRoute.Send(
                userWalletId = userWalletId,
                currency = currency,
                destinationAddress = address,
                amount = amount,
                tag = tag,
                entryType = entryType,
            ),
        )
    }

    override fun openNetworkSelectionBottomSheet(target: QrSendTarget.Multiple) {
        dialogNavigation.activate(
            configuration = WalletDialogConfig.NetworkSelection(
                address = target.address,
                amount = target.amount,
                memo = target.memo,
                walletGroups = target.walletGroups.map { walletGroup ->
                    WalletDialogConfig.NetworkSelection.WalletGroupData(
                        userWalletId = walletGroup.userWalletId,
                        walletName = walletGroup.walletName,
                        accounts = walletGroup.accounts.map { accountGroup ->
                            WalletDialogConfig.NetworkSelection.AccountGroupData(
                                accountId = accountGroup.accountId,
                                accountName = accountGroup.accountName,
                                currencies = accountGroup.currencies,
                                hiddenTokensCount = accountGroup.hiddenTokensCount,
                            )
                        },
                    )
                },
            ),
        )
    }

    inner class OrganizeCallbacks : OrganizeTokensComponent.Callback {
        override fun onDismiss() {
            dialogNavigation.dismiss()
        }
    }
}