package com.tangem.tests.tangempay

import com.tangem.common.BaseTestCase
import com.tangem.common.constants.TestConstants.SVS_SEED_PHRASE_12
import com.tangem.common.constants.TestConstants.TANGEM_PAY_ACCESS_CODE
import com.tangem.common.constants.TestConstants.TANGEM_PAY_ELIGIBILITY_CHANNELS_SCENARIO
import com.tangem.common.constants.TestConstants.TANGEM_PAY_ELIGIBILITY_SCENARIO
import com.tangem.common.constants.TestConstants.TANGEM_PAY_GEO_COUNTRIES_SCENARIO
import com.tangem.common.constants.TestConstants.TANGEM_PAY_KYC_STATUS_SCENARIO
import com.tangem.common.constants.TestConstants.WAIT_UNTIL_TIMEOUT_LONG
import com.tangem.common.extensions.clickWithAssertion
import com.tangem.common.utils.setWireMockScenarioState
import com.tangem.core.configtoggle.FeatureToggles
import com.tangem.domain.pay.TangemPayEligibilityManager
import com.tangem.scenarios.assertTangemPayTileNotDisplayed
import com.tangem.scenarios.awaitNoWalletOfferedTangemPay
import com.tangem.scenarios.awaitTangemPayRequestCountIncreased
import com.tangem.scenarios.clickGetCardOnTangemPayOnboarding
import com.tangem.scenarios.clickTangemPayResidencyItem
import com.tangem.scenarios.exitTangemPayKycScreen
import com.tangem.scenarios.getTangemPayCountriesRequestCount
import com.tangem.scenarios.getTangemPayEligibilityCheckRequestCount
import com.tangem.scenarios.getTangemPayRegionsRequestCount
import com.tangem.scenarios.getTangemPayRemovalRequestCount
import com.tangem.scenarios.openMainScreenWithExistingHotWallet
import com.tangem.scenarios.openTangemPayCountryStepFromDetails
import com.tangem.scenarios.searchTangemPayResidency
import com.tangem.scenarios.selectTangemPayResidency
import com.tangem.screens.onDetailsScreen
import com.tangem.screens.onDialog
import com.tangem.screens.onMainScreen
import com.tangem.screens.onMainScreenTopBar
import com.tangem.screens.tangempay.onTangemPayKycScreen
import com.tangem.screens.tangempay.onTangemPayKycSheet
import com.tangem.screens.tangempay.onTangemPayMainScreen
import com.tangem.screens.tangempay.onTangemPayOnboardingScreen
import com.tangem.screens.tangempay.onTangemPayResidencyScreen
import com.tangem.core.res.R as CoreResR
import com.tangem.utils.annotations.RemoveWithToggle
import dagger.hilt.android.testing.HiltAndroidTest
import io.github.kakaocup.kakao.common.utilities.getResourceString
import io.qameta.allure.kotlin.AllureId
import io.qameta.allure.kotlin.junit4.DisplayName
import org.junit.Test
import javax.inject.Inject

@HiltAndroidTest
class TangemPayChooseCountryTest : BaseTestCase() {

    @Inject
    lateinit var tangemPayEligibilityManager: TangemPayEligibilityManager

    private val unitedStates = "United States"
    private val alabama = "Alabama"
    private val california = "California"
    private val germany = "Germany"
    private val unitedStatesCode = "US"

    @RemoveWithToggle(
        toggleName = "TWI_1841_HIDE_MARKETING_BANNERS_WHILE_SYSTEM_ENABLED",
        description = "Move the tests to a wallet without system cards, where the Main promo is shown",
    )
    override val featureToggleOverrides = mapOf(
        FeatureToggles.TWI_1841_HIDE_MARKETING_BANNERS_WHILE_SYSTEM_ENABLED.rawName to false,
    )

    @AllureId("12752")
    @DisplayName("Tangem Pay: Get Tangem Pay in Details is hidden after KYC starts and is back once the account is hidden")
    @Test
    fun getTangemPayInDetailsFollowsPaymentAccountTest() {
        val eligibilityStartedState = "Started"
        val eligibilityPaeraCustomerState = "PaeraCustomer"
        val kycInProgressState = "InProgress"
        val kycInProgressText = getResourceString(CoreResR.string.tangempay_kyc_in_progress)
        val hideVerificationText = getResourceString(CoreResR.string.tangempay_cancel_kyc)

        setupHooks().run {
            step("Set WireMock scenario '$TANGEM_PAY_ELIGIBILITY_SCENARIO' to '$eligibilityStartedState'") {
                setWireMockScenarioState(TANGEM_PAY_ELIGIBILITY_SCENARIO, eligibilityStartedState)
            }
            step("Set WireMock scenario '$TANGEM_PAY_KYC_STATUS_SCENARIO' to '$kycInProgressState'") {
                setWireMockScenarioState(TANGEM_PAY_KYC_STATUS_SCENARIO, kycInProgressState)
            }
            step("Open 'Main' screen with existing hot wallet") {
                openMainScreenWithExistingHotWallet(SVS_SEED_PHRASE_12, accessCode = TANGEM_PAY_ACCESS_CODE)
            }
            step("Open 'Country of residence' screen from app Details") { openTangemPayCountryStepFromDetails() }
            step("Set WireMock scenario '$TANGEM_PAY_ELIGIBILITY_SCENARIO' to '$eligibilityPaeraCustomerState'") {
                setWireMockScenarioState(TANGEM_PAY_ELIGIBILITY_SCENARIO, eligibilityPaeraCustomerState)
            }
            step("Select residency: '$unitedStates', '$california'") {
                selectTangemPayResidency(country = unitedStates, region = california)
            }
            step("Leave the KYC screen") { exitTangemPayKycScreen() }
            step("Wait until no wallet is offered Tangem Pay") { awaitNoWalletOfferedTangemPay(tangemPayEligibilityManager) }
            step("Click on 'More' button") {
                onMainScreenTopBar { moreButton.clickWithAssertion() }
            }
            step("Assert app Details screen is displayed") {
                flakySafely { onDetailsScreen { appSettingsButton.assertIsDisplayed() } }
            }
            step("Assert 'Get Tangem Pay' row is not displayed") {
                onDetailsScreen { getTangemPayRow.assertDoesNotExist() }
            }
            step("Click on 'Back' button") {
                onDetailsScreen { topAppBarBackButton.clickWithAssertion() }
            }
            step("Assert Tangem Pay tile with '$kycInProgressText' is displayed") {
                flakySafely(WAIT_UNTIL_TIMEOUT_LONG) {
                    onTangemPayMainScreen { tileWithSubtitle(kycInProgressText).assertIsDisplayed() }
                }
            }
            step("Click on Tangem Pay tile") {
                onTangemPayMainScreen { mainScreenTile.clickWithAssertion() }
            }
            step("Assert KYC in progress sheet is displayed") {
                flakySafely { onTangemPayKycSheet { secondaryButtonWithText(hideVerificationText).assertIsDisplayed() } }
            }
            step("Click on '$hideVerificationText' button") {
                onTangemPayKycSheet { secondaryButtonWithText(hideVerificationText).performClick() }
            }
            val removalRequestsBefore = getTangemPayRemovalRequestCount()
            step("Assert hide verification dialog is displayed") {
                flakySafely { onDialog { confirmButton.assertIsDisplayed() } }
            }
            step("Click on 'Confirm' button") {
                onDialog { confirmButton.performClick() }
            }
            step("Assert Tangem Pay is disabled for the wallet") {
                awaitTangemPayRequestCountIncreased("customer/pay-enabled", removalRequestsBefore) {
                    getTangemPayRemovalRequestCount()
                }
            }
            step("Assert Tangem Pay tile is not displayed") { assertTangemPayTileNotDisplayed() }
            step("Click on 'More' button") {
                onMainScreenTopBar { moreButton.clickWithAssertion() }
            }
            step("Assert 'Get Tangem Pay' row is displayed") {
                flakySafely { onDetailsScreen { getTangemPayRow.assertIsDisplayed() } }
            }
        }
    }

    @AllureId("12753")
    @DisplayName("Tangem Pay: closing the country step started from Get Tangem Pay returns to app Details")
    @Test
    fun closingCountryStepReturnsToDetailsTest() {
        val eligibilityStartedState = "Started"
        val kycRequiredState = "Required"

        setupHooks().run {
            step("Set WireMock scenario '$TANGEM_PAY_ELIGIBILITY_SCENARIO' to '$eligibilityStartedState'") {
                setWireMockScenarioState(TANGEM_PAY_ELIGIBILITY_SCENARIO, eligibilityStartedState)
            }
            step("Set WireMock scenario '$TANGEM_PAY_KYC_STATUS_SCENARIO' to '$kycRequiredState'") {
                setWireMockScenarioState(TANGEM_PAY_KYC_STATUS_SCENARIO, kycRequiredState)
            }
            step("Open 'Main' screen with existing hot wallet") {
                openMainScreenWithExistingHotWallet(SVS_SEED_PHRASE_12, accessCode = TANGEM_PAY_ACCESS_CODE)
            }
            step("Open 'Country of residence' screen from app Details") { openTangemPayCountryStepFromDetails() }
            step("Click on 'Close' button") {
                onTangemPayResidencyScreen { closeButton.clickWithAssertion() }
            }
            step("Assert app Details screen is displayed") {
                flakySafely { onDetailsScreen { appSettingsButton.assertIsDisplayed() } }
            }
        }
    }

    @AllureId("12754")
    @DisplayName("Tangem Pay: closing the country step started from the banner leaves 'Action required' on Main")
    @Test
    fun closingCountryStepFromBannerShowsActionRequiredTest() {
        val eligibilityStartedState = "Started"
        val eligibilityPaeraCustomerState = "PaeraCustomer"
        val channelsBannerState = "Banner"
        val kycRequiredState = "Required"
        val actionRequiredText = getResourceString(CoreResR.string.tangempay_action_required_title)

        setupHooks().run {
            step("Set WireMock scenario '$TANGEM_PAY_ELIGIBILITY_SCENARIO' to '$eligibilityStartedState'") {
                setWireMockScenarioState(TANGEM_PAY_ELIGIBILITY_SCENARIO, eligibilityStartedState)
            }
            step("Set WireMock scenario '$TANGEM_PAY_ELIGIBILITY_CHANNELS_SCENARIO' to '$channelsBannerState'") {
                setWireMockScenarioState(TANGEM_PAY_ELIGIBILITY_CHANNELS_SCENARIO, channelsBannerState)
            }
            step("Set WireMock scenario '$TANGEM_PAY_KYC_STATUS_SCENARIO' to '$kycRequiredState'") {
                setWireMockScenarioState(TANGEM_PAY_KYC_STATUS_SCENARIO, kycRequiredState)
            }
            step("Open 'Main' screen with existing hot wallet") {
                openMainScreenWithExistingHotWallet(SVS_SEED_PHRASE_12, accessCode = TANGEM_PAY_ACCESS_CODE)
            }
            step("Assert 'Get Tangem Pay' banner is displayed on Main") {
                flakySafely(WAIT_UNTIL_TIMEOUT_LONG) { onMainScreen { getTangemPayBanner.assertIsDisplayed() } }
            }
            step("Click on 'Learn more' button on 'Get Tangem Pay' banner") {
                onMainScreen { getTangemPayBannerLearnMoreButton.clickWithAssertion() }
            }
            step("Assert Tangem Pay onboarding screen is displayed") {
                flakySafely { onTangemPayOnboardingScreen { title.assertIsDisplayed() } }
            }
            step("Click on 'Get card' button") { clickGetCardOnTangemPayOnboarding() }
            step("Assert 'Country of residence' screen is displayed") {
                flakySafely(WAIT_UNTIL_TIMEOUT_LONG) { onTangemPayResidencyScreen { countryTitle.assertIsDisplayed() } }
            }
            step("Set WireMock scenario '$TANGEM_PAY_ELIGIBILITY_SCENARIO' to '$eligibilityPaeraCustomerState'") {
                setWireMockScenarioState(TANGEM_PAY_ELIGIBILITY_SCENARIO, eligibilityPaeraCustomerState)
            }
            step("Click on 'Close' button") {
                onTangemPayResidencyScreen { closeButton.clickWithAssertion() }
            }
            step("Assert Tangem Pay tile with '$actionRequiredText' is displayed") {
                flakySafely(WAIT_UNTIL_TIMEOUT_LONG) {
                    onTangemPayMainScreen { tileWithSubtitle(actionRequiredText).assertIsDisplayed() }
                }
            }
            step("Assert 'Get Tangem Pay' banner is not displayed") {
                onMainScreen { getTangemPayBanner.assertDoesNotExist() }
            }
            step("Click on Tangem Pay tile") {
                onTangemPayMainScreen { mainScreenTile.clickWithAssertion() }
            }
            step("Assert 'Country of residence' screen is displayed") {
                flakySafely { onTangemPayResidencyScreen { countryTitle.assertIsDisplayed() } }
            }
        }
    }

    @AllureId("12757")
    @DisplayName("Tangem Pay: countries and US states are split into suggested and full lists")
    @Test
    fun countriesAndStatesSplitIntoSuggestedAndFullListsTest() {
        val eligibilityStartedState = "Started"
        val kycRequiredState = "Required"
        val geoFullListState = "FullList"

        setupHooks().run {
            step("Set WireMock scenario '$TANGEM_PAY_ELIGIBILITY_SCENARIO' to '$eligibilityStartedState'") {
                setWireMockScenarioState(TANGEM_PAY_ELIGIBILITY_SCENARIO, eligibilityStartedState)
            }
            step("Set WireMock scenario '$TANGEM_PAY_KYC_STATUS_SCENARIO' to '$kycRequiredState'") {
                setWireMockScenarioState(TANGEM_PAY_KYC_STATUS_SCENARIO, kycRequiredState)
            }
            step("Set WireMock scenario '$TANGEM_PAY_GEO_COUNTRIES_SCENARIO' to '$geoFullListState'") {
                setWireMockScenarioState(TANGEM_PAY_GEO_COUNTRIES_SCENARIO, geoFullListState)
            }
            step("Open 'Main' screen with existing hot wallet") {
                openMainScreenWithExistingHotWallet(SVS_SEED_PHRASE_12, accessCode = TANGEM_PAY_ACCESS_CODE)
            }
            val countriesRequestsBefore = getTangemPayCountriesRequestCount()
            step("Open 'Country of residence' screen from app Details") { openTangemPayCountryStepFromDetails() }
            step("Assert countries are requested") {
                awaitTangemPayRequestCountIncreased("geo/countries", countriesRequestsBefore) {
                    getTangemPayCountriesRequestCount()
                }
            }
            step("Assert 'Suggested country' section is displayed") {
                flakySafely { onTangemPayResidencyScreen { suggestedCountryHeader.assertIsDisplayed() } }
            }
            step("Assert 'Change country' section is displayed") {
                onTangemPayResidencyScreen { allCountriesHeader.assertIsDisplayed() }
            }
            step("Assert '$unitedStates' is listed in both sections") {
                onTangemPayResidencyScreen {
                    row(unitedStates, index = 0).assertIsDisplayed()
                    row(unitedStates, index = 1).assertIsDisplayed()
                }
            }
            step("Assert '$germany' is listed only in 'Change country' section") {
                onTangemPayResidencyScreen {
                    row(germany, index = 0).assertIsDisplayed()
                    row(germany, index = 1).assertDoesNotExist()
                }
            }
            val regionsRequestsBefore = getTangemPayRegionsRequestCount(unitedStatesCode)
            step("Click on country: '$unitedStates'") { clickTangemPayResidencyItem(unitedStates) }
            step("Assert US states are requested") {
                awaitTangemPayRequestCountIncreased("geo/countries/US/regions", regionsRequestsBefore) {
                    getTangemPayRegionsRequestCount(unitedStatesCode)
                }
            }
            step("Assert 'Select state' screen is displayed") {
                flakySafely { onTangemPayResidencyScreen { regionTitle.assertIsDisplayed() } }
            }
            step("Assert 'Suggested state' section is displayed") {
                flakySafely { onTangemPayResidencyScreen { suggestedStateHeader.assertIsDisplayed() } }
            }
            step("Assert 'Select another state' section is displayed") {
                onTangemPayResidencyScreen { allStatesHeader.assertIsDisplayed() }
            }
            step("Assert '$california' is listed in both sections") {
                onTangemPayResidencyScreen {
                    row(california, index = 0).assertIsDisplayed()
                    row(california, index = 1).assertIsDisplayed()
                }
            }
            step("Assert '$alabama' is listed only in 'Select another state' section") {
                onTangemPayResidencyScreen {
                    row(alabama, index = 0).assertIsDisplayed()
                    row(alabama, index = 1).assertDoesNotExist()
                }
            }
        }
    }

    @AllureId("12758")
    @DisplayName("Tangem Pay: Suggested sections are hidden when no country or state is suggested")
    @Test
    fun suggestedSectionsHiddenWithoutSuggestedItemsTest() {
        val eligibilityStartedState = "Started"
        val kycRequiredState = "Required"
        val geoNoSuggestedState = "NoSuggested"

        setupHooks().run {
            step("Set WireMock scenario '$TANGEM_PAY_ELIGIBILITY_SCENARIO' to '$eligibilityStartedState'") {
                setWireMockScenarioState(TANGEM_PAY_ELIGIBILITY_SCENARIO, eligibilityStartedState)
            }
            step("Set WireMock scenario '$TANGEM_PAY_KYC_STATUS_SCENARIO' to '$kycRequiredState'") {
                setWireMockScenarioState(TANGEM_PAY_KYC_STATUS_SCENARIO, kycRequiredState)
            }
            step("Set WireMock scenario '$TANGEM_PAY_GEO_COUNTRIES_SCENARIO' to '$geoNoSuggestedState'") {
                setWireMockScenarioState(TANGEM_PAY_GEO_COUNTRIES_SCENARIO, geoNoSuggestedState)
            }
            step("Open 'Main' screen with existing hot wallet") {
                openMainScreenWithExistingHotWallet(SVS_SEED_PHRASE_12, accessCode = TANGEM_PAY_ACCESS_CODE)
            }
            val countriesRequestsBefore = getTangemPayCountriesRequestCount()
            step("Open 'Country of residence' screen from app Details") { openTangemPayCountryStepFromDetails() }
            step("Assert countries are requested") {
                awaitTangemPayRequestCountIncreased("geo/countries", countriesRequestsBefore) {
                    getTangemPayCountriesRequestCount()
                }
            }
            step("Assert '$unitedStates' is displayed") {
                flakySafely { onTangemPayResidencyScreen { row(unitedStates).assertIsDisplayed() } }
            }
            step("Assert 'Suggested country' section is not displayed") {
                onTangemPayResidencyScreen { suggestedCountryHeader.assertDoesNotExist() }
            }
            val regionsRequestsBefore = getTangemPayRegionsRequestCount(unitedStatesCode)
            step("Click on country: '$unitedStates'") { clickTangemPayResidencyItem(unitedStates) }
            step("Assert US states are requested") {
                awaitTangemPayRequestCountIncreased("geo/countries/US/regions", regionsRequestsBefore) {
                    getTangemPayRegionsRequestCount(unitedStatesCode)
                }
            }
            step("Assert 'Select state' screen is displayed") {
                flakySafely { onTangemPayResidencyScreen { regionTitle.assertIsDisplayed() } }
            }
            step("Assert '$alabama' is displayed") {
                flakySafely { onTangemPayResidencyScreen { row(alabama).assertIsDisplayed() } }
            }
            step("Assert 'Suggested state' section is not displayed") {
                onTangemPayResidencyScreen { suggestedStateHeader.assertDoesNotExist() }
            }
        }
    }

    @AllureId("12759")
    @DisplayName("Tangem Pay: country and state search shows matches, a zero state and the full list once cleared")
    @Test
    fun countryAndStateSearchTest() {
        val eligibilityStartedState = "Started"
        val kycRequiredState = "Required"
        val geoFullListState = "FullList"
        val countryQuery = "bu"
        val stateQuery = "ko"
        val unitedStatesQuery = "United"
        val noResultsQuery = "qwerty"
        val bulgaria = "Bulgaria"
        val burundi = "Burundi"
        val northDakota = "North Dakota"
        val southDakota = "South Dakota"

        setupHooks().run {
            step("Set WireMock scenario '$TANGEM_PAY_ELIGIBILITY_SCENARIO' to '$eligibilityStartedState'") {
                setWireMockScenarioState(TANGEM_PAY_ELIGIBILITY_SCENARIO, eligibilityStartedState)
            }
            step("Set WireMock scenario '$TANGEM_PAY_KYC_STATUS_SCENARIO' to '$kycRequiredState'") {
                setWireMockScenarioState(TANGEM_PAY_KYC_STATUS_SCENARIO, kycRequiredState)
            }
            step("Set WireMock scenario '$TANGEM_PAY_GEO_COUNTRIES_SCENARIO' to '$geoFullListState'") {
                setWireMockScenarioState(TANGEM_PAY_GEO_COUNTRIES_SCENARIO, geoFullListState)
            }
            step("Open 'Main' screen with existing hot wallet") {
                openMainScreenWithExistingHotWallet(SVS_SEED_PHRASE_12, accessCode = TANGEM_PAY_ACCESS_CODE)
            }
            step("Open 'Country of residence' screen from app Details") { openTangemPayCountryStepFromDetails() }
            step("Type '$countryQuery' into search field") { searchTangemPayResidency(countryQuery) }
            step("Assert '$bulgaria' and '$burundi' are displayed") {
                flakySafely {
                    onTangemPayResidencyScreen {
                        row(bulgaria).assertIsDisplayed()
                        row(burundi).assertIsDisplayed()
                    }
                }
            }
            step("Assert '$germany' is not displayed") {
                onTangemPayResidencyScreen { row(germany).assertDoesNotExist() }
            }
            step("Assert 'Suggested country' and 'Change country' sections are not displayed") {
                onTangemPayResidencyScreen {
                    suggestedCountryHeader.assertDoesNotExist()
                    allCountriesHeader.assertDoesNotExist()
                }
            }
            step("Type '$noResultsQuery' into search field") { searchTangemPayResidency(noResultsQuery) }
            step("Assert zero state is displayed") {
                flakySafely { onTangemPayResidencyScreen { emptyState.assertIsDisplayed() } }
            }
            step("Clear search field") {
                onTangemPayResidencyScreen { searchField.performTextClearance() }
            }
            step("Assert the full country list is displayed") {
                flakySafely {
                    onTangemPayResidencyScreen {
                        suggestedCountryHeader.assertIsDisplayed()
                        allCountriesHeader.assertIsDisplayed()
                        row(germany).assertIsDisplayed()
                    }
                }
            }
            step("Type '$unitedStatesQuery' into search field") { searchTangemPayResidency(unitedStatesQuery) }
            step("Click on country: '$unitedStates'") { clickTangemPayResidencyItem(unitedStates) }
            step("Assert 'Select state' screen is displayed") {
                flakySafely { onTangemPayResidencyScreen { regionTitle.assertIsDisplayed() } }
            }
            step("Type '$stateQuery' into search field") { searchTangemPayResidency(stateQuery) }
            step("Assert '$northDakota' and '$southDakota' are displayed") {
                flakySafely {
                    onTangemPayResidencyScreen {
                        row(northDakota).assertIsDisplayed()
                        row(southDakota).assertIsDisplayed()
                    }
                }
            }
            step("Assert '$alabama' is not displayed") {
                onTangemPayResidencyScreen { row(alabama).assertDoesNotExist() }
            }
            step("Assert 'Suggested state' and 'Select another state' sections are not displayed") {
                onTangemPayResidencyScreen {
                    suggestedStateHeader.assertDoesNotExist()
                    allStatesHeader.assertDoesNotExist()
                }
            }
            step("Type '$noResultsQuery' into search field") { searchTangemPayResidency(noResultsQuery) }
            step("Assert zero state is displayed") {
                flakySafely { onTangemPayResidencyScreen { emptyState.assertIsDisplayed() } }
            }
        }
    }

    @AllureId("12778")
    @DisplayName("Tangem Pay: an eligible US state is checked with eligibility/check and opens KYC")
    @Test
    fun eligibleStateCheckedAndOpensKycTest() {
        val eligibilityStartedState = "Started"
        val kycRequiredState = "Required"
        val geoFullListState = "FullList"
        val alabamaCode = "US-AL"

        setupHooks().run {
            step("Set WireMock scenario '$TANGEM_PAY_ELIGIBILITY_SCENARIO' to '$eligibilityStartedState'") {
                setWireMockScenarioState(TANGEM_PAY_ELIGIBILITY_SCENARIO, eligibilityStartedState)
            }
            step("Set WireMock scenario '$TANGEM_PAY_KYC_STATUS_SCENARIO' to '$kycRequiredState'") {
                setWireMockScenarioState(TANGEM_PAY_KYC_STATUS_SCENARIO, kycRequiredState)
            }
            step("Set WireMock scenario '$TANGEM_PAY_GEO_COUNTRIES_SCENARIO' to '$geoFullListState'") {
                setWireMockScenarioState(TANGEM_PAY_GEO_COUNTRIES_SCENARIO, geoFullListState)
            }
            step("Open 'Main' screen with existing hot wallet") {
                openMainScreenWithExistingHotWallet(SVS_SEED_PHRASE_12, accessCode = TANGEM_PAY_ACCESS_CODE)
            }
            val countriesRequestsBefore = getTangemPayCountriesRequestCount()
            step("Open 'Country of residence' screen from app Details") { openTangemPayCountryStepFromDetails() }
            step("Assert countries are requested") {
                awaitTangemPayRequestCountIncreased("geo/countries", countriesRequestsBefore) {
                    getTangemPayCountriesRequestCount()
                }
            }
            val regionsRequestsBefore = getTangemPayRegionsRequestCount(unitedStatesCode)
            step("Click on country: '$unitedStates'") { clickTangemPayResidencyItem(unitedStates) }
            step("Assert US states are requested") {
                awaitTangemPayRequestCountIncreased("geo/countries/US/regions", regionsRequestsBefore) {
                    getTangemPayRegionsRequestCount(unitedStatesCode)
                }
            }
            step("Assert 'Select state' screen is displayed") {
                flakySafely { onTangemPayResidencyScreen { regionTitle.assertIsDisplayed() } }
            }
            val eligibilityChecksBefore = getTangemPayEligibilityCheckRequestCount(alabamaCode)
            step("Click on state: '$alabama'") { clickTangemPayResidencyItem(alabama) }
            step("Assert eligibility is checked for '$alabamaCode'") {
                awaitTangemPayRequestCountIncreased("eligibility/check", eligibilityChecksBefore) {
                    getTangemPayEligibilityCheckRequestCount(alabamaCode)
                }
            }
            step("Assert KYC screen is displayed") {
                flakySafely(WAIT_UNTIL_TIMEOUT_LONG) { onTangemPayKycScreen { screenContainer.assertIsDisplayed() } }
            }
        }
    }
}