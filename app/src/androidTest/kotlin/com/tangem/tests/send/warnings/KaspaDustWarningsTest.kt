package com.tangem.tests.send.warnings

import com.tangem.common.BaseTestCase
import com.tangem.common.constants.TestConstants.KASPA_RECIPIENT_ADDRESS
import com.tangem.common.constants.TestConstants.QUOTES_API_SCENARIO
import com.tangem.common.constants.TestConstants.USER_TOKENS_API_SCENARIO
import com.tangem.common.extensions.clickWithAssertion
import com.tangem.common.utils.resetWireMockScenarioState
import com.tangem.common.utils.setWireMockScenarioState
import com.tangem.scenarios.checkSendWarning
import com.tangem.scenarios.openSendScreen
import com.tangem.screens.onSendAddressScreen
import com.tangem.screens.onSendScreen
import com.tangem.wallet.R
import dagger.hilt.android.testing.HiltAndroidTest
import io.github.kakaocup.kakao.common.utilities.getResourceString
import io.qameta.allure.kotlin.AllureId
import io.qameta.allure.kotlin.junit4.DisplayName
import org.junit.Test

@HiltAndroidTest
class KaspaDustWarningsTest : BaseTestCase() {
    private val tokenName = "Kaspa"
    private val amountLessThanMinimum = "0.1"
    private val amountExactlyMinimum = "0.2"
    private val amountMoreThanMinimum = "0.3"
    private val amountToLeaveMoreThanMinimumChange = "0.5"
    private val amountToLeaveExactlyMinimumChange = "0.79"
    private val amountToLeaveLessThanMinimumChange = "0.85"

    private val kaspaUTXOScenarioName = "kaspa_utxo"
    private val dustState = "dust"

    private val dustAmount = "KAS 0.20"
    private val invalidAmountTitle = getResourceString(R.string.send_notification_invalid_amount_title)
    private val invalidAmountMessage = getResourceString(
        R.string.send_notification_invalid_minimum_amount_text,
        dustAmount, dustAmount
    )

    @AllureId("4685")
    @DisplayName("Warnings: invalid amount warning is displayed, when sending less than minimum amount (Kaspa)")
    @Test
    fun warningIsDisplayedWhenSendingLessThanMinimum() {
        setupHooks(
            additionalAfterSection = {
                resetWireMockScenarioState(kaspaUTXOScenarioName)
                resetWireMockScenarioState(USER_TOKENS_API_SCENARIO)
                resetWireMockScenarioState(QUOTES_API_SCENARIO)
            }
        ).run {
            step("Set WireMock scenario: '$kaspaUTXOScenarioName' to state: '$dustState'") {
                setWireMockScenarioState(scenarioName = kaspaUTXOScenarioName, state = dustState)
            }
            step("Open 'Send Screen' with token: $tokenName") {
                openSendScreen(tokenName)
            }
            step("Type '$amountLessThanMinimum' in input text field") {
                onSendScreen {
                    amountInputTextField.performClick()
                    amountInputTextField.performTextReplacement(amountLessThanMinimum)
                }
            }
            step("Click on 'Next' button") {
                onSendScreen { nextButton.clickWithAssertion() }
            }
            step("Type address in input text field") {
                onSendAddressScreen { addressTextField.performTextReplacement(KASPA_RECIPIENT_ADDRESS) }
            }
            step("Click on 'Next' button") {
                onSendAddressScreen { nextButton.clickWithAssertion() }
            }
            step("Assert 'Invalid amount warning' is displayed") {
                checkSendWarning(
                    title = invalidAmountTitle,
                    message = invalidAmountMessage
                )
            }
        }
    }

    @AllureId("9860")
    @DisplayName("Warnings: invalid amount warning is NOT displayed, when sending exactly minimum amount (Kaspa)")
    @Test
    fun warningIsNotDisplayedWhenSendingExactlyMinimum() {
        setupHooks(
            additionalAfterSection = {
                resetWireMockScenarioState(kaspaUTXOScenarioName)
                resetWireMockScenarioState(USER_TOKENS_API_SCENARIO)
                resetWireMockScenarioState(QUOTES_API_SCENARIO)
            }
        ).run {
            step("Set WireMock scenario: '$kaspaUTXOScenarioName' to state: '$dustState'") {
                setWireMockScenarioState(scenarioName = kaspaUTXOScenarioName, state = dustState)
            }
            step("Open 'Send Screen' with token: $tokenName") {
                openSendScreen(tokenName)
            }
            step("Type '$amountExactlyMinimum' in input text field") {
                onSendScreen {
                    amountInputTextField.performClick()
                    amountInputTextField.performTextReplacement(amountExactlyMinimum)
                }
            }
            step("Click on 'Next' button") {
                onSendScreen { nextButton.clickWithAssertion() }
            }
            step("Type address in input text field") {
                onSendAddressScreen { addressTextField.performTextReplacement(KASPA_RECIPIENT_ADDRESS) }
            }
            step("Click on 'Next' button") {
                onSendAddressScreen { nextButton.clickWithAssertion() }
            }
            step("Assert 'Invalid amount warning' is not displayed") {
                checkSendWarning(
                    title = invalidAmountTitle,
                    message = invalidAmountMessage,
                    isDisplayed = false
                )
            }
        }
    }

    @AllureId("4683")
    @DisplayName("Warnings: invalid amount warning is NOT displayed, when sending more than minimum amount (Kaspa)")
    @Test
    fun warningIsNotDisplayedWhenSendingMoreThanMinimum() {
        setupHooks(
            additionalAfterSection = {
                resetWireMockScenarioState(kaspaUTXOScenarioName)
                resetWireMockScenarioState(USER_TOKENS_API_SCENARIO)
                resetWireMockScenarioState(QUOTES_API_SCENARIO)
            }
        ).run {
            step("Set WireMock scenario: '$kaspaUTXOScenarioName' to state: '$dustState'") {
                setWireMockScenarioState(scenarioName = kaspaUTXOScenarioName, state = dustState)
            }
            step("Open 'Send Screen' with token: $tokenName") {
                openSendScreen(tokenName)
            }
            step("Type '$amountMoreThanMinimum' in input text field") {
                onSendScreen {
                    amountInputTextField.performClick()
                    amountInputTextField.performTextReplacement(amountMoreThanMinimum)
                }
            }
            step("Click on 'Next' button") {
                onSendScreen { nextButton.clickWithAssertion() }
            }
            step("Type address in input text field") {
                onSendAddressScreen { addressTextField.performTextReplacement(KASPA_RECIPIENT_ADDRESS) }
            }
            step("Click on 'Next' button") {
                onSendAddressScreen { nextButton.clickWithAssertion() }
            }
            step("Assert 'Invalid amount warning' is not displayed") {
                checkSendWarning(
                    title = invalidAmountTitle,
                    message = invalidAmountMessage,
                    isDisplayed = false
                )
            }
        }
    }

    @AllureId("4684")
    @DisplayName("Warnings: invalid amount warning is NOT displayed, when change is more than minimum amount (Kaspa)")
    @Test
    fun warningIsNotDisplayedWhenChangeIsMoreThanMinimum() {
        setupHooks(
            additionalAfterSection = {
                resetWireMockScenarioState(kaspaUTXOScenarioName)
                resetWireMockScenarioState(USER_TOKENS_API_SCENARIO)
                resetWireMockScenarioState(QUOTES_API_SCENARIO)
            }
        ).run {
            step("Set WireMock scenario: '$kaspaUTXOScenarioName' to state: '$dustState'") {
                setWireMockScenarioState(scenarioName = kaspaUTXOScenarioName, state = dustState)
            }
            step("Open 'Send Screen' with token: $tokenName") {
                openSendScreen(tokenName)
            }
            step("Type '$amountToLeaveMoreThanMinimumChange' in input text field") {
                onSendScreen {
                    amountInputTextField.performClick()
                    amountInputTextField.performTextReplacement(amountToLeaveMoreThanMinimumChange)
                }
            }
            step("Click on 'Next' button") {
                onSendScreen { nextButton.clickWithAssertion() }
            }
            step("Type address in input text field") {
                onSendAddressScreen { addressTextField.performTextReplacement(KASPA_RECIPIENT_ADDRESS) }
            }
            step("Click on 'Next' button") {
                onSendAddressScreen { nextButton.clickWithAssertion() }
            }
            step("Assert 'Invalid amount warning' is not displayed") {
                checkSendWarning(
                    title = invalidAmountTitle,
                    message = invalidAmountMessage,
                    isDisplayed = false
                )
            }
        }
    }

    @AllureId("4682")
    @DisplayName("Warnings: invalid amount warning is displayed, when change is less than minimum amount (Kaspa)")
    @Test
    fun warningIsDisplayedWhenChangeIsLessThanMinimum() {
        setupHooks(
            additionalAfterSection = {
                resetWireMockScenarioState(kaspaUTXOScenarioName)
                resetWireMockScenarioState(USER_TOKENS_API_SCENARIO)
                resetWireMockScenarioState(QUOTES_API_SCENARIO)
            }
        ).run {
            step("Set WireMock scenario: '$kaspaUTXOScenarioName' to state: '$dustState'") {
                setWireMockScenarioState(scenarioName = kaspaUTXOScenarioName, state = dustState)
            }
            step("Open 'Send Screen' with token: $tokenName") {
                openSendScreen(tokenName)
            }
            step("Type '$amountToLeaveLessThanMinimumChange' in input text field") {
                onSendScreen {
                    amountInputTextField.performClick()
                    amountInputTextField.performTextReplacement(amountToLeaveLessThanMinimumChange)
                }
            }
            step("Click on 'Next' button") {
                onSendScreen { nextButton.clickWithAssertion() }
            }
            step("Type address in input text field") {
                onSendAddressScreen { addressTextField.performTextReplacement(KASPA_RECIPIENT_ADDRESS) }
            }
            step("Click on 'Next' button") {
                onSendAddressScreen { nextButton.clickWithAssertion() }
            }
            step("Assert 'Invalid amount warning' is displayed") {
                checkSendWarning(
                    title = invalidAmountTitle,
                    message = invalidAmountMessage
                )
            }
        }
    }

    @AllureId("9861")
    @DisplayName("Warnings: invalid amount warning is NOT displayed, when change is exactly minimum amount (Kaspa)")
    @Test
    fun warningIsNotDisplayedWhenChangeIsExactlyMinimum() {
        setupHooks(
            additionalAfterSection = {
                resetWireMockScenarioState(kaspaUTXOScenarioName)
                resetWireMockScenarioState(USER_TOKENS_API_SCENARIO)
                resetWireMockScenarioState(QUOTES_API_SCENARIO)
            }
        ).run {
            step("Set WireMock scenario: '$kaspaUTXOScenarioName' to state: '$dustState'") {
                setWireMockScenarioState(scenarioName = kaspaUTXOScenarioName, state = dustState)
            }
            step("Open 'Send Screen' with token: $tokenName") {
                openSendScreen(tokenName)
            }
            step("Type '$amountToLeaveExactlyMinimumChange' in input text field") {
                onSendScreen {
                    amountInputTextField.performClick()
                    amountInputTextField.performTextReplacement(amountToLeaveExactlyMinimumChange)
                }
            }
            step("Click on 'Next' button") {
                onSendScreen { nextButton.clickWithAssertion() }
            }
            step("Type address in input text field") {
                onSendAddressScreen { addressTextField.performTextReplacement(KASPA_RECIPIENT_ADDRESS) }
            }
            step("Click on 'Next' button") {
                onSendAddressScreen { nextButton.clickWithAssertion() }
            }
            step("Assert 'Invalid amount warning' is not displayed") {
                checkSendWarning(
                    title = invalidAmountTitle,
                    message = invalidAmountMessage,
                    isDisplayed = false
                )
            }
        }
    }
}