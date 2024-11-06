package com.tangem.features.onramp.selectcurrency.model

import com.tangem.core.decompose.di.ComponentScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.ui.components.fields.entity.SearchBarUM
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.domain.onramp.GetOnrampCurrenciesUseCase
import com.tangem.domain.onramp.OnrampSaveDefaultCurrencyUseCase
import com.tangem.domain.onramp.model.OnrampCurrency
import com.tangem.features.onramp.impl.R
import com.tangem.features.onramp.selectcurrency.SelectCurrencyComponent
import com.tangem.features.onramp.selectcurrency.entity.CurrenciesListUM
import com.tangem.features.onramp.selectcurrency.entity.CurrencyListController
import com.tangem.features.onramp.selectcurrency.entity.transformer.UpdateCurrencyItemsTransformer
import com.tangem.features.onramp.utils.SearchManager
import com.tangem.features.onramp.utils.UpdateSearchBarActiveStateTransformer
import com.tangem.features.onramp.utils.UpdateSearchQueryTransformer
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@ComponentScoped
internal class OnrampSelectCurrencyModel @Inject constructor(
    override val dispatchers: CoroutineDispatcherProvider,
    private val searchManager: SearchManager,
    private val getOnrampCurrenciesUseCase: GetOnrampCurrenciesUseCase,
    private val saveDefaultCurrencyUseCase: OnrampSaveDefaultCurrencyUseCase,
    paramsContainer: ParamsContainer,
) : Model() {

    val state: StateFlow<CurrenciesListUM> get() = controller.state

    private val params: SelectCurrencyComponent.Params = paramsContainer.require()
    private val controller = CurrencyListController(currencySearchBarUM = createSearchBarUM())
    private val refreshTrigger = MutableSharedFlow<Unit>()

    init {
        subscribeOnUpdateState()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun subscribeOnUpdateState() {
        combine(
            flow = refreshTrigger.onStart { emit(Unit) }.flatMapLatest { flowOf(getOnrampCurrenciesUseCase.invoke()) },
            flow2 = searchManager.query,
        ) { maybeCurrencies, query ->
            UpdateCurrencyItemsTransformer(
                maybeCurrencies = maybeCurrencies,
                query = query,
                onRetry = ::onRetry,
                onCurrencyClick = ::saveDefaultCurrency,
            )
        }
            .onEach(controller::update)
            .launchIn(modelScope)
    }

    fun dismiss() {
        params.onDismiss()
    }

    private fun saveDefaultCurrency(currency: OnrampCurrency) {
        modelScope.launch {
            saveDefaultCurrencyUseCase.invoke(currency)
            dismiss()
        }
    }

    private fun onRetry() {
        modelScope.launch {
            refreshTrigger.emit(Unit)
        }
    }

    private fun onSearchQueryChange(newQuery: String) {
        val searchBarUM = controller.state.value.searchBarUM
        if (searchBarUM.query == newQuery) return

        modelScope.launch {
            controller.update(transformer = UpdateSearchQueryTransformer(newQuery))

            searchManager.update(newQuery)
        }
    }

    private fun onSearchBarActiveChange(isActive: Boolean) {
        controller.update(
            transformer = UpdateSearchBarActiveStateTransformer(
                isActive = isActive,
                placeHolder = resourceReference(id = R.string.common_search),
            ),
        )
    }

    private fun createSearchBarUM(): SearchBarUM {
        return SearchBarUM(
            placeholderText = resourceReference(R.string.onramp_currency_search),
            query = "",
            onQueryChange = ::onSearchQueryChange,
            isActive = false,
            onActiveChange = ::onSearchBarActiveChange,
        )
    }
}
