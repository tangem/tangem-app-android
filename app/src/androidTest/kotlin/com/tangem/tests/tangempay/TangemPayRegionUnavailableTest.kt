package com.tangem.tests.tangempay

import com.tangem.common.BaseTestCase
import com.tangem.common.constants.TestConstants.SVS_SEED_PHRASE_12
import com.tangem.common.constants.TestConstants.TANGEM_PAY_ACCESS_CODE
import com.tangem.common.constants.TestConstants.TANGEM_PAY_ELIGIBILITY_CHECK_SCENARIO
import com.tangem.common.constants.TestConstants.TANGEM_PAY_ELIGIBILITY_SCENARIO
import com.tangem.common.constants.TestConstants.TANGEM_PAY_GEO_COUNTRIES_SCENARIO
import com.tangem.common.constants.TestConstants.TANGEM_PAY_KYC_STATUS_SCENARIO
import com.tangem.common.constants.TestConstants.WAIT_UNTIL_TIMEOUT_LONG
import com.tangem.common.extensions.clickWithAssertion
import com.tangem.common.utils.assertUrlOpenedStartingWith
import com.tangem.common.utils.setWireMockScenarioState
import com.tangem.common.utils.stubExternalUrlOpening
import com.tangem.scenarios.assertTangemPayTileNotDisplayed
import com.tangem.scenarios.assertTangemPayWaitlistJoined
import com.tangem.scenarios.awaitTangemPayRequestCountIncreased
import com.tangem.scenarios.awaitTangemPayWaitlistEmailFieldFocused
import com.tangem.scenarios.awaitTangemPayWaitlistRequestBody
import com.tangem.scenarios.clickTangemPayResidencyItem
import com.tangem.scenarios.getTangemPayEligibilityCheckRequestCount
import com.tangem.scenarios.getTangemPayEligibilityCheckRequestCountByCountry
import com.tangem.scenarios.getTangemPayRemovalRequestCount
import com.tangem.scenarios.getTangemPayWaitlistRequestBodies
import com.tangem.scenarios.openMainScreenWithExistingHotWallet
import com.tangem.scenarios.openTangemPayCountryStepFromDetails
import com.tangem.scenarios.returnToMainFromTangemPayRegionUnavailableScreen
import com.tangem.screens.onDetailsScreen
import com.tangem.screens.onMainScreenTopBar
import com.tangem.screens.tangempay.onTangemPayMainScreen
import com.tangem.screens.tangempay.onTangemPayRegionUnavailableScreen
import com.tangem.screens.tangempay.onTangemPayRegionUnavailableSheet
import com.tangem.screens.tangempay.onTangemPayResidencyScreen
import com.tangem.screens.tangempay.onTangemPayWaitlistSheet
import com.tangem.core.res.R as CoreResR
import dagger.hilt.android.testing.HiltAndroidTest
import io.github.kakaocup.kakao.common.utilities.getResourceString
import io.qameta.allure.kotlin.AllureId
import io.qameta.allure.kotlin.junit4.DisplayName
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

@HiltAndroidTest
class TangemPayRegionUnavailableTest : BaseTestCase() {

    private val unitedStates = "United States"
    private val alabama = "Alabama"
    private val kazakhstan = "Kazakhstan"
    private val validEmail = "pupu@bubug.com"
    private val productPaymentAccount = "PAYMENT_ACCOUNT"

    @AllureId("12761")
    @DisplayName("Tangem Pay: an unavailable country or US state opens the region-unavailable screen")
    @Test
    fun unavailableRegionOpensRegionUnavailableScreenTest() {
        val eligibilityStartedState = "Started"
        val kycRequiredState = "Required"
        val geoFullListState = "FullList"
        val eligibilityCheckUnavailableState = "Unavailable"
        val kazakhstanCode = "KZ"
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
            step(
                "Set WireMock scenario '$TANGEM_PAY_ELIGIBILITY_CHECK_SCENARIO' to '$eligibilityCheckUnavailableState'",
            ) {
                setWireMockScenarioState(TANGEM_PAY_ELIGIBILITY_CHECK_SCENARIO, eligibilityCheckUnavailableState)
            }
            step("Open 'Main' screen with existing hot wallet") {
                openMainScreenWithExistingHotWallet(SVS_SEED_PHRASE_12, accessCode = TANGEM_PAY_ACCESS_CODE)
            }
            step("Open 'Country of residence' screen from app Details") { openTangemPayCountryStepFromDetails() }
            val countryChecksBefore = getTangemPayEligibilityCheckRequestCountByCountry(kazakhstanCode)
            step("Click on country: '$kazakhstan'") { clickTangemPayResidencyItem(kazakhstan) }
            step("Assert eligibility is checked for '$kazakhstanCode'") {
                awaitTangemPayRequestCountIncreased("eligibility/check", countryChecksBefore) {
                    getTangemPayEligibilityCheckRequestCountByCountry(kazakhstanCode)
                }
            }
            step("Assert region-unavailable screen is displayed") {
                flakySafely {
                    onTangemPayRegionUnavailableScreen {
                        title.assertIsDisplayed()
                        description.assertIsDisplayed()
                        joinWaitlistButton.assertIsDisplayed()
                        selectAnotherRegionButton.assertIsDisplayed()
                        closeButton.assertIsDisplayed()
                    }
                }
            }
            step("Click on 'Select another region' button") {
                onTangemPayRegionUnavailableScreen { selectAnotherRegionButton.clickWithAssertion() }
            }
            step("Assert 'Country of residence' screen is displayed") {
                flakySafely { onTangemPayResidencyScreen { countryTitle.assertIsDisplayed() } }
            }
            step("Click on country: '$unitedStates'") { clickTangemPayResidencyItem(unitedStates) }
            step("Assert 'Select state' screen is displayed") {
                flakySafely { onTangemPayResidencyScreen { regionTitle.assertIsDisplayed() } }
            }
            val stateChecksBefore = getTangemPayEligibilityCheckRequestCount(alabamaCode)
            step("Click on state: '$alabama'") { clickTangemPayResidencyItem(alabama) }
            step("Assert eligibility is checked for '$alabamaCode'") {
                awaitTangemPayRequestCountIncreased("eligibility/check", stateChecksBefore) {
                    getTangemPayEligibilityCheckRequestCount(alabamaCode)
                }
            }
            step("Assert region-unavailable screen is displayed") {
                flakySafely { onTangemPayRegionUnavailableScreen { title.assertIsDisplayed() } }
            }
        }
    }

    @AllureId("12762")
    @DisplayName("Tangem Pay: Join the waitlist form shows its content with the email field in focus")
    @Test
    fun joinWaitlistFormContentTest() {
        val privacyPolicyUrl = "https://tangem.com/docs/en/privacy-policy.pdf"
        val eligibilityStartedState = "Started"
        val kycRequiredState = "Required"
        val geoFullListState = "FullList"
        val eligibilityCheckUnavailableState = "Unavailable"
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
            step(
                "Set WireMock scenario '$TANGEM_PAY_ELIGIBILITY_CHECK_SCENARIO' to '$eligibilityCheckUnavailableState'",
            ) {
                setWireMockScenarioState(TANGEM_PAY_ELIGIBILITY_CHECK_SCENARIO, eligibilityCheckUnavailableState)
            }
            step("Open 'Main' screen with existing hot wallet") {
                openMainScreenWithExistingHotWallet(SVS_SEED_PHRASE_12, accessCode = TANGEM_PAY_ACCESS_CODE)
            }
            step("Open 'Country of residence' screen from app Details") { openTangemPayCountryStepFromDetails() }
            step("Click on country: '$kazakhstan'") { clickTangemPayResidencyItem(kazakhstan) }
            step("Click on 'Join the waitlist' button") {
                flakySafely {
                    onTangemPayRegionUnavailableScreen { joinWaitlistButton.clickWithAssertion() }
                }
            }
            step("Assert email field is focused") { awaitTangemPayWaitlistEmailFieldFocused() }
            step("Assert 'Join the waitlist' form is displayed") {
                onTangemPayWaitlistSheet {
                    title.assertIsDisplayed()
                    description.assertIsDisplayed()
                    emailLabel.assertIsDisplayed()
                    emailPlaceholder.assertIsDisplayed()
                    marketingConsentCheckbox.assertIsDisplayed()
                    marketingConsentText.assertIsDisplayed()
                    saveButton.assertIsDisplayed()
                }
            }
            step("Stub opening the Privacy Policy page") { stubExternalUrlOpening() }
            step("Click on 'Privacy Policy' link") {
                onTangemPayWaitlistSheet { clickPrivacyPolicyLink() }
            }
            step("Assert the Privacy Policy page '$privacyPolicyUrl' is opened") {
                flakySafely { assertUrlOpenedStartingWith(privacyPolicyUrl) }
            }
        }
    }

    @AllureId("12763")
    @DisplayName("Tangem Pay: joining the waitlist for a US state with marketing consent")
    @Test
    fun joinWaitlistForUsStateWithMarketingConsentTest() {
        val eligibilityPaeraCustomerState = "PaeraCustomer"
        val eligibilityStartedState = "Started"
        val kycRequiredState = "Required"
        val geoFullListState = "FullList"
        val eligibilityCheckUnavailableState = "Unavailable"
        val invalidEmail = "pupupu@bubu"
        val alabamaCode = "US-AL"
        val unitedStatesCode = "US"

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
            step(
                "Set WireMock scenario '$TANGEM_PAY_ELIGIBILITY_CHECK_SCENARIO' to '$eligibilityCheckUnavailableState'",
            ) {
                setWireMockScenarioState(TANGEM_PAY_ELIGIBILITY_CHECK_SCENARIO, eligibilityCheckUnavailableState)
            }
            step("Open 'Main' screen with existing hot wallet") {
                openMainScreenWithExistingHotWallet(SVS_SEED_PHRASE_12, accessCode = TANGEM_PAY_ACCESS_CODE)
            }
            step("Open 'Country of residence' screen from app Details") { openTangemPayCountryStepFromDetails() }
            step("Set WireMock scenario '$TANGEM_PAY_ELIGIBILITY_SCENARIO' to '$eligibilityPaeraCustomerState'") {
                setWireMockScenarioState(TANGEM_PAY_ELIGIBILITY_SCENARIO, eligibilityPaeraCustomerState)
            }
            step("Click on country: '$unitedStates'") { clickTangemPayResidencyItem(unitedStates) }
            step("Click on state: '$alabama'") { clickTangemPayResidencyItem(alabama) }
            step("Click on 'Join the waitlist' button") {
                flakySafely {
                    onTangemPayRegionUnavailableScreen { joinWaitlistButton.clickWithAssertion() }
                }
            }
            step("Assert email field is focused") { awaitTangemPayWaitlistEmailFieldFocused() }
            step("Type '$invalidEmail' into email field") {
                onTangemPayWaitlistSheet { emailField.performTextReplacement(invalidEmail) }
            }
            step("Click on 'Save' button") {
                onTangemPayWaitlistSheet { saveButton.clickWithAssertion() }
            }
            step("Assert 'Incorrect email format' hint is displayed") {
                flakySafely { onTangemPayWaitlistSheet { emailError.assertIsDisplayed() } }
            }
            step("Type '$validEmail' into email field") {
                onTangemPayWaitlistSheet { emailField.performTextReplacement(validEmail) }
            }
            step("Click on marketing consent checkbox") {
                onTangemPayWaitlistSheet { marketingConsentCheckbox.clickWithAssertion() }
            }
            val waitlistRequestsBefore = getTangemPayWaitlistRequestBodies().size
            step("Click on 'Save' button") {
                onTangemPayWaitlistSheet { saveButton.clickWithAssertion() }
            }
            step("Assert waitlist request carries the email, the US state and the marketing consent") {
                val body = awaitTangemPayWaitlistRequestBody(waitlistRequestsBefore)
                assertEquals(validEmail, body.getString("email"))
                assertEquals(true, body.getBoolean("marketing_consent"))
                assertEquals(unitedStatesCode, body.getString("country_code"))
                assertEquals(productPaymentAccount, body.getString("product"))
                assertEquals(alabamaCode, body.getString("region_code"))
            }
            step("Assert waitlist is joined") { assertTangemPayWaitlistJoined() }
        }
    }

    @AllureId("12776")
    @DisplayName("Tangem Pay: joining the waitlist for a country without marketing consent")
    @Test
    fun joinWaitlistForCountryWithoutMarketingConsentTest() {
        val eligibilityPaeraCustomerState = "PaeraCustomer"
        val eligibilityStartedState = "Started"
        val kycRequiredState = "Required"
        val geoFullListState = "FullList"
        val eligibilityCheckUnavailableState = "Unavailable"
        val kazakhstanCode = "KZ"

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
            step(
                "Set WireMock scenario '$TANGEM_PAY_ELIGIBILITY_CHECK_SCENARIO' to '$eligibilityCheckUnavailableState'",
            ) {
                setWireMockScenarioState(TANGEM_PAY_ELIGIBILITY_CHECK_SCENARIO, eligibilityCheckUnavailableState)
            }
            step("Open 'Main' screen with existing hot wallet") {
                openMainScreenWithExistingHotWallet(SVS_SEED_PHRASE_12, accessCode = TANGEM_PAY_ACCESS_CODE)
            }
            step("Open 'Country of residence' screen from app Details") { openTangemPayCountryStepFromDetails() }
            step("Set WireMock scenario '$TANGEM_PAY_ELIGIBILITY_SCENARIO' to '$eligibilityPaeraCustomerState'") {
                setWireMockScenarioState(TANGEM_PAY_ELIGIBILITY_SCENARIO, eligibilityPaeraCustomerState)
            }
            step("Click on country: '$kazakhstan'") { clickTangemPayResidencyItem(kazakhstan) }
            step("Click on 'Join the waitlist' button") {
                flakySafely {
                    onTangemPayRegionUnavailableScreen { joinWaitlistButton.clickWithAssertion() }
                }
            }
            step("Assert email field is focused") { awaitTangemPayWaitlistEmailFieldFocused() }
            step("Type '$validEmail' into email field") {
                onTangemPayWaitlistSheet { emailField.performTextReplacement(validEmail) }
            }
            val waitlistRequestsBefore = getTangemPayWaitlistRequestBodies().size
            step("Click on 'Save' button") {
                onTangemPayWaitlistSheet { saveButton.clickWithAssertion() }
            }
            step("Assert waitlist request carries the email and the country without a region and consent") {
                val body = awaitTangemPayWaitlistRequestBody(waitlistRequestsBefore)
                assertEquals(validEmail, body.getString("email"))
                assertEquals(false, body.getBoolean("marketing_consent"))
                assertEquals(kazakhstanCode, body.getString("country_code"))
                assertEquals(productPaymentAccount, body.getString("product"))
                assertFalse("Waitlist request must not carry 'region_code': $body", body.has("region_code"))
            }
            step("Assert waitlist is joined") { assertTangemPayWaitlistJoined() }
        }
    }

    @AllureId("12765")
    @DisplayName("Tangem Pay: Hide account from home hides the Payment account of an unavailable country")
    @Test
    fun hideAccountFromHomeForUnavailableCountryTest() {
        val eligibilityStartedState = "Started"
        val kycRequiredState = "Required"
        val geoFullListState = "FullList"
        val eligibilityCheckUnavailableState = "Unavailable"
        val eligibilityPaeraCustomerState = "PaeraCustomer"
        val actionRequiredText = getResourceString(CoreResR.string.tangempay_action_required_title)

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
            step(
                "Set WireMock scenario '$TANGEM_PAY_ELIGIBILITY_CHECK_SCENARIO' to '$eligibilityCheckUnavailableState'",
            ) {
                setWireMockScenarioState(TANGEM_PAY_ELIGIBILITY_CHECK_SCENARIO, eligibilityCheckUnavailableState)
            }
            step("Open 'Main' screen with existing hot wallet") {
                openMainScreenWithExistingHotWallet(SVS_SEED_PHRASE_12, accessCode = TANGEM_PAY_ACCESS_CODE)
            }
            step("Open 'Country of residence' screen from app Details") { openTangemPayCountryStepFromDetails() }
            step("Set WireMock scenario '$TANGEM_PAY_ELIGIBILITY_SCENARIO' to '$eligibilityPaeraCustomerState'") {
                setWireMockScenarioState(TANGEM_PAY_ELIGIBILITY_SCENARIO, eligibilityPaeraCustomerState)
            }
            step("Click on country: '$kazakhstan'") { clickTangemPayResidencyItem(kazakhstan) }
            step("Assert region-unavailable screen is displayed") {
                flakySafely { onTangemPayRegionUnavailableScreen { title.assertIsDisplayed() } }
            }
            step("Return to 'Main' screen") { returnToMainFromTangemPayRegionUnavailableScreen() }
            step("Assert Tangem Pay tile with '$actionRequiredText' is displayed") {
                flakySafely(WAIT_UNTIL_TIMEOUT_LONG) {
                    onTangemPayMainScreen { tileWithSubtitle(actionRequiredText).assertIsDisplayed() }
                }
            }
            step("Click on Tangem Pay tile") {
                onTangemPayMainScreen { mainScreenTile.clickWithAssertion() }
            }
            step("Assert 'Not available in your region yet' sheet is displayed") {
                flakySafely { onTangemPayRegionUnavailableSheet { title.assertIsDisplayed() } }
            }
            val removalRequestsBefore = getTangemPayRemovalRequestCount()
            step("Click on 'Hide account from home' button") {
                onTangemPayRegionUnavailableSheet { hideAccountButton.clickWithAssertion() }
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

    @AllureId("12766")
    @DisplayName("Tangem Pay: Not available in your region yet sheet content and Select another region")
    @Test
    fun regionUnavailableSheetContentTest() {
        val eligibilityStartedState = "Started"
        val kycRequiredState = "Required"
        val geoFullListState = "FullList"
        val eligibilityCheckUnavailableState = "Unavailable"
        val eligibilityPaeraCustomerState = "PaeraCustomer"
        val actionRequiredText = getResourceString(CoreResR.string.tangempay_action_required_title)

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
            step(
                "Set WireMock scenario '$TANGEM_PAY_ELIGIBILITY_CHECK_SCENARIO' to '$eligibilityCheckUnavailableState'",
            ) {
                setWireMockScenarioState(TANGEM_PAY_ELIGIBILITY_CHECK_SCENARIO, eligibilityCheckUnavailableState)
            }
            step("Open 'Main' screen with existing hot wallet") {
                openMainScreenWithExistingHotWallet(SVS_SEED_PHRASE_12, accessCode = TANGEM_PAY_ACCESS_CODE)
            }
            step("Open 'Country of residence' screen from app Details") { openTangemPayCountryStepFromDetails() }
            step("Set WireMock scenario '$TANGEM_PAY_ELIGIBILITY_SCENARIO' to '$eligibilityPaeraCustomerState'") {
                setWireMockScenarioState(TANGEM_PAY_ELIGIBILITY_SCENARIO, eligibilityPaeraCustomerState)
            }
            step("Click on country: '$kazakhstan'") { clickTangemPayResidencyItem(kazakhstan) }
            step("Assert region-unavailable screen is displayed") {
                flakySafely { onTangemPayRegionUnavailableScreen { title.assertIsDisplayed() } }
            }
            step("Return to 'Main' screen") { returnToMainFromTangemPayRegionUnavailableScreen() }
            step("Assert Tangem Pay tile with '$actionRequiredText' is displayed") {
                flakySafely(WAIT_UNTIL_TIMEOUT_LONG) {
                    onTangemPayMainScreen { tileWithSubtitle(actionRequiredText).assertIsDisplayed() }
                }
            }
            step("Click on Tangem Pay tile") {
                onTangemPayMainScreen { mainScreenTile.clickWithAssertion() }
            }
            step("Assert 'Not available in your region yet' sheet is displayed") {
                flakySafely {
                    onTangemPayRegionUnavailableSheet {
                        icon.assertIsDisplayed()
                        title.assertIsDisplayed()
                        message.assertIsDisplayed()
                        selectAnotherRegionButton.assertIsDisplayed()
                        hideAccountButton.assertIsDisplayed()
                    }
                }
            }
            step("Click on 'Select another region' button") {
                onTangemPayRegionUnavailableSheet { selectAnotherRegionButton.clickWithAssertion() }
            }
            step("Assert 'Country of residence' screen is displayed") {
                flakySafely { onTangemPayResidencyScreen { countryTitle.assertIsDisplayed() } }
            }
        }
    }
}