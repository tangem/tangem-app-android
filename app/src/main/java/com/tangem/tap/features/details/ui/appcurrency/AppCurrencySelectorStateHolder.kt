package com.tangem.tap.features.details.ui.appcurrency

import com.tangem.core.ui.event.consumedEvent
import com.tangem.core.ui.event.triggeredEvent
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.tap.features.details.ui.appcurrency.converter.CurrencyConverter
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.mutate
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*

internal class AppCurrencySelectorStateHolder(
    private val intents: AppCurrencySelectorIntents,
    private val onSubscription: () -> Unit,
    stateFlowScope: CoroutineScope,
) {

    private var availableCurrencies: PersistentList<AppCurrencySelectorState.Currency> = persistentListOf()

    private val stateFlowInternal: MutableStateFlow<AppCurrencySelectorState> = MutableStateFlow(getInitialState())

    private val currencyConverter by lazy(mode = LazyThreadSafetyMode.NONE) { CurrencyConverter() }

    val stateFlow: StateFlow<AppCurrencySelectorState> = stateFlowInternal
        .onSubscription { onSubscription() }
        .stateIn(
            scope = stateFlowScope,
            started = SharingStarted.WhileSubscribed(),
            initialValue = getInitialState(),
        )

    fun updateStateWithAvailableCurrencies(currencies: List<AppCurrency>) {
        availableCurrencies = currencyConverter.convertList(currencies).toPersistentList()

        updateState {
            when (this) {
                is AppCurrencySelectorState.Loading -> getContentState(availableCurrencies)
                is AppCurrencySelectorState.Content -> copySealed(items = availableCurrencies)
            }
        }
    }

    fun updateStateWithSelectedCurrency(selectedCurrency: AppCurrency, selectedCurrencyIndex: Int) {
        val selectedCurrencyId = selectedCurrency.code

        updateState {
            when (this) {
                is AppCurrencySelectorState.Loading -> this
                is AppCurrencySelectorState.Content -> copySealed(
                    selectedId = selectedCurrencyId,
                    scrollToSelected = triggeredEvent(selectedCurrencyIndex, ::consumeScrollToSelectedEvent),
                )
            }
        }
    }

    fun updateStateWithSearch(input: String = "") {
        val filteredItems = availableCurrencies.mutate { list ->
            list.removeAll { input.lowercase() !in it.name.lowercase() }
        }

        updateState {
            when (this) {
                is AppCurrencySelectorState.Loading -> this
                is AppCurrencySelectorState.Search -> copy(items = filteredItems)
                is AppCurrencySelectorState.Default -> getSearchState(filteredItems, selectedId)
            }
        }
    }

    fun updateStateWithoutSearch() {
        updateState {
            when (this) {
                is AppCurrencySelectorState.Search -> getContentState(availableCurrencies, selectedId)
                is AppCurrencySelectorState.Default,
                is AppCurrencySelectorState.Loading,
                -> this
            }
        }
    }

    private fun getInitialState() = AppCurrencySelectorState.Loading(
        onBackClick = intents::onBackClick,
    )

    private fun getContentState(
        items: PersistentList<AppCurrencySelectorState.Currency>,
        selectedCurrencyId: String = "",
    ) = AppCurrencySelectorState.Default(
        selectedId = selectedCurrencyId,
        items = items,
        onBackClick = intents::onBackClick,
        onCurrencyClick = intents::onCurrencyClick,
        onTopBarActionClick = intents::onSearchClick,
        scrollToSelected = consumedEvent(),
    )

    private fun getSearchState(
        filteredItems: PersistentList<AppCurrencySelectorState.Currency>,
        selectedCurrencyId: String,
    ) = AppCurrencySelectorState.Search(
        selectedId = selectedCurrencyId,
        items = filteredItems,
        scrollToSelected = consumedEvent(),
        onBackClick = intents::onBackClick,
        onCurrencyClick = intents::onCurrencyClick,
        onSearchInputChange = intents::onSearchInputChange,
        onTopBarActionClick = intents::onDismissSearchClick,
    )

    private inline fun updateState(block: AppCurrencySelectorState.() -> AppCurrencySelectorState) {
        stateFlowInternal.update(block)
    }

    private fun consumeScrollToSelectedEvent() {
        updateState {
            when (this) {
                is AppCurrencySelectorState.Loading -> this
                is AppCurrencySelectorState.Content -> copySealed(scrollToSelected = consumedEvent())
            }
        }
    }
}