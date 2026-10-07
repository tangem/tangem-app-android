package com.tangem.tests.tangempay

import com.tangem.common.BaseTestCase
import com.tangem.common.constants.TestConstants.TANGEM_PAY_DEEPLINK_VALIDATE_SCENARIO
import com.tangem.common.constants.TestConstants.TANGEM_PAY_ELIGIBILITY_SCENARIO
import com.tangem.common.constants.TestConstants.TANGEM_PAY_KYC_STATUS_SCENARIO
import com.tangem.common.constants.TestConstants.WAIT_UNTIL_TIMEOUT
import com.tangem.common.constants.TestConstants.WAIT_UNTIL_TIMEOUT_LONG
import com.tangem.common.constants.TestConstants.WAIT_UNTIL_TIMEOUT_VERY_LONG
import com.tangem.common.extensions.clickWithAssertion
import com.tangem.common.utils.getWireMockRequestCount
import com.tangem.common.utils.setWireMockScenarioState
import com.tangem.scenarios.openAppByDeepLink
import com.tangem.scenarios.openMainScreenWithNewHotWallet
import com.tangem.screens.onIntroductionScreen
import com.tangem.screens.onMainScreen
import com.tangem.screens.tangempay.onTangemPayHotWalletOnboardingScreen
import com.tangem.screens.tangempay.onTangemPayKycScreen
import com.tangem.screens.tangempay.onTangemPayOnboardingScreen
import dagger.hilt.android.testing.HiltAndroidTest
import io.qameta.allure.kotlin.AllureId
import io.qameta.allure.kotlin.junit4.DisplayName
import org.junit.Assert
import org.junit.Test

@HiltAndroidTest
class TangemPayDeeplinkOnboardingTest : BaseTestCase() {

    private val onboardVisaDeeplink = "tangem://onboard-visa?entry=ff1&id=6USB3LU2"
    private val validateEndpoint = "/bff-v2/v1/deeplink/validate"

    @AllureId("9527")
    @DisplayName("Tangem Pay: onboard-visa deeplink validated as VALID opens Tangem Pay onboarding")
    @Test
    fun onboardingOpensForValidOnboardVisaDeeplinkTest() {
        val eligibilityStartedState = "Started"
        val validateValidState = "Started"

        setupHooks().run {
            step("Set WireMock scenario '$TANGEM_PAY_ELIGIBILITY_SCENARIO' to '$eligibilityStartedState'") {
                setWireMockScenarioState(TANGEM_PAY_ELIGIBILITY_SCENARIO, eligibilityStartedState)
            }
            step("Set WireMock scenario '$TANGEM_PAY_DEEPLINK_VALIDATE_SCENARIO' to '$validateValidState'") {
                setWireMockScenarioState(TANGEM_PAY_DEEPLINK_VALIDATE_SCENARIO, validateValidState)
            }
            step("Create a new mobile wallet and open 'Main' screen") {
                openMainScreenWithNewHotWallet()
            }
            val validateRequestsBefore = getWireMockRequestCount("POST", validateEndpoint)
            step("Open deeplink '$onboardVisaDeeplink'") {
                openAppByDeepLink(onboardVisaDeeplink)
            }
            step("Assert Tangem Pay onboarding screen is displayed") {
                flakySafely(WAIT_UNTIL_TIMEOUT) {
                    onTangemPayOnboardingScreen {
                        title.assertIsDisplayed()
                        getCardButton.assertIsDisplayed()
                    }
                }
            }
            step("Assert '$validateEndpoint' is requested once") {
                val validateRequestsAfter = getWireMockRequestCount("POST", validateEndpoint)
                Assert.assertEquals(1, validateRequestsAfter - validateRequestsBefore)
            }
        }
    }

    @AllureId("10415")
    @DisplayName("Tangem Pay: onboard-visa deeplink validated as INVALID opens the region restriction screen")
    @Test
    fun regionRestrictionOpensForInvalidOnboardVisaDeeplinkTest() {
        val eligibilityStartedState = "Started"
        val validateInvalidState = "Invalid"

        setupHooks().run {
            step("Set WireMock scenario '$TANGEM_PAY_ELIGIBILITY_SCENARIO' to '$eligibilityStartedState'") {
                setWireMockScenarioState(TANGEM_PAY_ELIGIBILITY_SCENARIO, eligibilityStartedState)
            }
            step("Set WireMock scenario '$TANGEM_PAY_DEEPLINK_VALIDATE_SCENARIO' to '$validateInvalidState'") {
                setWireMockScenarioState(TANGEM_PAY_DEEPLINK_VALIDATE_SCENARIO, validateInvalidState)
            }
            step("Create a new mobile wallet and open 'Main' screen") {
                openMainScreenWithNewHotWallet()
            }
            val validateRequestsBefore = getWireMockRequestCount("POST", validateEndpoint)
            step("Open deeplink '$onboardVisaDeeplink'") {
                openAppByDeepLink(onboardVisaDeeplink)
            }
            step("Assert region restriction screen is displayed") {
                flakySafely(WAIT_UNTIL_TIMEOUT) {
                    onTangemPayOnboardingScreen {
                        unavailableRegionIcon.assertIsDisplayed()
                        unavailableRegionTitle.assertIsDisplayed()
                        unavailableRegionDescription.assertIsDisplayed()
                        gotItButton.assertIsDisplayed()
                    }
                }
            }
            step("Assert '$validateEndpoint' is requested once") {
                val validateRequestsAfter = getWireMockRequestCount("POST", validateEndpoint)
                Assert.assertEquals(1, validateRequestsAfter - validateRequestsBefore)
            }
            step("Click on 'Got it' button") {
                onTangemPayOnboardingScreen { gotItButton.clickWithAssertion() }
            }
            step("Assert 'Main' screen is displayed") {
                flakySafely { onMainScreen { screenContainer.assertIsDisplayed() } }
            }
        }
    }

    @AllureId("12096")
    @DisplayName("Tangem Pay: onboard-visa deeplink received without a wallet keeps the Home screen")
    @Test
    fun onboardingDoesNotOpenForDeeplinkWithoutWalletTest() {
        val eligibilityStartedState = "Started"

        setupHooks().run {
            step("Set WireMock scenario '$TANGEM_PAY_ELIGIBILITY_SCENARIO' to '$eligibilityStartedState'") {
                setWireMockScenarioState(TANGEM_PAY_ELIGIBILITY_SCENARIO, eligibilityStartedState)
            }
            step("Assert 'Introduction' screen is displayed") {
                flakySafely(WAIT_UNTIL_TIMEOUT_LONG) {
                    onIntroductionScreen { createWalletButton.assertIsDisplayed() }
                }
            }
            val validateRequestsBefore = getWireMockRequestCount("POST", validateEndpoint)
            step("Open deeplink '$onboardVisaDeeplink'") {
                openAppByDeepLink(onboardVisaDeeplink)
            }
            step("Assert 'Introduction' screen is displayed") {
                waitForIdle()
                onIntroductionScreen { createWalletButton.assertIsDisplayed() }
            }
            step("Assert Tangem Pay onboarding screen is not displayed") {
                onTangemPayOnboardingScreen { title.assertIsNotDisplayed() }
            }
            step("Assert Tangem Pay mobile onboarding screen is not displayed") {
                onTangemPayHotWalletOnboardingScreen { setupWalletBlockTitle.assertIsNotDisplayed() }
            }
            step("Assert '$validateEndpoint' is not requested") {
                val validateRequestsAfter = getWireMockRequestCount("POST", validateEndpoint)
                Assert.assertEquals(0, validateRequestsAfter - validateRequestsBefore)
            }
        }
    }

    @AllureId("12097")
    @DisplayName("Tangem Pay: onboard-visa deeplink received without a wallet opens onboarding once a wallet is created")
    @Test
    fun onboardingOpensAfterWalletCreatedForDeeplinkTest() {
        val eligibilityStartedState = "Started"
        val validateValidState = "Started"

        setupHooks().run {
            step("Set WireMock scenario '$TANGEM_PAY_ELIGIBILITY_SCENARIO' to '$eligibilityStartedState'") {
                setWireMockScenarioState(TANGEM_PAY_ELIGIBILITY_SCENARIO, eligibilityStartedState)
            }
            step("Set WireMock scenario '$TANGEM_PAY_DEEPLINK_VALIDATE_SCENARIO' to '$validateValidState'") {
                setWireMockScenarioState(TANGEM_PAY_DEEPLINK_VALIDATE_SCENARIO, validateValidState)
            }
            step("Assert 'Introduction' screen is displayed") {
                flakySafely(WAIT_UNTIL_TIMEOUT_LONG) {
                    onIntroductionScreen { createWalletButton.assertIsDisplayed() }
                }
            }
            val validateRequestsBefore = getWireMockRequestCount("POST", validateEndpoint)
            step("Open deeplink '$onboardVisaDeeplink'") {
                openAppByDeepLink(onboardVisaDeeplink)
            }
            // Not openMainScreenWithNewHotWallet: the parked deeplink covers Main before its tooltip can be closed.
            step("Click on 'Create wallet' button") {
                onIntroductionScreen { createWalletButton.clickWithAssertion() }
            }
            step("Click on 'Create seed phrase wallet' option") {
                onIntroductionScreen { createSeedPhraseWalletOption.clickWithAssertion() }
            }
            step("Assert Tangem Pay onboarding screen is displayed") {
                flakySafely(WAIT_UNTIL_TIMEOUT_VERY_LONG) {
                    onTangemPayOnboardingScreen {
                        title.assertIsDisplayed()
                        getCardButton.assertIsDisplayed()
                    }
                }
            }
            step("Assert Tangem Pay mobile onboarding 'Set up wallet' block is not displayed") {
                onTangemPayHotWalletOnboardingScreen { setupWalletBlockTitle.assertIsNotDisplayed() }
            }
            step("Assert '$validateEndpoint' is requested once") {
                val validateRequestsAfter = getWireMockRequestCount("POST", validateEndpoint)
                Assert.assertEquals(1, validateRequestsAfter - validateRequestsBefore)
            }
        }
    }

    @AllureId("9698")
    @DisplayName("Tangem Pay: production pay-app link opens onboarding and 'Get card' starts KYC")
    @Test
    fun payAppLinkOpensOnboardingAndGetCardStartsKycTest() {
        val payAppLink = "https://tangem.com/pay-app?id=dec25"
        val eligibilityStartedState = "Started"
        val validateValidState = "Started"
        val kycInProgressState = "InProgress"

        setupHooks().run {
            step("Set WireMock scenario '$TANGEM_PAY_ELIGIBILITY_SCENARIO' to '$eligibilityStartedState'") {
                setWireMockScenarioState(TANGEM_PAY_ELIGIBILITY_SCENARIO, eligibilityStartedState)
            }
            step("Set WireMock scenario '$TANGEM_PAY_DEEPLINK_VALIDATE_SCENARIO' to '$validateValidState'") {
                setWireMockScenarioState(TANGEM_PAY_DEEPLINK_VALIDATE_SCENARIO, validateValidState)
            }
            step("Set WireMock scenario '$TANGEM_PAY_KYC_STATUS_SCENARIO' to '$kycInProgressState'") {
                setWireMockScenarioState(TANGEM_PAY_KYC_STATUS_SCENARIO, kycInProgressState)
            }
            step("Create a new mobile wallet and open 'Main' screen") {
                openMainScreenWithNewHotWallet()
            }
            step("Open link '$payAppLink'") {
                openAppByDeepLink(payAppLink)
            }
            step("Assert Tangem Pay onboarding screen is displayed") {
                flakySafely(WAIT_UNTIL_TIMEOUT) {
                    onTangemPayOnboardingScreen { title.assertIsDisplayed() }
                }
            }
            step("Click on 'Get card' button") {
                onTangemPayOnboardingScreen { getCardButton.clickWithAssertion() }
            }
            // The mocked build skips the wallet authorization that precedes KYC in production.
            step("Assert KYC screen is displayed") {
                flakySafely(WAIT_UNTIL_TIMEOUT_LONG) {
                    onTangemPayKycScreen { screenContainer.assertIsDisplayed() }
                }
            }
        }
    }
}