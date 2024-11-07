package com.tangem.features.onramp.selectcountry.model

import com.tangem.core.decompose.di.ComponentScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.domain.onramp.GetOnrampCountriesUseCase
import com.tangem.domain.onramp.GetOnrampCountryUseCase
import com.tangem.domain.onramp.OnrampSaveDefaultCountryUseCase
import com.tangem.domain.onramp.model.OnrampCountry
import com.tangem.features.onramp.impl.R
import com.tangem.features.onramp.selectcountry.SelectCountryComponent
import com.tangem.features.onramp.selectcountry.entity.CountryListUM
import com.tangem.features.onramp.selectcountry.entity.CountryListUMController
import com.tangem.features.onramp.selectcountry.entity.transformer.UpdateCountryItemsTransformer
import com.tangem.features.onramp.utils.SearchManager
import com.tangem.features.onramp.utils.UpdateSearchBarActiveStateTransformer
import com.tangem.features.onramp.utils.UpdateSearchQueryTransformer
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@ComponentScoped
@Suppress("LongParameterList")
internal class OnrampSelectCountryModel @Inject constructor(
    override val dispatchers: CoroutineDispatcherProvider,
    private val searchManager: SearchManager,
    private val countryListUMController: CountryListUMController,
    private val getOnrampCountriesUseCase: GetOnrampCountriesUseCase,
    private val saveDefaultCountryUseCase: OnrampSaveDefaultCountryUseCase,
    private val getOnrampCountryUseCase: GetOnrampCountryUseCase,
    paramsContainer: ParamsContainer,
) : Model() {

    val state: StateFlow<CountryListUM> = countryListUMController.state
    private val params: SelectCountryComponent.Params = paramsContainer.require()

    init {
        modelScope.launch { subscribeOnUpdateState() }
    }

    fun dismiss() {
        params.onDismiss()
    }

    private suspend fun subscribeOnUpdateState() {
        combine(
            flow = flowOf(getOnrampCountriesUseCase.invoke()),
            flow2 = getOnrampCountryUseCase.invoke(),
            flow3 = searchManager.query,
        ) { maybeCountries, maybeCountry, query ->
            UpdateCountryItemsTransformer(
                maybeCountries = maybeCountries,
                defaultCountry = maybeCountry.getOrNull(),
                query = query,
                onQueryChange = ::onSearchQueryChange,
                onActiveChange = ::onSearchBarActiveChange,
                onCountryClick = ::saveCountry,
            )
        }
            .onEach(countryListUMController::update)
            .launchIn(modelScope)
    }

    private fun saveCountry(country: OnrampCountry) {
        modelScope.launch {
            saveDefaultCountryUseCase.invoke(country)
            dismiss()
        }
    }

    private fun onSearchQueryChange(newQuery: String) {
        val searchBar = countryListUMController.getSearchBar()
        if (searchBar?.searchBarUM?.query == newQuery) return

        modelScope.launch {
            countryListUMController.update(transformer = UpdateSearchQueryTransformer(newQuery))

            searchManager.update(newQuery)
        }
    }

    private fun onSearchBarActiveChange(isActive: Boolean) {
        countryListUMController.update(
            transformer = UpdateSearchBarActiveStateTransformer(
                isActive = isActive,
                placeHolder = resourceReference(id = R.string.common_search),
            ),
        )
    }
}