package com.tangem.tap.features.wallet.redux.middlewares

import com.tangem.common.doOnSuccess
import com.tangem.common.extensions.guard
import com.tangem.common.flatMap
import com.tangem.core.analytics.Analytics
import com.tangem.tap.common.analytics.events.AnalyticsParam
import com.tangem.tap.common.analytics.events.Token.ButtonRemoveToken
import com.tangem.tap.common.extensions.addContext
import com.tangem.tap.common.extensions.dispatchDialogShow
import com.tangem.tap.common.extensions.dispatchErrorNotification
import com.tangem.tap.common.extensions.dispatchOnMain
import com.tangem.tap.common.redux.global.GlobalAction
import com.tangem.tap.common.redux.navigation.AppScreen
import com.tangem.tap.common.redux.navigation.NavigationAction
import com.tangem.tap.domain.TapError
import com.tangem.tap.domain.model.UserWallet
import com.tangem.tap.features.wallet.redux.WalletAction
import com.tangem.tap.features.wallet.redux.WalletState
import com.tangem.tap.features.wallet.redux.models.WalletDialog
import com.tangem.tap.scope
import com.tangem.tap.store
import com.tangem.tap.tangemSdkManager
import com.tangem.tap.userTokensRepository
import com.tangem.tap.userWalletsListManager
import com.tangem.tap.walletCurrenciesManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber

class MultiWalletMiddleware {
    @Suppress("LongMethod", "ComplexMethod")
    fun handle(action: WalletAction.MultiWallet, walletState: WalletState?) {
        when (action) {
            is WalletAction.MultiWallet.SelectWallet -> {
                if (action.currency != null) {
                    store.dispatch(NavigationAction.NavigateTo(AppScreen.WalletDetails))
                }
            }
            is WalletAction.MultiWallet.TryToRemoveWallet -> {
                val currency = action.currency
                val walletManager = walletState?.getWalletManager(currency).guard {
                    store.dispatchErrorNotification(TapError.UnsupportedState("walletManager is NULL"))
                    store.dispatch(NavigationAction.PopBackTo(AppScreen.Home))
                    return
                }

                if (currency.isBlockchain() && walletManager.cardTokens.isNotEmpty()) {
                    store.dispatchDialogShow(
                        WalletDialog.TokensAreLinkedDialog(
                            currencyTitle = currency.currencyName,
                            currencySymbol = currency.currencySymbol,
                        ),
                    )
                } else {
                    store.dispatchDialogShow(
                        WalletDialog.RemoveWalletDialog(
                            currencyTitle = currency.currencyName,
                            onOk = {
                                Analytics.send(ButtonRemoveToken(AnalyticsParam.CurrencyType.Currency(currency)))
                                store.dispatch(WalletAction.MultiWallet.RemoveWallet(currency))
                                store.dispatch(NavigationAction.PopBackTo())
                            },
                        ),
                    )
                }
            }
            is WalletAction.MultiWallet.RemoveWallet -> {
                val selectedUserWallet = userWalletsListManager.selectedUserWalletSync.guard {
                    Timber.e("Unable to remove wallet, no user wallet selected")
                    return
                }
                scope.launch {
                    walletCurrenciesManager.removeCurrency(
                        userWallet = selectedUserWallet,
                        currencyToRemove = action.currency,
                    )
                }
            }
            is WalletAction.MultiWallet.BackupWallet -> {
                val selectedUserWallet = userWalletsListManager.selectedUserWalletSync.guard {
                    Timber.e("Unable to backup wallet, no user wallet selected")
                    return
                }
                val scanResponse = selectedUserWallet.scanResponse
                Analytics.addContext(scanResponse)
                store.dispatch(GlobalAction.Onboarding.Start(scanResponse, canSkipBackup = false))
                store.dispatch(NavigationAction.NavigateTo(AppScreen.OnboardingWallet))
            }
            is WalletAction.MultiWallet.AddMissingDerivations -> {
                store.state.globalState.topUpController?.addMissingDerivations(action.blockchains)
            }
            is WalletAction.MultiWallet.ScanToGetDerivations -> {
                val selectedUserWallet = userWalletsListManager.selectedUserWalletSync.guard {
                    Timber.e("Unable to scan to get derivations, no user wallet selected")
                    return
                }
                store.state.globalState.topUpController?.scanToGetDerivations()
                scanAndUpdateCard(selectedUserWallet, walletState)
            }
            else -> {}
        }
    }

    private fun scanAndUpdateCard(selectedUserWallet: UserWallet, state: WalletState?) =
        scope.launch(Dispatchers.Default) {
            tangemSdkManager.scanProduct(
                cardId = selectedUserWallet.cardId,
                userTokensRepository = userTokensRepository,
                additionalBlockchainsToDerive = state?.missingDerivations?.map { it.blockchain },
                allowsRequestAccessCodeFromRepository = true,
            )
                .flatMap { scanResponse ->
                    userWalletsListManager.update(
                        userWalletId = selectedUserWallet.walletId,
                        update = { userWallet ->
                            userWallet.copy(
                                scanResponse = scanResponse,
                            )
                        },
                    )
                }
                .doOnSuccess { updatedUserWallet ->
                    store.dispatchOnMain(WalletAction.MultiWallet.AddMissingDerivations(emptyList()))
                    store.state.globalState.tapWalletManager.loadData(updatedUserWallet, refresh = true)
                }
        }
}
