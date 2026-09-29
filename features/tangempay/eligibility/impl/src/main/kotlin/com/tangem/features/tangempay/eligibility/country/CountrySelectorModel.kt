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
import java.text.Collator
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
        is Params.Regions -> Step.Regions(
            country = null,
            countryCode = params.countryCode,
            selectedRegionCode = params.selectedRegionCode,
        )
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
                title = resourceReference(R.string.tangempay_order_data_country),
                search = state.search.copy(query = "", isActive = false),
                onBackClick = null,
            )
        }
        load()
    }

    private fun showRegions(country: TangemPayCountry, selectedRegionCode: String?) {
        step = Step.Regions(
            country = country,
            countryCode = country.code,
            selectedRegionCode = selectedRegionCode,
        )
        uiState.update { state ->
            state.copy(
                title = resourceReference(R.string.tangempay_withdraw_bank_geo_state_title),
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
            showRegions(country = country, selectedRegionCode = params.selectedRegionCodeOf(country))
        } else {
            params.onSelected(country, null)
            params.onDismiss()
        }
    }

    /** A region survives the jump to the region step only under the country it was picked in. */
    private fun Params.Countries.selectedRegionCodeOf(country: TangemPayCountry): String? =
        selectedRegionCode?.takeIf { country.code == selectedCountryCode }

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
        val items = countries.map(::toItem).sortedByName()
        val selectedCountryCode = (params as? Params.Countries)?.selectedCountryCode
        data = ResidencyListData.Loaded(
            suggested = items.highlighted { it == selectedCountryCode },
            all = items,
        )
        updateContent()
    }

    private fun setRegionsLoaded(regions: List<TangemPayRegion>) {
        val items = regions.map(::toItem).sortedByName()
        val selectedRegionCode = (step as? Step.Regions)?.selectedRegionCode
        data = ResidencyListData.Loaded(
            suggested = items.highlighted { code -> code.isSameSubdivisionAs(selectedRegionCode) },
            all = items,
        )
        updateContent()
    }

    /** The picked item leads the list in a section of its own, and the full list below it keeps it too. */
    private fun List<ResidencyListUM.ItemUM>.highlighted(
        isSelected: (code: String) -> Boolean,
    ): List<ResidencyListUM.ItemUM> = listOfNotNull(firstOrNull { item -> isSelected(item.id) })

    /** The address form stores a bare subdivision ("CA") while the geo API returns ISO 3166-2 ("US-CA"). */
    private fun String.isSameSubdivisionAs(other: String?): Boolean = other != null &&
        substringAfterLast(delimiter = '-').equals(other.substringAfterLast(delimiter = '-'), ignoreCase = true)

    /** Names come localized, so a [Collator] keeps "Åland" and "Türkiye" in the alphabet instead of after "Z". */
    private fun List<ResidencyListUM.ItemUM>.sortedByName(): List<ResidencyListUM.ItemUM> =
        sortedWith(compareBy(Collator.getInstance(), ResidencyListUM.ItemUM::name))

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
        // Without a pick the picker is one plain list, so the headers that name the two sections stay off.
        val hasHighlighted = (data as? ResidencyListData.Loaded)?.suggested?.isNotEmpty() == true
        uiState.update { state ->
            state.copy(
                content = data.toContent(
                    query = state.search.query,
                    suggestedTitle = if (hasHighlighted) selectedSectionTitle() else null,
                    allTitle = if (hasHighlighted) allSectionTitle() else null,
                ),
            )
        }
    }

    private fun selectedSectionTitle(): TextReference = when (step) {
        Step.Countries -> resourceReference(R.string.tangempay_withdraw_bank_geo_country_selected)
        is Step.Regions -> resourceReference(R.string.tangempay_withdraw_bank_geo_state_selected)
    }

    private fun allSectionTitle(): TextReference = when (step) {
        Step.Countries -> resourceReference(R.string.tangempay_residency_all_country)
        is Step.Regions -> resourceReference(R.string.tangempay_residency_all_state)
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
            is Params.Countries -> resourceReference(R.string.tangempay_order_data_country)
            is Params.Regions -> resourceReference(R.string.tangempay_withdraw_bank_geo_state_title)
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

    private sealed interface Step {

        data object Countries : Step

        data class Regions(
            val country: TangemPayCountry?,
            val countryCode: String,
            val selectedRegionCode: String?,
        ) : Step
    }
}