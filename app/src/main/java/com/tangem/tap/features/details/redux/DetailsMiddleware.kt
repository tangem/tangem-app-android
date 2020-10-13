package com.tangem.tap.features.details.redux

import com.tangem.commands.common.network.Result
import com.tangem.common.CompletionResult
import com.tangem.tap.common.redux.AppState
import com.tangem.tap.common.redux.global.GlobalAction
import com.tangem.tap.common.redux.navigation.AppScreen
import com.tangem.tap.common.redux.navigation.NavigationAction
import com.tangem.tap.features.disclaimer.redux.DisclaimerAction
import com.tangem.tap.features.wallet.redux.WalletAction
import com.tangem.tap.network.coinmarketcap.CoinMarketCapService
import com.tangem.tap.preferencesStorage
import com.tangem.tap.scope
import com.tangem.tap.store
import com.tangem.tap.tangemSdkManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.rekotlin.Middleware

class DetailsMiddleware {
    private val eraseWalletMiddleware = EraseWalletMiddleware()
    private val appCurrencyMiddleware = AppCurrencyMiddleware()
    private val manageSecurityMiddleware = ManageSecurityMiddleware()
    val detailsMiddleware: Middleware<AppState> = { dispatch, state ->
        { next ->
            { action ->
                when (action) {
                    is DetailsAction.PrepareScreen -> prepareData()
                    is DetailsAction.EraseWallet -> eraseWalletMiddleware.handle(action)
                    is DetailsAction.AppCurrencyAction -> appCurrencyMiddleware.handle(action)
                    is DetailsAction.ManageSecurity -> manageSecurityMiddleware.handle(action)
                    is  DetailsAction.ShowDisclaimer -> {
                        store.dispatch(DisclaimerAction.ShowAcceptedDisclaimer)
                        store.dispatch(NavigationAction.NavigateTo(AppScreen.Disclaimer))
                    }
                }
                next(action)
            }
        }
    }

    private fun prepareData() {
        scope.launch {
            val loadedCurrencies = preferencesStorage.getFiatCurrencies()
            if (loadedCurrencies.isNullOrEmpty()) {
                val response = CoinMarketCapService().getFiatCurrencies()
                withContext(Dispatchers.Main) {
                    when (response) {
                        is Result.Success -> {
                            preferencesStorage.saveFiatCurrencies(response.data)
                            store.dispatch(DetailsAction.AppCurrencyAction.SetCurrencies(response.data))
                        }
                    }
                }
            } else {
                withContext(Dispatchers.Main) {
                    store.dispatch(DetailsAction.AppCurrencyAction.SetCurrencies(loadedCurrencies))
                }
            }
        }
    }

    class EraseWalletMiddleware() {
        fun handle(action: DetailsAction.EraseWallet) {
            when (action) {
                is DetailsAction.EraseWallet.Proceed -> {
                    when (store.state.detailsState.eraseWalletState) {
                        EraseWalletState.Allowed ->
                            store.dispatch(NavigationAction.NavigateTo(AppScreen.DetailsConfirm))
                        EraseWalletState.NotAllowedByCard ->
                            store.dispatch(DetailsAction.EraseWallet.Proceed.NotAllowedByCard)
                        EraseWalletState.NotEmpty ->
                            store.dispatch(DetailsAction.EraseWallet.Proceed.NotEmpty)
                    }
                }
                is DetailsAction.EraseWallet.Cancel -> {
                    store.dispatch(NavigationAction.PopBackTo())
                }
                is DetailsAction.EraseWallet.Confirm -> {
                    scope.launch {
                        val result = tangemSdkManager.eraseWallet(
                                store.state.detailsState.card?.cardId
                        )
                        withContext(Dispatchers.Main) {
                            when (result) {
                                is CompletionResult.Success -> {
                                    store.dispatch(NavigationAction.PopBackTo(AppScreen.Home))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    class AppCurrencyMiddleware {
        fun handle(action: DetailsAction.AppCurrencyAction) {
            when (action) {
                is DetailsAction.AppCurrencyAction.SelectAppCurrency -> {
                    preferencesStorage.saveAppCurrency(action.fiatCurrencyName)
                    store.dispatch(GlobalAction.ChangeAppCurrency(action.fiatCurrencyName))
                    store.dispatch(WalletAction.LoadFiatRate)
                }
            }
        }
    }

    class ManageSecurityMiddleware {
        fun handle(action: DetailsAction.ManageSecurity) {
            when (action) {
                is DetailsAction.ManageSecurity.OpenSecurity -> {
                    store.dispatch(NavigationAction.NavigateTo(AppScreen.DetailsSecurity))
                }
                is DetailsAction.ManageSecurity.ConfirmSelection -> {
                    if (action.option != store.state.detailsState.securityScreenState?.currentOption) {
                        if (action.option == SecurityOption.LongTap) {
                            store.dispatch(DetailsAction.ManageSecurity.SaveChanges)
                        } else {
                            store.dispatch(NavigationAction.NavigateTo(AppScreen.DetailsConfirm))
                        }
                    } else {
                        store.dispatch(DetailsAction.ManageSecurity.ConfirmSelection.AlreadySet)
                    }
                }
                is DetailsAction.ManageSecurity.SaveChanges -> {
                    val cardId = store.state.detailsState.card?.cardId
                    val selectedOption = store.state.detailsState.securityScreenState?.selectedOption
                    scope.launch {
                        val result = when (selectedOption) {
                            SecurityOption.LongTap -> tangemSdkManager.setLongTap(cardId)
                            SecurityOption.PassCode -> tangemSdkManager.setPasscode(cardId)
                            SecurityOption.AccessCode -> tangemSdkManager.setAccessCode(cardId)
                            else -> null
                        }
                        withContext(Dispatchers.Main) {
                            when (result) {
                                is CompletionResult.Success -> {
                                    if (selectedOption != SecurityOption.LongTap) {
                                        store.dispatch(NavigationAction.PopBackTo())
                                    }
                                    store.dispatch(DetailsAction.ManageSecurity.SaveChanges.Success)
                                }
                                is CompletionResult.Failure, null ->
                                    store.dispatch(DetailsAction.ManageSecurity.SaveChanges.Failure)
                            }
                        }

                    }

                }
            }
        }
    }
}

