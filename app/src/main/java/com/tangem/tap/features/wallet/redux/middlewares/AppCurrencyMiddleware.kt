package com.tangem.tap.features.wallet.redux.middlewares

import com.tangem.datasource.api.tangemTech.models.CurrenciesResponse
import com.tangem.core.analytics.Analytics
import com.tangem.tap.common.analytics.events.AnalyticsParam
import com.tangem.tap.common.analytics.events.MainScreen
import com.tangem.tap.common.entities.FiatCurrency
import com.tangem.tap.common.extensions.dispatchDialogShow
import com.tangem.tap.common.redux.global.GlobalAction
import com.tangem.tap.domain.TapWalletManager
import com.tangem.tap.features.details.redux.DetailsAction
import com.tangem.tap.features.wallet.domain.WalletRepository
import com.tangem.tap.features.wallet.redux.WalletAction
import com.tangem.tap.features.wallet.redux.models.WalletDialog
import com.tangem.tap.features.walletSelector.redux.WalletSelectorAction
import com.tangem.tap.persistence.FiatCurrenciesPrefStorage
import com.tangem.tap.scope
import com.tangem.tap.store
import com.tangem.tap.userWalletsListManager
import kotlinx.coroutines.launch

class AppCurrencyMiddleware(
    private val walletRepository: WalletRepository,
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
                WalletDialog.CurrencySelectionDialog(
                    currenciesList = storedFiatCurrencies.mapToUiModel(),
                    currentAppCurrency = appCurrencyProvider.invoke(),
                )
            )
        }

        scope.launch {
            runCatching { walletRepository.getCurrencyList() }
                .onSuccess {
                    val currenciesList = it.currencies
                    if (currenciesList.isNotEmpty() && !currenciesList.toSet().equals(storedFiatCurrencies.toSet())) {
                        fiatCurrenciesPrefStorage.save(currenciesList)
                        store.dispatchDialogShow(
                            WalletDialog.CurrencySelectionDialog(
                                currenciesList = currenciesList.mapToUiModel(),
                                currentAppCurrency = appCurrencyProvider.invoke(),
                            )
                        )
                    }
                }
        }
    }

    private fun selectCurrency(action: WalletAction.AppCurrencyAction.SelectAppCurrency) {
        Analytics.send(MainScreen.MainCurrencyChanged(AnalyticsParam.CurrencyType.FiatCurrency(action.fiatCurrency)))
        fiatCurrenciesPrefStorage.saveAppCurrency(action.fiatCurrency)
        store.dispatch(GlobalAction.ChangeAppCurrency(action.fiatCurrency))
        store.dispatch(DetailsAction.ChangeAppCurrency(action.fiatCurrency))
        store.dispatch(WalletSelectorAction.ChangeAppCurrency(action.fiatCurrency))
        val selectedUserWallet = userWalletsListManager.selectedUserWalletSync
        if (selectedUserWallet != null) {
            scope.launch {
                tapWalletManager.loadData(selectedUserWallet, refresh = true)
            }
        } else {
            tapWalletManager.rates.clear()
            store.dispatch(WalletAction.LoadFiatRate())
        }
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
