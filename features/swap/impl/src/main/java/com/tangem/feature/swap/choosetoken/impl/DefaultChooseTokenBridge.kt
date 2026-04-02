package com.tangem.feature.swap.choosetoken.impl

import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.feature.swap.choosetoken.api.ChooseTokenAnalyticsPayload
import com.tangem.feature.swap.choosetoken.api.ChooseTokenBridge
import com.tangem.feature.swap.choosetoken.api.ChooseTokenResult
import com.tangem.feature.swap.choosetoken.impl.model.ChooseTokenModel.Companion.DEBOUNCE_SEARCH_DELAY
import com.tangem.feature.swap.domain.models.ui.CurrenciesGroup
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*

internal class DefaultChooseTokenBridge @AssistedInject constructor(
    @Assisted private val modelScope: CoroutineScope,
) : ChooseTokenBridge {

    override val onCurrencyChosen: Channel<ChooseTokenResult> = Channel()

    override val onTokenSelected: Channel<Pair<String, ChooseTokenAnalyticsPayload.IsSearched>> = Channel()
    override val onNewTokenAdded: Channel<Pair<CryptoCurrency, ChooseTokenAnalyticsPayload.IsSearched>> = Channel()
    override val onClose: Channel<Unit> = Channel()

    private val onSearchQuery: Channel<String> = Channel()
    override val searchQueryState: StateFlow<String> = onSearchQuery.receiveAsFlow()
        .debounce(DEBOUNCE_SEARCH_DELAY)
        .stateIn(modelScope, SharingStarted.Eagerly, initialValue = "")

    private val _currenciesGroupFlow = MutableStateFlow<CurrenciesGroup?>(null)
    override val currenciesGroup: Flow<CurrenciesGroup> = _currenciesGroupFlow.filterNotNull()

    override fun onSearchQuery(query: String) {
        onSearchQuery.trySend(query)
    }

    override fun updateCurrenciesGroup(currenciesGroup: CurrenciesGroup) {
        _currenciesGroupFlow.update { currenciesGroup }
    }

    @AssistedFactory
    interface Factory : ChooseTokenBridge.Factory {
        override fun create(modelScope: CoroutineScope): DefaultChooseTokenBridge
    }
}