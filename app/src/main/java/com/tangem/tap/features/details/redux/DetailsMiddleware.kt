package com.tangem.tap.features.details.redux

import com.tangem.commands.common.network.Result
import com.tangem.common.CompletionResult
import com.tangem.tap.common.redux.AppState
import com.tangem.tap.common.redux.global.GlobalAction
import com.tangem.tap.common.redux.navigation.AppScreen
import com.tangem.tap.common.redux.navigation.NavigationAction
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
    val detailsMiddleware: Middleware<AppState> = { dispatch, state ->
        { next ->
            { action ->
                when (action) {
                    is DetailsAction.PrepareScreen -> {
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
                            val result = tangemSdkManager.eraseWallet()
                            withContext(Dispatchers.Main) {
                                when (result) {
                                    is CompletionResult.Success -> {
                                        store.dispatch(NavigationAction.PopBackTo(AppScreen.Home))
                                    }
                                }
                            }
                        }
                    }
                    is DetailsAction.AppCurrencyAction.SelectAppCurrency -> {
                        preferencesStorage.saveAppCurrency(action.fiatCurrencyName)
                        store.dispatch(GlobalAction.ChangeAppCurrency(action.fiatCurrencyName))
                        store.dispatch(WalletAction.LoadFiatRate)
                    }
                }
                next(action)
            }
        }
    }
}
