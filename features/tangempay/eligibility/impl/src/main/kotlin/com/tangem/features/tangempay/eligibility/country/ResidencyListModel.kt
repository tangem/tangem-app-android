package com.tangem.features.tangempay.eligibility.country

import androidx.compose.runtime.Stable
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.ui.R
import com.tangem.core.ui.ds2.search.TangemSearch
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.message.DialogMessage
import com.tangem.core.ui.message.ToastMessage
import com.tangem.features.tangempay.eligibility.EligibilityInnerRoute
import com.tangem.features.tangempay.eligibility.country.ResidencyListUM.Content
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.logging.TangemLogger
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@Stable
@ModelScoped
internal class ResidencyListModel @Inject constructor(
    override val dispatchers: CoroutineDispatcherProvider,
    private val router: Router,
    private val residencySource: MockResidencySource,
    private val uiMessageSender: UiMessageSender,
) : Model() {

    val uiState: StateFlow<ResidencyListUM>
        field = MutableStateFlow(getInitialState())

    private var step: Step = Step.Countries
    private var countries: List<CountryOfResidence> = emptyList()
    private var suggestedItems: List<ResidencyListUM.ItemUM> = emptyList()
    private var restItems: List<ResidencyListUM.ItemUM> = emptyList()
    private var checkingItemId: String? = null

    init {
        showCountries()
    }

    private fun showCountries() {
        step = Step.Countries
        uiState.update { state ->
            state.copy(
                title = resourceReference(R.string.tangempay_residency_title_country),
                search = state.search.copy(query = "", isActive = false),
                content = Content.Loading,
                onBackClick = null,
            )
        }
        modelScope.launch {
            if (countries.isEmpty()) {
                countries = residencySource.getCountries()
            }
            val suggestedCodes = residencySource.getSuggestedCountryCodes()
            suggestedItems = countries.filter { country -> country.code in suggestedCodes }.map(::toItem)
            restItems = countries.map(::toItem)
            updateContent()
        }
    }

    private fun showStates(country: CountryOfResidence) {
        step = Step.States
        uiState.update { state ->
            state.copy(
                title = resourceReference(R.string.tangempay_residency_title_state),
                search = state.search.copy(query = "", isActive = false),
                content = Content.Loading,
                onBackClick = ::showCountries,
            )
        }
        modelScope.launch {
            val states = residencySource.getStates(country.code)
            val suggestedCodes = residencySource.getSuggestedStateCodes(country.code)

            suggestedItems = states
                .filter { state -> state.code in suggestedCodes }
                .map { state -> toItem(country, state) }

            restItems = states.map { state -> toItem(country, state) }
            updateContent()
        }
    }

    private fun onCountryClick(country: CountryOfResidence) {
        checkEligibility(
            itemId = country.code,
            countryCode = country.code,
            stateCode = null,
        ) { isEligible ->
            when {
                country.hasStates -> showStates(country)
                isEligible -> openKycScreen()
                else -> openEligibilityDeniedScreen(countryCode = country.code, regionCode = null)
            }
        }
    }

    private fun onStateClick(country: CountryOfResidence, state: StateOfResidence) {
        checkEligibility(
            itemId = state.code,
            countryCode = country.code,
            stateCode = state.code,
        ) { isEligible ->
            if (isEligible) {
                openKycScreen()
            } else {
                openEligibilityDeniedScreen(countryCode = country.code, regionCode = state.code)
            }
        }
    }

    private fun checkEligibility(
        itemId: String,
        countryCode: String,
        stateCode: String?,
        onEligible: (isEligible: Boolean) -> Unit,
    ) {
        TangemLogger.i("ResidencyListModel: checkEligibility $countryCode $stateCode")
        if (checkingItemId != null) return
        checkingItemId = itemId
        updateContent()
        modelScope.launch {
            val result = residencySource.checkEligibility()

            checkingItemId = null

            result
                .onLeft {
                    uiMessageSender.send(
                        DialogMessage(
                            title = resourceReference(R.string.common_something_went_wrong),
                            message = resourceReference(R.string.common_try_again_later),
                        ),
                    )
                    updateContent()
                }
                .onRight { isEligible ->
                    onEligible(isEligible)
                }
        }
    }

    // TODO v_rodionov #[REDACTED_TASK_KEY]
    private fun openKycScreen() {
        updateContent()
        uiMessageSender.send(ToastMessage(stringReference("Open KYC")))
    }

    private fun openEligibilityDeniedScreen(countryCode: String, regionCode: String?) {
        updateContent()
        router.push(
            EligibilityInnerRoute.RegionUnavailable(countryCode = countryCode, regionCode = regionCode),
        )
    }

    private fun onQueryChange(query: String) {
        if (checkingItemId != null) return
        uiState.update { state -> state.copy(search = state.search.copy(query = query)) }
        updateContent()
    }

    private fun onActiveChange(isActive: Boolean) {
        uiState.update { state -> state.copy(search = state.search.copy(isActive = isActive)) }
    }

    private fun updateContent() {
        uiState.update { state -> state.copy(content = buildContent(query = state.search.query)) }
    }

    private fun buildContent(query: String): Content {
        if (restItems.isEmpty()) return Content.Loading

        if (query.isNotBlank()) {
            val found = restItems.filter { item -> item.name.contains(query.trim(), ignoreCase = true) }
            return if (found.isEmpty()) {
                Content.Empty
            } else {
                Content.Sections(
                    sections = persistentListOf(
                        ResidencyListUM.SectionUM(
                            id = SEARCH_SECTION_ID,
                            title = null,
                            items = found.map(::withLoadingState).toImmutableList(),
                        ),
                    ),
                )
            }
        }

        val sections = buildList {
            if (suggestedItems.isNotEmpty()) {
                add(
                    ResidencyListUM.SectionUM(
                        id = SUGGESTED_SECTION_ID,
                        title = suggestedSectionTitle(),
                        items = suggestedItems.map(::withLoadingState).toImmutableList(),
                    ),
                )
            }
            add(
                ResidencyListUM.SectionUM(
                    id = ALL_SECTION_ID,
                    title = allSectionTitle(),
                    items = restItems.map(::withLoadingState).toImmutableList(),
                ),
            )
        }

        return Content.Sections(sections = sections.toImmutableList())
    }

    private fun withLoadingState(item: ResidencyListUM.ItemUM): ResidencyListUM.ItemUM {
        return item.copy(isLoading = item.id == checkingItemId)
    }

    private fun toItem(country: CountryOfResidence): ResidencyListUM.ItemUM {
        return ResidencyListUM.ItemUM(
            id = country.code,
            name = country.name,
            flagUrl = country.flagUrl,
            isLoading = false,
            onClick = { onCountryClick(country) },
        )
    }

    private fun toItem(country: CountryOfResidence, state: StateOfResidence): ResidencyListUM.ItemUM {
        return ResidencyListUM.ItemUM(
            id = state.code,
            name = state.name,
            flagUrl = null,
            isLoading = false,
            onClick = { onStateClick(country, state) },
        )
    }

    private fun getInitialState(): ResidencyListUM {
        return ResidencyListUM(
            title = resourceReference(R.string.tangempay_residency_title_country),
            search = TangemSearch.State(
                placeholderText = resourceReference(R.string.common_search),
                query = "",
                onQueryChange = ::onQueryChange,
                isActive = false,
                onActiveChange = ::onActiveChange,
                onCloseClick = null,
            ),
            content = Content.Loading,
            onBackClick = null,
            onCloseClick = router::pop,
        )
    }

    private fun suggestedSectionTitle(): TextReference = when (step) {
        Step.Countries -> resourceReference(R.string.tangempay_residency_suggested_country)
        Step.States -> resourceReference(R.string.tangempay_residency_suggested_state)
    }

    private fun allSectionTitle(): TextReference = when (step) {
        Step.Countries -> resourceReference(R.string.tangempay_residency_all_country)
        Step.States -> resourceReference(R.string.tangempay_residency_all_state)
    }

    private enum class Step { Countries, States }

    private companion object {
        const val SUGGESTED_SECTION_ID = "suggested"
        const val ALL_SECTION_ID = "all"
        const val SEARCH_SECTION_ID = "search"
    }
}