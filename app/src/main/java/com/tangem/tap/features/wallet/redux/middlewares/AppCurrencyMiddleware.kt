package com.tangem.tap.features.wallet.redux.middlewares

import com.tangem.common.extensions.guard
import com.tangem.core.analytics.Analytics
import com.tangem.data.source.preferences.model.DataSourceCurrency
import com.tangem.data.source.preferences.model.DataSourceFiatCurrency
import com.tangem.data.source.preferences.storage.FiatCurrenciesPrefStorage
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
import com.tangem.tap.scope
import com.tangem.tap.store
import com.tangem.tap.userWalletsListManager
import kotlinx.coroutines.launch
import timber.log.Timber

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
                ),
            )
        }

        scope.launch {
            runCatching { walletRepository.getCurrencyList() }
                .onSuccess { response ->
                    val currenciesList = response.currencies
                        .map { with(it) { DataSourceCurrency(id, code, name, rateBTC, unit, type) } }

                    if (currenciesList.isNotEmpty() && currenciesList.toSet() != storedFiatCurrencies.toSet()) {
                        fiatCurrenciesPrefStorage.save(currenciesList)
                        store.dispatchDialogShow(
                            WalletDialog.CurrencySelectionDialog(
                                currenciesList = currenciesList.mapToUiModel(),
                                currentAppCurrency = appCurrencyProvider.invoke(),
                            ),
                        )
                    }
                }
        }
    }

    private fun selectCurrency(action: WalletAction.AppCurrencyAction.SelectAppCurrency) {
        Analytics.send(MainScreen.MainCurrencyChanged(AnalyticsParam.CurrencyType.FiatCurrency(action.fiatCurrency)))
        fiatCurrenciesPrefStorage.saveAppCurrency(
            with(action.fiatCurrency) { DataSourceFiatCurrency(code, name, symbol) },
        )
        store.dispatch(GlobalAction.ChangeAppCurrency(action.fiatCurrency))
        store.dispatch(DetailsAction.ChangeAppCurrency(action.fiatCurrency))
        store.dispatch(WalletSelectorAction.ChangeAppCurrency(action.fiatCurrency))
        val selectedUserWallet = userWalletsListManager.selectedUserWalletSync.guard {
            Timber.e("Unable to select currency, no user wallet selected")
            return
        }
        scope.launch {
            tapWalletManager.loadData(selectedUserWallet, refresh = true)
        }
    }

    private fun List<DataSourceCurrency>.mapToUiModel(): List<FiatCurrency> {
        return this.map {
            FiatCurrency(
                code = it.code,
                name = it.name,
                symbol = it.unit,
            )
        }
    }
}