package com.tangem.tap.features.details.ui.appcurrency

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tangem.common.routing.AppRouter
import com.tangem.core.analytics.api.AnalyticsEventHandler

import com.tangem.domain.appcurrency.GetAvailableCurrenciesUseCase
import com.tangem.domain.appcurrency.GetSelectedAppCurrencyUseCase
import com.tangem.domain.appcurrency.SelectAppCurrencyUseCase
import com.tangem.tap.common.analytics.events.Settings
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
internal class AppCurrencySelectorViewModel @Inject constructor(
    private val getSelectedAppCurrencyUseCase: GetSelectedAppCurrencyUseCase,
    private val getAvailableCurrenciesUseCase: GetAvailableCurrenciesUseCase,
    private val selectAppCurrencyUseCase: SelectAppCurrencyUseCase,
    private val router: AppRouter,
    private val dispatchers: CoroutineDispatcherProvider,
    private val analyticsEventHandler: AnalyticsEventHandler,
) : ViewModel(), AppCurrencySelectorIntents {

    private val stateController = AppCurrencySelectorStateHolder(
        intents = this,
        onSubscription = { fetchCurrencies() },
        stateFlowScope = viewModelScope,
    )

    val uiState: StateFlow<AppCurrencySelectorState> = stateController.stateFlow

    override fun onBackClick() {
        router.pop()
    }

    override fun onSearchClick() {
        stateController.updateStateWithSearch()
    }

    override fun onSearchInputChange(input: String) {
        stateController.updateStateWithSearch(input)
    }

    override fun onCurrencyClick(currency: AppCurrencySelectorState.Currency) {
        viewModelScope.launch(dispatchers.io) {
            selectAppCurrencyUseCase(currency.id)
                .onRight {
                    analyticsEventHandler.send(
                        event = Settings.AppSettings.MainCurrencyChanged(currencyType = currency.name),
                    )
                    router.pop()
                }
        }
    }

    override fun onDismissSearchClick() {
        stateController.updateStateWithoutSearch()
    }

    private fun fetchCurrencies() {
        viewModelScope.launch(dispatchers.io) {
            val availableCurrencies = getAvailableCurrenciesUseCase()
                .onRight(stateController::updateStateWithAvailableCurrencies)
                .getOrNull()

            getSelectedAppCurrencyUseCase().collectLatest { maybeSelectedCurrency ->
                val selectedCurrency = maybeSelectedCurrency.getOrNull() ?: return@collectLatest
                val selectedCurrencyIndex = availableCurrencies?.indexOfFirst { it == selectedCurrency }

                if (selectedCurrencyIndex != null && selectedCurrencyIndex != -1) {
                    stateController.updateStateWithSelectedCurrency(selectedCurrency, selectedCurrencyIndex)
                }
            }
        }
    }
}