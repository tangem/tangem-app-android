package com.tangem.tap.features.wallet.redux.middlewares

import com.tangem.common.services.Result
import com.tangem.network.api.tangemTech.CurrenciesResponse
import com.tangem.network.api.tangemTech.TangemTechService
import com.tangem.tap.common.entities.FiatCurrency
import com.tangem.tap.common.extensions.dispatchDialogShow
import com.tangem.tap.common.redux.AppDialog
import com.tangem.tap.common.redux.global.GlobalAction
import com.tangem.tap.domain.TapWalletManager
import com.tangem.tap.features.wallet.redux.WalletAction
import com.tangem.tap.persistence.FiatCurrenciesPrefStorage
import com.tangem.tap.scope
import com.tangem.tap.store
import kotlinx.coroutines.launch

class AppCurrencyMiddleware(
    private val tangemTechService: TangemTechService,
    private val tapWalletManager: TapWalletManager,
    private val fiatCurrenciesPrefStorage: FiatCurrenciesPrefStorage,
    private val appCurrencyProvider: () -> FiatCurrency,
) {
    fun handle(action: WalletAction.AppCurrencyAction) {
        when (action) {
            is WalletAction.AppCurrencyAction.ChooseAppCurrency -> showSelector()
            is WalletAction.AppCurrencyAction.SelectAppCurrency -> selectCurrency(action)
        }
    }

    private fun showSelector() {
        val storedFiatCurrencies = fiatCurrenciesPrefStorage.restore()
        if (storedFiatCurrencies.isNotEmpty()) {
            store.dispatchDialogShow(
                AppDialog.CurrencySelectionDialog(
                    currenciesList = storedFiatCurrencies.mapToUiModel(),
                    currentAppCurrency = appCurrencyProvider.invoke()
                )
            )
        }

        scope.launch {
            when (val result = tangemTechService.currencies()) {
                is Result.Success -> {
                    val currenciesList = result.data.currencies
                    if (currenciesList.isNotEmpty() &&
                        currenciesList.toSet() != storedFiatCurrencies.toSet()
                    ) {
                        fiatCurrenciesPrefStorage.save(currenciesList)
                        store.dispatchDialogShow(
                            AppDialog.CurrencySelectionDialog(
                                currenciesList = currenciesList.mapToUiModel(),
                                currentAppCurrency = appCurrencyProvider.invoke()
                            )
                        )
                    }
                }
                is Result.Failure -> {}
            }
        }
    }

    private fun selectCurrency(action: WalletAction.AppCurrencyAction.SelectAppCurrency) {
        tapWalletManager.rates.clear()
        fiatCurrenciesPrefStorage.saveAppCurrency(action.fiatCurrency)
        store.dispatch(GlobalAction.ChangeAppCurrency(action.fiatCurrency))
        store.dispatch(WalletAction.LoadFiatRate())
    }

    private fun List<CurrenciesResponse.Currency>.mapToUiModel(): List<FiatCurrency> {
        return this.map {
            FiatCurrency(
                code = it.code,
                name = it.name,
                symbol = it.unit
            )
        }
    }
}