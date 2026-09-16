package com.tangem.features.tangempay.eligibility.country

import androidx.compose.runtime.Stable
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.ui.R
import com.tangem.core.ui.ds2.search.TangemSearch
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.domain.models.pay.TangemPayCountry
import com.tangem.domain.models.pay.TangemPayRegion
import com.tangem.domain.pay.usecase.GetTangemPayCountriesUseCase
import com.tangem.domain.pay.usecase.GetTangemPayRegionsUseCase
import com.tangem.features.tangempay.eligibility.TangemPayCountrySelectorComponent.Params
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@Stable
@ModelScoped
internal class CountrySelectorModel @Inject constructor(
    override val dispatchers: CoroutineDispatcherProvider,
    private val getCountriesUseCase: GetTangemPayCountriesUseCase,
    private val getRegionsUseCase: GetTangemPayRegionsUseCase,
    paramsContainer: ParamsContainer,
) : Model() {

    private val params = paramsContainer.require<Params>()

    val uiState: StateFlow<ResidencyListUM>
        field = MutableStateFlow(getInitialState())

    private var step: Step = when (val params = params) {
        is Params.Countries -> Step.Countries
        is Params.Regions -> Step.Regions(country = null, countryCode = params.countryCode)
    }
    private var data: ResidencyListData = ResidencyListData.Loading
    private var countries: List<TangemPayCountry> = emptyList()

    init {
        load()
    }

    private fun showCountries() {
        step = Step.Countries
        uiState.update { state ->
            state.copy(
                title = resourceReference(R.string.tangempay_residency_title_country),
                search = state.search.copy(query = "", isActive = false),
                onBackClick = null,
            )
        }
        load()
    }

    private fun showRegions(country: TangemPayCountry) {
        step = Step.Regions(country = country, countryCode = country.code)
        uiState.update { state ->
            state.copy(
                title = resourceReference(R.string.tangempay_residency_title_state),
                search = state.search.copy(query = "", isActive = false),
                onBackClick = ::showCountries,
            )
        }
        load()
    }

    private fun load() {
        data = ResidencyListData.Loading
        updateContent()
        when (val step = step) {
            Step.Countries -> loadCountries()
            is Step.Regions -> loadRegions(step.countryCode)
        }
    }

    private fun loadCountries() {
        modelScope.launch {
            if (countries.isNotEmpty()) {
                setCountriesLoaded()
                return@launch
            }
            getCountriesUseCase()
                .onLeft { setFailed(onRetry = ::loadCountries) }
                .onRight { loaded ->
                    countries = loaded
                    setCountriesLoaded()
                }
        }
    }

    private fun loadRegions(countryCode: String) {
        modelScope.launch {
            getRegionsUseCase(countryCode)
                .onLeft { setFailed(onRetry = { loadRegions(countryCode) }) }
                .onRight(::setRegionsLoaded)
        }
    }

    private fun onCountryClick(country: TangemPayCountry) {
        val params = params as? Params.Countries ?: return
        if (country.hasRegions) {
            showRegions(country)
        } else {
            params.onSelected(country, null)
            params.onDismiss()
        }
    }

    private fun onRegionClick(region: TangemPayRegion) {
        when (val params = params) {
            is Params.Countries -> {
                val country = (step as? Step.Regions)?.country ?: return
                params.onSelected(country, region)
            }
            is Params.Regions -> params.onSelected(region)
        }
        params.onDismiss()
    }

    private fun setCountriesLoaded() {
        data = ResidencyListData.Loaded(
            suggested = countries.filter(TangemPayCountry::isSuggested).map(::toItem),
            all = countries.map(::toItem),
        )
        updateContent()
    }

    private fun setRegionsLoaded(regions: List<TangemPayRegion>) {
        data = ResidencyListData.Loaded(
            suggested = regions.filter(TangemPayRegion::isSuggested).map(::toItem),
            all = regions.map(::toItem),
        )
        updateContent()
    }

    private fun setFailed(onRetry: () -> Unit) {
        data = ResidencyListData.Failed(onRetry = onRetry)
        updateContent()
    }

    private fun onQueryChange(query: String) {
        uiState.update { state -> state.copy(search = state.search.copy(query = query)) }
        updateContent()
    }

    private fun onActiveChange(isActive: Boolean) {
        uiState.update { state -> state.copy(search = state.search.copy(isActive = isActive)) }
    }

    private fun updateContent() {
        uiState.update { state ->
            state.copy(
                content = data.toContent(
                    query = state.search.query,
                    suggestedTitle = suggestedSectionTitle(),
                    allTitle = allSectionTitle(),
                ),
            )
        }
    }

    private fun toItem(country: TangemPayCountry): ResidencyListUM.ItemUM = ResidencyListUM.ItemUM(
        id = country.code,
        name = country.name,
        flagUrl = country.flagUrl,
        isLoading = false,
        onClick = { onCountryClick(country) },
    )

    private fun toItem(region: TangemPayRegion): ResidencyListUM.ItemUM = ResidencyListUM.ItemUM(
        id = region.code,
        name = region.name,
        flagUrl = null,
        isLoading = false,
        onClick = { onRegionClick(region) },
    )

    private fun getInitialState(): ResidencyListUM = ResidencyListUM(
        title = when (params) {
            is Params.Countries -> resourceReference(R.string.tangempay_residency_title_country)
            is Params.Regions -> resourceReference(R.string.tangempay_residency_title_state)
        },
        search = TangemSearch.State(
            placeholderText = resourceReference(R.string.common_search),
            query = "",
            onQueryChange = ::onQueryChange,
            isActive = false,
            onActiveChange = ::onActiveChange,
            onCloseClick = null,
        ),
        content = ResidencyListUM.Content.Loading,
        onBackClick = null,
        onCloseClick = params.onDismiss,
    )

    private fun suggestedSectionTitle(): TextReference = when (step) {
        Step.Countries -> resourceReference(R.string.tangempay_residency_suggested_country)
        is Step.Regions -> resourceReference(R.string.tangempay_residency_suggested_state)
    }

    private fun allSectionTitle(): TextReference = when (step) {
        Step.Countries -> resourceReference(R.string.tangempay_residency_all_country)
        is Step.Regions -> resourceReference(R.string.tangempay_residency_all_state)
    }

    private sealed interface Step {

        data object Countries : Step

        data class Regions(val country: TangemPayCountry?, val countryCode: String) : Step
    }
}