package com.tangem.tests.tangempay

import com.tangem.common.BaseTestCase
import com.tangem.common.constants.TestConstants.TANGEM_PAY_ELIGIBILITY_SCENARIO
import com.tangem.common.extensions.assertSnackbarWithText
import com.tangem.common.extensions.assertTextContainsSafe
import com.tangem.common.extensions.clickWithAssertion
import com.tangem.common.utils.resetWireMockScenarioState
import com.tangem.common.utils.resetWireMockScenarios
import com.tangem.common.utils.setWireMockScenarioState
import com.tangem.core.res.R as CoreResR
import com.tangem.scenarios.openTangemPayCardPage
import com.tangem.scenarios.revealCardDetailsFromCardPage
import com.tangem.screens.tangempay.onTangemPayAddToWalletGuideScreen
import com.tangem.screens.tangempay.onTangemPayCardPageScreen
import dagger.hilt.android.testing.HiltAndroidTest
import io.github.kakaocup.kakao.common.utilities.getResourceString
import io.qameta.allure.kotlin.AllureId
import io.qameta.allure.kotlin.junit4.DisplayName
import org.junit.Ignore
import org.junit.Test

@HiltAndroidTest
class TangemPayCardRequisitesTest : BaseTestCase() {

    private val eligibilityState = "PaeraCustomer"

    @AllureId("9595")
    @DisplayName("Tangem Pay: card requisites are displayed on the card details and in the Add to wallet guide")
    @Test
    fun cardRequisitesDisplayedInCardDetailsAndGuideTest() {
        val expectedNumber = "4242 4242 4242"
        val expectedExpiry = "12/28"
        val expectedCvc = "123"

        setupHooks(
            additionalBeforeSection = {
                resetWireMockScenarios()
                setWireMockScenarioState(TANGEM_PAY_ELIGIBILITY_SCENARIO, eligibilityState)
            },
            additionalAfterSection = {
                resetWireMockScenarioState(TANGEM_PAY_ELIGIBILITY_SCENARIO)
            },
        ).run {
            openTangemPayCardPage()
            step("Reveal card details from the card page") { revealCardDetailsFromCardPage() }
            step("Assert card number is displayed") {
                onTangemPayCardPageScreen { numberValue.assertTextContainsSafe(expectedNumber, substring = true) }
            }
            step("Assert card expiration is displayed") {
                onTangemPayCardPageScreen { expirationValue.assertTextEquals(expectedExpiry) }
            }
            step("Assert card CVC is displayed") {
                onTangemPayCardPageScreen { cvcValue.assertTextEquals(expectedCvc) }
            }
            step("Click on 'Add to wallet' banner") {
                onTangemPayCardPageScreen { addToWalletBanner.clickWithAssertion() }
            }
            step("Assert Add to wallet guide is displayed") {
                flakySafely { onTangemPayAddToWalletGuideScreen { container.assertIsDisplayed() } }
            }
            step("Click on 'Show details' button in the guide") {
                flakySafely { onTangemPayAddToWalletGuideScreen { showDetailsButton.assertIsDisplayed() } }
                onTangemPayAddToWalletGuideScreen { showDetailsButton.performClick() }
            }
            step("Assert guide card number is displayed") {
                flakySafely { onTangemPayAddToWalletGuideScreen { numberValue.assertIsDisplayed() } }
                onTangemPayAddToWalletGuideScreen {
                    numberValue.assertTextContainsSafe(expectedNumber, substring = true)
                }
            }
            step("Assert guide card expiration is displayed") {
                onTangemPayAddToWalletGuideScreen { expirationValue.assertTextEquals(expectedExpiry) }
            }
            step("Assert guide card CVC is displayed") {
                onTangemPayAddToWalletGuideScreen { cvcValue.assertTextEquals(expectedCvc) }
            }
        }
    }

    @Ignore("[REDACTED_JIRA]")
    @AllureId("9593")
    @DisplayName("Tangem Pay: guide requisites are hidden independently from the card details")
    @Test
    fun guideRequisitesHiddenIndependentlyFromCardDetailsTest() {
        setupHooks(
            additionalBeforeSection = {
                resetWireMockScenarios()
                setWireMockScenarioState(TANGEM_PAY_ELIGIBILITY_SCENARIO, eligibilityState)
            },
            additionalAfterSection = {
                resetWireMockScenarioState(TANGEM_PAY_ELIGIBILITY_SCENARIO)
            },
        ).run {
            openTangemPayCardPage()
            step("Reveal card details from the card page") { revealCardDetailsFromCardPage() }
            step("Click on 'Add to wallet' banner") {
                onTangemPayCardPageScreen { addToWalletBanner.clickWithAssertion() }
            }
            step("Assert Add to wallet guide is displayed") {
                flakySafely { onTangemPayAddToWalletGuideScreen { container.assertIsDisplayed() } }
            }
            // Both card faces stay in the tree (SubcomposeLayout) — the hidden face is laid out at zero size.
            step("Assert guide card number is not displayed") {
                onTangemPayAddToWalletGuideScreen { numberValue.assertIsNotDisplayed() }
            }
            step("Assert guide 'Show details' button is displayed") {
                onTangemPayAddToWalletGuideScreen { showDetailsButton.assertIsDisplayed() }
            }
            step("Click on 'Show details' button in the guide") {
                onTangemPayAddToWalletGuideScreen { showDetailsButton.performClick() }
            }
            step("Assert guide card number is displayed") {
                flakySafely { onTangemPayAddToWalletGuideScreen { numberValue.assertIsDisplayed() } }
            }
            step("Click on 'Close' button in the guide") {
                onTangemPayAddToWalletGuideScreen { closeButton.performClick() }
            }
            // Android keeps the card-page reveal alive on return (30s timer) — no iOS auto-hide-on-return.
            step("Assert card page is displayed again") {
                flakySafely { onTangemPayCardPageScreen { moreButton.assertIsDisplayed() } }
            }
        }
    }

    @AllureId("9560")
    @DisplayName("Tangem Pay: error toast is shown when card details reveal fails")
    @Test
    fun cardDetailsErrorToastShownWhenRevealFailsTest() {
        // Mirrors UITEST_REVEAL_ERROR_KEY in MockAwareTangemPayCardDetailsRepository — forces the reveal to fail.
        val revealErrorKey = "uitest.tangempay.card_details_error"
        val errorText = getResourceString(CoreResR.string.tangempay_card_details_error_text)

        setupHooks(
            additionalBeforeSection = {
                resetWireMockScenarios()
                setWireMockScenarioState(TANGEM_PAY_ELIGIBILITY_SCENARIO, eligibilityState)
                System.setProperty(revealErrorKey, "1")
            },
            additionalAfterSection = {
                System.clearProperty(revealErrorKey)
                resetWireMockScenarioState(TANGEM_PAY_ELIGIBILITY_SCENARIO)
            },
        ).run {
            openTangemPayCardPage()
            step("Click on 'Show details' row") {
                onTangemPayCardPageScreen { showDetailsButton.clickWithAssertion() }
            }
            step("Assert card details error toast is displayed") {
                assertSnackbarWithText(errorText)
            }
        }
    }
}