package com.tangem.tap.features.details.ui.appcurrency

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tangem.core.navigation.ReduxNavController
import com.tangem.domain.appcurrency.GetAvailableCurrenciesUseCase
import com.tangem.domain.appcurrency.GetSelectedAppCurrencyUseCase
import com.tangem.domain.appcurrency.SelectAppCurrencyUseCase
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
    private val reduxNavController: ReduxNavController,
    private val dispatchers: CoroutineDispatcherProvider,
) : ViewModel(), AppCurrencySelectorIntents {

    private val stateController = AppCurrencySelectorStateHolder(
        intents = this,
        onSubscription = { fetchCurrencies() },
        stateFlowScope = viewModelScope,
    )

    val uiState: StateFlow<AppCurrencySelectorState> = stateController.stateFlow

    override fun onBackClick() {
        reduxNavController.popBackStack()
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
                .onRight { reduxNavController.popBackStack() }
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
                maybeSelectedCurrency
                    .onRight { selectedCurrency ->
                        val selectedCurrencyIndex = availableCurrencies?.indexOfFirst { it == selectedCurrency }

                        if (selectedCurrencyIndex != null && selectedCurrencyIndex != -1) {
                            stateController.updateStateWithSelectedCurrency(selectedCurrency, selectedCurrencyIndex)
                        }
                    }
            }
        }
    }
}
