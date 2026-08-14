package com.tangem.tests.tangempay

import androidx.test.InstrumentationRegistry.getTargetContext
import com.tangem.common.BaseTestCase
import com.tangem.common.constants.TestConstants.TANGEM_PAY_ELIGIBILITY_SCENARIO
import com.tangem.common.constants.TestConstants.WAIT_UNTIL_TIMEOUT_LONG
import com.tangem.common.extensions.assertTextContainsSafe
import com.tangem.common.extensions.bringAppToForeground
import com.tangem.common.extensions.clickWithAssertion
import com.tangem.common.extensions.collapseAppByHomeButton
import com.tangem.common.utils.resetWireMockScenarioState
import com.tangem.common.utils.resetWireMockScenarios
import com.tangem.common.utils.setWireMockScenarioState
import com.tangem.core.res.R as CoreResR
import com.tangem.scenarios.openTangemPayCardPage
import com.tangem.scenarios.openTangemPayChangePin
import com.tangem.scenarios.openTangemPayViewPin
import com.tangem.screens.tangempay.onTangemPayCardPageScreen
import com.tangem.screens.tangempay.onTangemPayChangePinScreen
import com.tangem.screens.tangempay.onTangemPayViewPinSheet
import dagger.hilt.android.testing.HiltAndroidTest
import io.github.kakaocup.kakao.common.utilities.getResourceString
import io.qameta.allure.kotlin.AllureId
import io.qameta.allure.kotlin.junit4.DisplayName
import org.junit.Test

@HiltAndroidTest
class TangemPayPinTest : BaseTestCase() {

    private val pinSetupScenario = "tangem_pay_pin_setup"
    private val pinNotSetState = "PinNotSet"
    private val pinSetState = "PinSet"
    private val eligibilityState = "PaeraCustomer"

    private val repeatedPin = "1111"
    private val sequentialPin = "4567"
    private val validPin = "6194"

    // Mirrors MOCK_PIN in MockAwareTangemPayCardDetailsRepository — the PIN the mocked build reveals.
    private val currentPin = "1234"

    @AllureId("9650")
    @DisplayName("Tangem Pay: PIN entry screen opens from card details when PIN is not set")
    @Test
    fun pinEntryScreenOpensFromCardDetailsWhenPinIsNotSetTest() {
        val descriptionText = getResourceString(CoreResR.string.visa_onboarding_pin_code_description)

        setupHooks(
            additionalBeforeSection = { resetWireMockScenarios() },
            additionalAfterSection = {
                resetWireMockScenarioState(TANGEM_PAY_ELIGIBILITY_SCENARIO)
                resetWireMockScenarioState(pinSetupScenario)
            },
        ).run {
            step("Set WireMock scenario: '$TANGEM_PAY_ELIGIBILITY_SCENARIO' to state: '$eligibilityState'") {
                setWireMockScenarioState(scenarioName = TANGEM_PAY_ELIGIBILITY_SCENARIO, state = eligibilityState)
            }
            step("Set WireMock scenario: '$pinSetupScenario' to state: '$pinNotSetState'") {
                setWireMockScenarioState(scenarioName = pinSetupScenario, state = pinNotSetState)
            }
            step("Open PIN entry screen") { openTangemPayChangePin() }
            step("Assert PIN entry description is displayed") {
                onTangemPayChangePinScreen { description.assertTextContainsSafe(descriptionText) }
            }
            step("Assert validation error is not displayed") {
                onTangemPayChangePinScreen { errorMessage.assertDoesNotExist() }
            }
        }
    }

    @AllureId("9534")
    @DisplayName("Tangem Pay: PIN validation rejects repeated and sequential digits")
    @Test
    fun pinValidationRejectsRepeatedAndSequentialDigitsTest() {
        val validationError = getResourceString(CoreResR.string.visa_onboarding_pin_validation_error_message)

        setupHooks(
            additionalBeforeSection = { resetWireMockScenarios() },
            additionalAfterSection = {
                resetWireMockScenarioState(TANGEM_PAY_ELIGIBILITY_SCENARIO)
                resetWireMockScenarioState(pinSetupScenario)
            },
        ).run {
            step("Set WireMock scenario: '$TANGEM_PAY_ELIGIBILITY_SCENARIO' to state: '$eligibilityState'") {
                setWireMockScenarioState(scenarioName = TANGEM_PAY_ELIGIBILITY_SCENARIO, state = eligibilityState)
            }
            step("Set WireMock scenario: '$pinSetupScenario' to state: '$pinNotSetState'") {
                setWireMockScenarioState(scenarioName = pinSetupScenario, state = pinNotSetState)
            }
            step("Open PIN entry screen") { openTangemPayChangePin() }
            step("Enter repeated PIN '$repeatedPin'") {
                onTangemPayChangePinScreen { inputField.performTextInput(repeatedPin) }
            }
            step("Assert PIN input field contains '$repeatedPin'") {
                onTangemPayChangePinScreen { inputField.assertTextContainsSafe(repeatedPin) }
            }
            step("Assert validation error is displayed") {
                flakySafely {
                    onTangemPayChangePinScreen { errorMessage.assertTextContainsSafe(validationError) }
                }
            }
            step("Delete entered digits") {
                onTangemPayChangePinScreen { inputField.performTextClearance() }
            }
            step("Assert validation error is not displayed") {
                flakySafely { onTangemPayChangePinScreen { errorMessage.assertDoesNotExist() } }
            }
            step("Enter sequential PIN '$sequentialPin'") {
                onTangemPayChangePinScreen { inputField.performTextInput(sequentialPin) }
            }
            step("Assert validation error is displayed") {
                flakySafely {
                    onTangemPayChangePinScreen { errorMessage.assertTextContainsSafe(validationError) }
                }
            }
        }
    }

    @AllureId("9579")
    @DisplayName("Tangem Pay: PIN entry screen closes without saving and returns to card details")
    @Test
    fun pinEntryScreenClosesWithoutSavingAndReturnsToCardDetailsTest() {
        setupHooks(
            additionalBeforeSection = { resetWireMockScenarios() },
            additionalAfterSection = {
                resetWireMockScenarioState(TANGEM_PAY_ELIGIBILITY_SCENARIO)
                resetWireMockScenarioState(pinSetupScenario)
            },
        ).run {
            step("Set WireMock scenario: '$TANGEM_PAY_ELIGIBILITY_SCENARIO' to state: '$eligibilityState'") {
                setWireMockScenarioState(scenarioName = TANGEM_PAY_ELIGIBILITY_SCENARIO, state = eligibilityState)
            }
            step("Set WireMock scenario: '$pinSetupScenario' to state: '$pinNotSetState'") {
                setWireMockScenarioState(scenarioName = pinSetupScenario, state = pinNotSetState)
            }
            step("Open PIN entry screen") { openTangemPayChangePin() }
            // A valid PIN auto-submits, so the digits that must stay on screen are deliberately invalid.
            step("Enter repeated PIN '$repeatedPin'") {
                onTangemPayChangePinScreen { inputField.performTextInput(repeatedPin) }
            }
            step("Assert PIN input field contains '$repeatedPin'") {
                onTangemPayChangePinScreen { inputField.assertTextContainsSafe(repeatedPin) }
            }
            step("Click on 'Close' button") {
                onTangemPayChangePinScreen { closeButton.clickWithAssertion() }
            }
            step("Assert PIN entry screen is not displayed") {
                flakySafely { onTangemPayChangePinScreen { inputField.assertDoesNotExist() } }
            }
            step("Assert card page 'PIN code' row is displayed") {
                onTangemPayCardPageScreen { changePinRow.assertIsDisplayed() }
            }
        }
    }

    @AllureId("9582")
    @DisplayName("Tangem Pay: success screen shows the created PIN state")
    @Test
    fun pinSuccessScreenShowsCreatedStateTest() {
        val successTitleText = getResourceString(CoreResR.string.tangempay_card_details_change_pin_success_title)
        val successDescriptionText =
            getResourceString(CoreResR.string.tangempay_card_details_change_pin_success_description)

        setupHooks(
            additionalBeforeSection = { resetWireMockScenarios() },
            additionalAfterSection = {
                resetWireMockScenarioState(TANGEM_PAY_ELIGIBILITY_SCENARIO)
                resetWireMockScenarioState(pinSetupScenario)
            },
        ).run {
            step("Set WireMock scenario: '$TANGEM_PAY_ELIGIBILITY_SCENARIO' to state: '$eligibilityState'") {
                setWireMockScenarioState(scenarioName = TANGEM_PAY_ELIGIBILITY_SCENARIO, state = eligibilityState)
            }
            step("Set WireMock scenario: '$pinSetupScenario' to state: '$pinNotSetState'") {
                setWireMockScenarioState(scenarioName = pinSetupScenario, state = pinNotSetState)
            }
            step("Open PIN entry screen") { openTangemPayChangePin() }
            // There is no submit button: a valid full PIN is submitted automatically.
            step("Enter valid PIN '$validPin'") {
                onTangemPayChangePinScreen { inputField.performTextInput(validPin) }
            }
            step("Assert success screen title is displayed") {
                flakySafely(WAIT_UNTIL_TIMEOUT_LONG) {
                    onTangemPayChangePinScreen { successTitle.assertTextContainsSafe(successTitleText) }
                }
            }
            step("Assert success screen description is displayed") {
                onTangemPayChangePinScreen { successDescription.assertTextContainsSafe(successDescriptionText) }
            }
            step("Assert 'Done' button is displayed") {
                onTangemPayChangePinScreen { doneButton.assertIsDisplayed() }
            }
        }
    }

    @AllureId("9651")
    @DisplayName("Tangem Pay: current PIN sheet opens when a PIN is already set on the card")
    @Test
    fun currentPinSheetOpensWhenPinIsAlreadySetTest() {
        setupHooks(
            additionalBeforeSection = { resetWireMockScenarios() },
            additionalAfterSection = {
                resetWireMockScenarioState(TANGEM_PAY_ELIGIBILITY_SCENARIO)
                resetWireMockScenarioState(pinSetupScenario)
            },
        ).run {
            step("Set WireMock scenario: '$TANGEM_PAY_ELIGIBILITY_SCENARIO' to state: '$eligibilityState'") {
                setWireMockScenarioState(scenarioName = TANGEM_PAY_ELIGIBILITY_SCENARIO, state = eligibilityState)
            }
            step("Set WireMock scenario: '$pinSetupScenario' to state: '$pinSetState'") {
                setWireMockScenarioState(scenarioName = pinSetupScenario, state = pinSetState)
            }
            step("Open current PIN sheet") { openTangemPayViewPin() }
            step("Assert current PIN '$currentPin' is displayed") {
                flakySafely { onTangemPayViewPinSheet { pin.assertTextEquals(currentPin) } }
            }
            step("Assert 'Change PIN-code' button is displayed") {
                onTangemPayViewPinSheet { changePinButton.assertIsDisplayed() }
            }
        }
    }

    @AllureId("9654")
    @DisplayName("Tangem Pay: loader is shown while the current PIN is loading")
    @Test
    fun loaderIsShownWhileCurrentPinIsLoadingTest() {
        // Mirrors UITEST_PIN_DELAY_MS_KEY in MockAwareTangemPayCardDetailsRepository — stalls the current-PIN read.
        val pinDelayKey = "uitest.tangempay.pin_delay_ms"
        val pinDelayMs = "10000"

        setupHooks(
            additionalBeforeSection = {
                resetWireMockScenarios()
                System.setProperty(pinDelayKey, pinDelayMs)
            },
            additionalAfterSection = {
                System.clearProperty(pinDelayKey)
                resetWireMockScenarioState(TANGEM_PAY_ELIGIBILITY_SCENARIO)
                resetWireMockScenarioState(pinSetupScenario)
            },
        ).run {
            step("Set WireMock scenario: '$TANGEM_PAY_ELIGIBILITY_SCENARIO' to state: '$eligibilityState'") {
                setWireMockScenarioState(scenarioName = TANGEM_PAY_ELIGIBILITY_SCENARIO, state = eligibilityState)
            }
            step("Set WireMock scenario: '$pinSetupScenario' to state: '$pinSetState'") {
                setWireMockScenarioState(scenarioName = pinSetupScenario, state = pinSetState)
            }
            step("Open current PIN sheet") { openTangemPayViewPin() }
            step("Assert loader is displayed") {
                onTangemPayViewPinSheet { loader.assertIsDisplayed() }
            }
            step("Assert 'Change PIN-code' button is not enabled") {
                onTangemPayViewPinSheet { changePinButton.assertIsNotEnabled() }
            }
            step("Assert current PIN '$currentPin' is displayed once loaded") {
                flakySafely(WAIT_UNTIL_TIMEOUT_LONG) {
                    onTangemPayViewPinSheet { pin.assertTextEquals(currentPin) }
                }
            }
            step("Assert loader is not displayed") {
                onTangemPayViewPinSheet { loader.assertDoesNotExist() }
            }
        }
    }

    @AllureId("9655")
    @DisplayName("Tangem Pay: current PIN sheet is dismissed after collapsing and reopening the app")
    @Test
    fun currentPinSheetIsDismissedAfterCollapseAndExpandTest() {
        val packageName = getTargetContext().packageName

        setupHooks(
            additionalBeforeSection = { resetWireMockScenarios() },
            additionalAfterSection = {
                resetWireMockScenarioState(TANGEM_PAY_ELIGIBILITY_SCENARIO)
                resetWireMockScenarioState(pinSetupScenario)
            },
        ).run {
            step("Set WireMock scenario: '$TANGEM_PAY_ELIGIBILITY_SCENARIO' to state: '$eligibilityState'") {
                setWireMockScenarioState(scenarioName = TANGEM_PAY_ELIGIBILITY_SCENARIO, state = eligibilityState)
            }
            step("Set WireMock scenario: '$pinSetupScenario' to state: '$pinSetState'") {
                setWireMockScenarioState(scenarioName = pinSetupScenario, state = pinSetState)
            }
            step("Open current PIN sheet") { openTangemPayViewPin() }
            step("Assert current PIN '$currentPin' is displayed") {
                flakySafely { onTangemPayViewPinSheet { pin.assertTextEquals(currentPin) } }
            }
            step("Press 'Home' to collapse the app") { collapseAppByHomeButton() }
            step("Return to the app") { bringAppToForeground(packageName) }
            // [REDACTED_TASK_KEY] dismisses the PIN sheet on pause, so the PIN needs biometrics again; iOS keeps it open.
            step("Assert 'Card page' screen is displayed") {
                flakySafely { onTangemPayCardPageScreen { changePinRow.assertIsDisplayed() } }
            }
            step("Assert current PIN sheet is not displayed") {
                onTangemPayViewPinSheet {
                    title.assertDoesNotExist()
                    pin.assertDoesNotExist()
                }
            }
        }
    }

    @AllureId("9653")
    @DisplayName("Tangem Pay: error is shown when the current PIN request fails")
    @Test
    fun errorIsShownWhenCurrentPinRequestFailsTest() {
        // Mirrors UITEST_PIN_ERROR_KEY in MockAwareTangemPayCardDetailsRepository — fails the PIN read.
        val pinErrorKey = "uitest.tangempay.pin_error"

        setupHooks(
            additionalBeforeSection = {
                resetWireMockScenarios()
                System.setProperty(pinErrorKey, "1")
            },
            additionalAfterSection = {
                System.clearProperty(pinErrorKey)
                resetWireMockScenarioState(TANGEM_PAY_ELIGIBILITY_SCENARIO)
                resetWireMockScenarioState(pinSetupScenario)
            },
        ).run {
            step("Set WireMock scenario: '$TANGEM_PAY_ELIGIBILITY_SCENARIO' to state: '$eligibilityState'") {
                setWireMockScenarioState(scenarioName = TANGEM_PAY_ELIGIBILITY_SCENARIO, state = eligibilityState)
            }
            step("Set WireMock scenario: '$pinSetupScenario' to state: '$pinSetState'") {
                setWireMockScenarioState(scenarioName = pinSetupScenario, state = pinSetState)
            }
            step("Open card page") { openTangemPayCardPage() }
            step("Click on 'PIN code' row") {
                onTangemPayCardPageScreen { changePinRow.clickWithAssertion() }
            }
            // Android surfaces the failed PIN read inside the sheet itself, not as a toast the way iOS does.
            step("Assert error title is displayed") {
                flakySafely { onTangemPayViewPinSheet { errorTitle.assertIsDisplayed() } }
            }
            step("Assert error message is displayed") {
                onTangemPayViewPinSheet { errorMessage.assertIsDisplayed() }
            }
            step("Click on 'Got it' button") {
                onTangemPayViewPinSheet { errorGotItButton.clickWithAssertion() }
            }
            step("Assert error is not displayed") {
                flakySafely { onTangemPayViewPinSheet { errorTitle.assertDoesNotExist() } }
            }
            step("Assert card page 'PIN code' row is displayed") {
                onTangemPayCardPageScreen { changePinRow.assertIsDisplayed() }
            }
        }
    }

    @AllureId("5053")
    @DisplayName("Tangem Pay: PIN change succeeds for a card that already has a PIN")
    @Test
    fun pinChangeSucceedsForCardWithPinAlreadySetTest() {
        val successTitleText = getResourceString(CoreResR.string.tangempay_card_details_change_pin_success_title)

        setupHooks(
            additionalBeforeSection = { resetWireMockScenarios() },
            additionalAfterSection = {
                resetWireMockScenarioState(TANGEM_PAY_ELIGIBILITY_SCENARIO)
                resetWireMockScenarioState(pinSetupScenario)
            },
        ).run {
            step("Set WireMock scenario: '$TANGEM_PAY_ELIGIBILITY_SCENARIO' to state: '$eligibilityState'") {
                setWireMockScenarioState(scenarioName = TANGEM_PAY_ELIGIBILITY_SCENARIO, state = eligibilityState)
            }
            step("Set WireMock scenario: '$pinSetupScenario' to state: '$pinSetState'") {
                setWireMockScenarioState(scenarioName = pinSetupScenario, state = pinSetState)
            }
            step("Open current PIN sheet") { openTangemPayViewPin() }
            step("Assert current PIN '$currentPin' is displayed") {
                flakySafely { onTangemPayViewPinSheet { pin.assertTextEquals(currentPin) } }
            }
            step("Click on 'Change PIN-code' button") {
                onTangemPayViewPinSheet { changePinButton.clickWithAssertion() }
            }
            step("Assert PIN entry screen is displayed") {
                flakySafely { onTangemPayChangePinScreen { inputField.assertIsDisplayed() } }
            }
            // There is no submit button: a valid full PIN is submitted automatically.
            step("Enter valid PIN '$validPin'") {
                onTangemPayChangePinScreen { inputField.performTextInput(validPin) }
            }
            step("Assert success screen title is displayed") {
                flakySafely(WAIT_UNTIL_TIMEOUT_LONG) {
                    onTangemPayChangePinScreen { successTitle.assertTextContainsSafe(successTitleText) }
                }
            }
            step("Click on 'Done' button") {
                onTangemPayChangePinScreen { doneButton.clickWithAssertion() }
            }
            step("Assert card page 'PIN code' row is displayed") {
                flakySafely { onTangemPayCardPageScreen { changePinRow.assertIsDisplayed() } }
            }
        }
    }
}