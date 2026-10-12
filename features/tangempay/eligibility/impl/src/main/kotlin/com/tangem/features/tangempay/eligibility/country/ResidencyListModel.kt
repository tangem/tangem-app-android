package com.tangem.features.tangempay.eligibility.country

import androidx.compose.runtime.Stable
import com.arkivanov.decompose.router.slot.SlotNavigation
import com.arkivanov.decompose.router.slot.activate
import com.arkivanov.decompose.router.slot.dismiss
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.ui.R
import com.tangem.core.ui.ds2.search.TangemSearch
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.message.DialogMessage
import com.tangem.domain.models.pay.TangemPayCountry
import com.tangem.domain.models.pay.TangemPayEligibility
import com.tangem.domain.models.pay.TangemPayRegion
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.usecase.CheckTangemPayEligibilityUseCase
import com.tangem.domain.pay.usecase.GetTangemPayCountriesUseCase
import com.tangem.domain.pay.usecase.GetTangemPayRegionsUseCase
import com.tangem.domain.tangempay.TangemPayAnalyticsEvents
import com.tangem.features.tangempay.eligibility.EligibilityInnerRoute
import com.tangem.features.tangempay.eligibility.TangemPayEligibilityComponent
import com.tangem.features.tangempay.eligibility.onboarding.PaymentAccountOnboardingLauncher
import com.tangem.features.tangempay.eligibility.TangemPayWalletSelectorComponent
import com.tangem.features.tangempay.eligibility.country.ResidencyListUM.Content
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@Suppress("LongParameterList")
@Stable
@ModelScoped
internal class ResidencyListModel @Inject constructor(
    override val dispatchers: CoroutineDispatcherProvider,
    private val router: Router,
    private val getCountriesUseCase: GetTangemPayCountriesUseCase,
    private val getRegionsUseCase: GetTangemPayRegionsUseCase,
    private val checkEligibilityUseCase: CheckTangemPayEligibilityUseCase,
    private val onboardingLauncher: PaymentAccountOnboardingLauncher,
    private val uiMessageSender: UiMessageSender,
    private val analytics: AnalyticsEventHandler,
    paramsContainer: ParamsContainer,
) : Model(), TangemPayWalletSelectorComponent.Listener {

    private val params = paramsContainer.require<TangemPayEligibilityComponent.Params>()

    val bottomSheetNavigation: SlotNavigation<ResidencyBottomSheetConfig> = SlotNavigation()

    val uiState: StateFlow<ResidencyListUM>
        field = MutableStateFlow(getInitialState())

    private var step: Step = Step.Countries
    private var data: ResidencyListData = ResidencyListData.Loading
    private var countries: List<TangemPayCountry> = emptyList()
    private var checkingItemId: String? = null
    private var continuingItemId: String? = null

    init {
        analytics.send(TangemPayAnalyticsEvents.Eligibility.CountrySelectScreenOpened())
        showCountries()
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
        loadCountries()
    }

    private fun loadCountries() {
        setLoading()
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

    private fun showRegions(country: TangemPayCountry) {
        analytics.send(TangemPayAnalyticsEvents.Eligibility.StateScreenOpened())
        step = Step.Regions(country)
        uiState.update { state ->
            state.copy(
                title = resourceReference(R.string.tangempay_residency_title_state),
                search = state.search.copy(query = "", isActive = false),
                onBackClick = ::showCountries,
            )
        }
        loadRegions(country)
    }

    private fun loadRegions(country: TangemPayCountry) {
        setLoading()
        modelScope.launch {
            getRegionsUseCase(country.code)
                .onLeft { setFailed(onRetry = { loadRegions(country) }) }
                .onRight { setRegionsLoaded(country, it) }
        }
    }

    private fun onCountryClick(country: TangemPayCountry, fromSuggested: Boolean) {
        analytics.send(
            TangemPayAnalyticsEvents.Eligibility.CountrySelected(
                country = country.name,
                isSuggested = fromSuggested,
            ),
        )
        checkEligibility(itemId = country.code, countryCode = country.code, regionCode = null) { eligibility ->
            sendEligibilityCheckResult(eligibility = eligibility, country = country, region = null)
            when {
                !eligibility.isAvailable -> openRegionUnavailableScreen(eligibility)
                country.hasRegions -> showRegions(country)
                else -> continueOnboarding(itemId = country.code)
            }
        }
    }

    private fun onRegionClick(country: TangemPayCountry, region: TangemPayRegion, fromSuggested: Boolean) {
        analytics.send(
            TangemPayAnalyticsEvents.Eligibility.StateSelected(
                state = region.name,
                isSuggested = fromSuggested,
            ),
        )
        checkEligibility(itemId = region.code, countryCode = country.code, regionCode = region.code) { eligibility ->
            sendEligibilityCheckResult(eligibility = eligibility, country = country, region = region)
            if (eligibility.isAvailable) {
                continueOnboarding(itemId = region.code)
            } else {
                openRegionUnavailableScreen(eligibility)
            }
        }
    }

    private fun sendEligibilityCheckResult(
        eligibility: TangemPayEligibility,
        country: TangemPayCountry,
        region: TangemPayRegion?,
    ) {
        analytics.send(
            TangemPayAnalyticsEvents.Eligibility.EligibilityCheckResult(
                isEligible = eligibility.isAvailable,
                country = country.name,
                state = region?.name,
            ),
        )
    }

    private fun checkEligibility(
        itemId: String,
        countryCode: String,
        regionCode: String?,
        onChecked: suspend (TangemPayEligibility) -> Unit,
    ) {
        if (isBusy()) return
        checkingItemId = itemId
        updateContent()
        modelScope.launch {
            val result = checkEligibilityUseCase(
                product = params.product,
                countryCode = countryCode,
                regionCode = regionCode,
            )

            checkingItemId = null

            result.fold(
                ifLeft = {
                    uiMessageSender.send(
                        DialogMessage(
                            title = resourceReference(R.string.common_something_went_wrong),
                            message = resourceReference(R.string.common_try_again_later),
                        ),
                    )
                    updateContent()
                },
                ifRight = { eligibility -> onChecked(eligibility) },
            )
        }
    }

    private suspend fun continueOnboarding(itemId: String) {
        continuingItemId = itemId
        updateContent()
        val userWalletId = params.userWalletId
        if (userWalletId != null) {
            onboardingLauncher.openKyc(userWalletId)
        } else {
            onboardingLauncher.start(
                onWalletSelectionRequired = ::openWalletSelector,
                onFailed = ::releaseContinuing,
            )
        }
    }

    private fun openWalletSelector(walletsIds: List<UserWalletId>) {
        bottomSheetNavigation.activate(ResidencyBottomSheetConfig.WalletSelector(walletsIds = walletsIds))
    }

    override fun onWalletSelected(userWalletId: UserWalletId) {
        bottomSheetNavigation.dismiss()
        modelScope.launch {
            onboardingLauncher.continueWith(userWalletId = userWalletId, onFailed = ::releaseContinuing)
        }
    }

    override fun onWalletSelectorDismiss() {
        bottomSheetNavigation.dismiss()
        releaseContinuing()
    }

    private fun releaseContinuing() {
        continuingItemId = null
        updateContent()
    }

    private fun isBusy(): Boolean = checkingItemId != null || continuingItemId != null

    private fun openRegionUnavailableScreen(eligibility: TangemPayEligibility) {
        analytics.send(TangemPayAnalyticsEvents.Eligibility.RegionNotAvailableScreenOpened())
        updateContent()
        router.push(
            EligibilityInnerRoute.RegionUnavailable(
                countryCode = eligibility.country,
                regionCode = eligibility.region,
            ),
        )
    }

    private fun onQueryChange(query: String) {
        if (isBusy()) return
        uiState.update { state -> state.copy(search = state.search.copy(query = query)) }
        updateContent()
    }

    private fun onActiveChange(isActive: Boolean) {
        uiState.update { state -> state.copy(search = state.search.copy(isActive = isActive)) }
    }

    private fun setLoading() {
        data = ResidencyListData.Loading
        updateContent()
    }

    private fun setCountriesLoaded() {
        data = ResidencyListData.Loaded(
            suggested = countries.filter(TangemPayCountry::isSuggested).map { country ->
                toItem(country = country, fromSuggested = true)
            },
            all = countries.map { country -> toItem(country = country, fromSuggested = false) },
        )
        updateContent()
    }

    private fun setRegionsLoaded(country: TangemPayCountry, regions: List<TangemPayRegion>) {
        data = ResidencyListData.Loaded(
            suggested = regions.filter(TangemPayRegion::isSuggested).map { region ->
                toItem(country = country, region = region, fromSuggested = true)
            },
            all = regions.map { region -> toItem(country = country, region = region, fromSuggested = false) },
        )
        updateContent()
    }

    private fun setFailed(onRetry: () -> Unit) {
        data = ResidencyListData.Failed(onRetry = onRetry)
        updateContent()
    }

    private fun updateContent() {
        uiState.update { state ->
            state.copy(
                content = data.withLoadingState().toContent(
                    query = state.search.query,
                    suggestedTitle = suggestedSectionTitle(),
                    allTitle = allSectionTitle(),
                ),
            )
        }
    }

    private fun ResidencyListData.withLoadingState(): ResidencyListData = when (this) {
        is ResidencyListData.Loaded -> copy(
            suggested = suggested.map(::withLoadingState),
            all = all.map(::withLoadingState),
        )
        else -> this
    }

    private fun withLoadingState(item: ResidencyListUM.ItemUM): ResidencyListUM.ItemUM {
        return item.copy(isLoading = item.id == checkingItemId || item.id == continuingItemId)
    }

    private fun toItem(country: TangemPayCountry, fromSuggested: Boolean): ResidencyListUM.ItemUM {
        return ResidencyListUM.ItemUM(
            id = country.code,
            name = country.name,
            flagUrl = country.flagUrl,
            isLoading = false,
            onClick = { onCountryClick(country = country, fromSuggested = fromSuggested) },
        )
    }

    private fun toItem(
        country: TangemPayCountry,
        region: TangemPayRegion,
        fromSuggested: Boolean,
    ): ResidencyListUM.ItemUM {
        return ResidencyListUM.ItemUM(
            id = region.code,
            name = region.name,
            flagUrl = null,
            isLoading = false,
            onClick = { onRegionClick(country = country, region = region, fromSuggested = fromSuggested) },
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
            content = ResidencyListUM.Content.Loading,
            onBackClick = null,
            onCloseClick = router::pop,
        )
    }

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
        data class Regions(val country: TangemPayCountry) : Step
    }
}