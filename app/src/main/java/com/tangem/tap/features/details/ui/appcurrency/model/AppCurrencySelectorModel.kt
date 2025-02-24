package com.tangem.tap.features.details.ui.appcurrency.model

import androidx.compose.runtime.Stable
import com.tangem.common.routing.AppRouter
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.decompose.di.ComponentScoped
import com.tangem.core.decompose.model.Model
import com.tangem.domain.appcurrency.GetAvailableCurrenciesUseCase
import com.tangem.domain.appcurrency.GetSelectedAppCurrencyUseCase
import com.tangem.domain.appcurrency.SelectAppCurrencyUseCase
import com.tangem.tap.common.analytics.events.Settings
import com.tangem.tap.features.details.ui.appcurrency.AppCurrencySelectorIntents
import com.tangem.tap.features.details.ui.appcurrency.AppCurrencySelectorState
import com.tangem.tap.features.details.ui.appcurrency.AppCurrencySelectorStateHolder
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@Stable
@ComponentScoped
internal class AppCurrencySelectorModel @Inject constructor(
    override val dispatchers: CoroutineDispatcherProvider,
    private val getSelectedAppCurrencyUseCase: GetSelectedAppCurrencyUseCase,
    private val getAvailableCurrenciesUseCase: GetAvailableCurrenciesUseCase,
    private val selectAppCurrencyUseCase: SelectAppCurrencyUseCase,
    private val router: AppRouter,
    private val analyticsEventHandler: AnalyticsEventHandler,
) : Model(), AppCurrencySelectorIntents {

    private val stateController = AppCurrencySelectorStateHolder(
        intents = this,
        onSubscription = { fetchCurrencies() },
        stateFlowScope = modelScope,
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
        modelScope.launch(dispatchers.io) {
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
        modelScope.launch(dispatchers.io) {
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