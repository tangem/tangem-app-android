package com.tangem.tests.tangempay

import com.tangem.common.BaseTestCase
import com.tangem.common.constants.TestConstants.TANGEM_PAY_ELIGIBILITY_SCENARIO
import com.tangem.common.constants.TestConstants.WAIT_UNTIL_TIMEOUT_LONG
import com.tangem.common.extensions.assertTextContainsSafe
import com.tangem.common.extensions.clickWithAssertion
import com.tangem.common.utils.resetWireMockScenarioState
import com.tangem.common.utils.setWireMockScenarioState
import com.tangem.core.res.R as CoreResR
import com.tangem.scenarios.openTangemPayChangePin
import com.tangem.screens.tangempay.onTangemPayCardPageScreen
import com.tangem.screens.tangempay.onTangemPayChangePinScreen
import dagger.hilt.android.testing.HiltAndroidTest
import io.github.kakaocup.kakao.common.utilities.getResourceString
import io.qameta.allure.kotlin.AllureId
import io.qameta.allure.kotlin.junit4.DisplayName
import org.junit.Test

@HiltAndroidTest
class TangemPayPinTest : BaseTestCase() {

    private val pinSetupScenario = "tangem_pay_pin_setup"
    private val pinNotSetState = "PinNotSet"
    private val eligibilityState = "PaeraCustomer"

    private val repeatedPin = "1111"
    private val sequentialPin = "4567"
    private val validPin = "6194"

    @AllureId("9650")
    @DisplayName("Tangem Pay: PIN entry screen opens from card details when PIN is not set")
    @Test
    fun pinEntryScreenOpensFromCardDetailsWhenPinIsNotSetTest() {
        val descriptionText = getResourceString(CoreResR.string.visa_onboarding_pin_code_description)

        setupHooks(
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
            step("Assert entered digits are kept in the input field") {
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
}