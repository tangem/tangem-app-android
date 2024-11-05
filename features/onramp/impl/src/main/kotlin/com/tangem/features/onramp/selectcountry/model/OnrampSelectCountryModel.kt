package com.tangem.features.onramp.selectcountry.model

import com.tangem.core.decompose.di.ComponentScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.features.onramp.impl.R
import com.tangem.features.onramp.selectcountry.entity.CountryItemState
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
internal class OnrampSelectCountryModel @Inject constructor(
    override val dispatchers: CoroutineDispatcherProvider,
    private val searchManager: SearchManager,
    private val countryListUMController: CountryListUMController,
) : Model() {

    val state: StateFlow<CountryListUM> = countryListUMController.state

    init {
        subscribeOnUpdateState()
    }

    private fun subscribeOnUpdateState() {
        combine(
            flow = MockedCountriesData.getCountryItems(),
            flow2 = searchManager.query,
        ) { countryItems, query ->
            val filteredCountryItems = countryItems.filterByQuery(query = query)

            UpdateCountryItemsTransformer(
                countries = filteredCountryItems,
                onQueryChange = ::onSearchQueryChange,
                onActiveChange = ::onSearchBarActiveChange,
            )
        }
            .onEach(countryListUMController::update)
            .flowOn(dispatchers.main)
            .launchIn(modelScope)
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

    // TODO: Temporarily. Will be refactored after implement domain
    private fun List<CountryItemState>.filterByQuery(query: String): List<CountryItemState> {
        return filter {
            (it as? CountryItemState.Content)?.countryName?.contains(query) == true ||
                (it as? CountryItemState.Unavailable)?.countryName?.contains(query) == true
        }
    }
}
