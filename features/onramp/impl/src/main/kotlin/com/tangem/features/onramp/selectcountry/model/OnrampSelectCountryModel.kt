package com.tangem.features.onramp.selectcountry.model

import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.ui.components.fields.InputManager
import com.tangem.core.ui.components.fields.entity.SearchBarUM
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.domain.onramp.FetchOnrampCountriesUseCase
import com.tangem.domain.onramp.GetOnrampCountriesUseCase
import com.tangem.domain.onramp.GetOnrampCountryUseCase
import com.tangem.domain.onramp.OnrampSaveDefaultCountryUseCase
import com.tangem.domain.onramp.analytics.OnrampAnalyticsEvent
import com.tangem.domain.onramp.model.OnrampCountry
import com.tangem.domain.wallets.usecase.GetWalletsUseCase
import com.tangem.features.onramp.impl.R
import com.tangem.features.onramp.selectcountry.SelectCountryComponent
import com.tangem.features.onramp.selectcountry.entity.CountryItemState
import com.tangem.features.onramp.selectcountry.entity.CountryListUM
import com.tangem.features.onramp.selectcountry.entity.CountryListUMController
import com.tangem.features.onramp.selectcountry.entity.transformer.UpdateCountryItemsErrorTransformer
import com.tangem.features.onramp.selectcountry.entity.transformer.UpdateCountryItemsLoadingTransformer
import com.tangem.features.onramp.selectcountry.entity.transformer.UpdateCountryItemsTransformer
import com.tangem.features.onramp.utils.UpdateSearchBarActiveStateTransformer
import com.tangem.features.onramp.utils.UpdateSearchQueryTransformer
import com.tangem.features.onramp.utils.sendOnrampErrorEvent
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

@Suppress("LongParameterList")
@ModelScoped
internal class OnrampSelectCountryModel @Inject constructor(
    override val dispatchers: CoroutineDispatcherProvider,
    private val analyticsEventHandler: AnalyticsEventHandler,
    private val searchManager: InputManager,
    private val getOnrampCountriesUseCase: GetOnrampCountriesUseCase,
    private val saveDefaultCountryUseCase: OnrampSaveDefaultCountryUseCase,
    private val getOnrampCountryUseCase: GetOnrampCountryUseCase,
    private val fetchOnrampCountriesUseCase: FetchOnrampCountriesUseCase,
    getWalletsUseCase: GetWalletsUseCase,
    paramsContainer: ParamsContainer,
) : Model() {

    private val params: SelectCountryComponent.Params = paramsContainer.require()
    private val userWallet = getWalletsUseCase.invokeSync().first { it.walletId == params.userWalletId }

    val state: StateFlow<CountryListUM> get() = controller.state

    private val controller = CountryListUMController(
        searchBarUM = createSearchBarUM(),
        loadingItems = loadingItems,
    )

    init {
        analyticsEventHandler.send(OnrampAnalyticsEvent.SelectResidenceOpened)
        updateCountriesList()
        modelScope.launch { subscribeOnUpdateState() }
    }

    fun dismiss(isCountrySelected: Boolean) {
        params.onDismiss(isCountrySelected)
    }

    private suspend fun subscribeOnUpdateState() {
        combine(
            flow = getOnrampCountriesUseCase(),
            flow2 = getOnrampCountryUseCase(),
            flow3 = searchManager.query,
        ) { maybeCountries, maybeCountry, query ->
            maybeCountries.onLeft {
                analyticsEventHandler.sendOnrampErrorEvent(it, params.cryptoCurrency.symbol)
            }
            maybeCountry.onLeft {
                analyticsEventHandler.sendOnrampErrorEvent(it, params.cryptoCurrency.symbol)
            }
            UpdateCountryItemsTransformer(
                maybeCountries = maybeCountries,
                defaultCountry = maybeCountry.getOrNull(),
                query = query,
                onRetry = ::onRetry,
                onCountryClick = ::saveCountry,
            )
        }
            .onEach(controller::update)
            .launchIn(modelScope)
    }

    private fun saveCountry(country: OnrampCountry) {
        analyticsEventHandler.send(OnrampAnalyticsEvent.OnResidenceChosen(country.name))
        modelScope.launch {
            saveDefaultCountryUseCase.invoke(country)
            dismiss(isCountrySelected = true)
        }
    }

    private fun onRetry() {
        controller.update(UpdateCountryItemsLoadingTransformer(loadingItems))
        updateCountriesList()
    }

    private fun updateCountriesList() {
        modelScope.launch {
            fetchOnrampCountriesUseCase(userWallet).onLeft {
                controller.update(UpdateCountryItemsErrorTransformer(onRetry = ::onRetry))
            }
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
            placeholderText = resourceReference(R.string.onramp_country_search),
            query = "",
            onQueryChange = ::onSearchQueryChange,
            isActive = false,
            onActiveChange = ::onSearchBarActiveChange,
        )
    }

    private companion object {
        const val LOADING_ITEMS_COUNT = 5
        val loadingItems: ImmutableList<CountryItemState.Loading> = MutableList(LOADING_ITEMS_COUNT) {
            CountryItemState.Loading("Loading #$it")
        }.toImmutableList()
    }
}