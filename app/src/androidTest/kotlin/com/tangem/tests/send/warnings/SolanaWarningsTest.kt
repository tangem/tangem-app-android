package com.tangem.tests.send.warnings

import com.tangem.common.BaseTestCase
import com.tangem.common.constants.TestConstants.SOLANA_RECIPIENT_ADDRESS
import com.tangem.common.extensions.clickWithAssertion
import com.tangem.common.utils.resetWireMockScenarioState
import com.tangem.common.utils.setWireMockScenarioState
import com.tangem.scenarios.checkSendWarning
import com.tangem.scenarios.openMainScreen
import com.tangem.scenarios.synchronizeAddresses
import com.tangem.screens.onMainScreen
import com.tangem.screens.onSendAddressScreen
import com.tangem.screens.onSendScreen
import com.tangem.screens.onTokenDetailsScreen
import com.tangem.wallet.R
import dagger.hilt.android.testing.HiltAndroidTest
import io.qameta.allure.kotlin.AllureId
import io.qameta.allure.kotlin.junit4.DisplayName
import org.junit.Test

@HiltAndroidTest
class SolanaWarningsTest : BaseTestCase() {
    private val tokenName = "Solana"
    private val amountToLeaveLessThanRent = "0.0016941"
    private val amountToLeaveGreaterThanRent = "0.0000941"
    private val amountToLeaveRentOnly = "0.00168934"
    private val userTokensScenarioName = "user_tokens_api"
    private val userTokensScenarioState = "Solana"
    private val quotesScenarioName = "quotes_api"
    private val quotesScenarioState = "Solana"
    private val rentAmount = "0.000890880"

    private val invalidAmountTitleResId = R.string.send_notification_invalid_amount_title
    private val invalidAmountMessageResId = R.string.send_notification_invalid_amount_rent_fee

    @AllureId("564")
    @DisplayName("Warnings: warning is displayed, if after send balance is less than rent amount (SOLANA)")
    @Test
    fun warningIsDisplayedWhenLeaveLessThanRent() {
        setupHooks(
            additionalAfterSection = {
                resetWireMockScenarioState(userTokensScenarioName)
                resetWireMockScenarioState(quotesScenarioName)
            }
        ).run {
            step("Set WireMock scenario: '$userTokensScenarioName' to state: '$userTokensScenarioState'") {
                setWireMockScenarioState(scenarioName = userTokensScenarioName, state = userTokensScenarioState)
            }
            step("Set WireMock scenario: '$quotesScenarioName' to state: '$quotesScenarioState'") {
                setWireMockScenarioState(scenarioName = quotesScenarioName, state = quotesScenarioState)
            }
            step("Open 'Main Screen'") {
                openMainScreen()
            }
            step("Synchronize addresses") {
                synchronizeAddresses()
            }
            step("Click on token with name: '$tokenName'") {
                onMainScreen { tokenWithTitleAndAddress(tokenName).clickWithAssertion() }
            }
            step("Click on 'Send' button") {
                onTokenDetailsScreen { sendButton().performClick() }
            }
            step("Type '$amountToLeaveLessThanRent' in input text field") {
                onSendScreen {
                    amountInputTextField.performClick()
                    amountInputTextField.performTextReplacement(amountToLeaveLessThanRent)
                }
            }
            step("Click on 'Next' button") {
                onSendScreen { nextButton.clickWithAssertion() }
            }
            step("Type address in input text field") {
                onSendAddressScreen { addressTextField.performTextReplacement(SOLANA_RECIPIENT_ADDRESS) }
            }
            step("Click on 'Next' button") {
                onSendAddressScreen { nextButton.clickWithAssertion() }
            }
            step("Assert 'Invalid amount warning' is displayed") {
                checkSendWarning(
                    titleResId = invalidAmountTitleResId,
                    messageResId = invalidAmountMessageResId,
                    amount = rentAmount
                )
            }
        }
    }

    @AllureId("567")
    @DisplayName("Warnings: warning is not displayed, if after send balance is greater than rent amount (SOLANA)")
    @Test
    fun warningIsNotDisplayedWhenLeaveGreaterThanRent() {
        setupHooks(
            additionalAfterSection = {
                resetWireMockScenarioState(userTokensScenarioName)
                resetWireMockScenarioState(quotesScenarioName)
            }
        ).run {
            step("Set WireMock scenario: '$userTokensScenarioName' to state: '$userTokensScenarioState'") {
                setWireMockScenarioState(scenarioName = userTokensScenarioName, state = userTokensScenarioState)
            }
            step("Set WireMock scenario: '$quotesScenarioName' to state: '$quotesScenarioState'") {
                setWireMockScenarioState(scenarioName = quotesScenarioName, state = quotesScenarioState)
            }
            step("Open 'Main Screen'") {
                openMainScreen()
            }
            step("Synchronize addresses") {
                synchronizeAddresses()
            }
            step("Click on token with name: '$tokenName'") {
                onMainScreen { tokenWithTitleAndAddress(tokenName).clickWithAssertion() }
            }
            step("Click on 'Send' button") {
                onTokenDetailsScreen { sendButton().performClick() }
            }
            step("Type '$amountToLeaveGreaterThanRent' in input text field") {
                onSendScreen {
                    amountInputTextField.performClick()
                    amountInputTextField.performTextReplacement(amountToLeaveGreaterThanRent)
                }
            }
            step("Click on 'Next' button") {
                onSendScreen { nextButton.clickWithAssertion() }
            }
            step("Type address in input text field") {
                onSendAddressScreen { addressTextField.performTextReplacement(SOLANA_RECIPIENT_ADDRESS) }
            }
            step("Click on 'Next' button") {
                onSendAddressScreen { nextButton.clickWithAssertion() }
            }
            step("Assert 'Invalid amount warning' is not displayed") {
                checkSendWarning(
                    titleResId = invalidAmountTitleResId,
                    messageResId = invalidAmountMessageResId,
                    amount = rentAmount,
                    isDisplayed = false
                )
            }
        }
    }

    @AllureId("566")
    @DisplayName("Warnings: warning is not displayed, if after send balance is equal to rent amount (SOLANA)")
    @Test
    fun warningIsNotDisplayedWhenLeaveOnlyRent() {
        setupHooks(
            additionalAfterSection = {
                resetWireMockScenarioState(userTokensScenarioName)
                resetWireMockScenarioState(quotesScenarioName)
            }
        ).run {
            step("Set WireMock scenario: '$userTokensScenarioName' to state: '$userTokensScenarioState'") {
                setWireMockScenarioState(scenarioName = userTokensScenarioName, state = userTokensScenarioState)
            }
            step("Set WireMock scenario: '$quotesScenarioName' to state: '$quotesScenarioState'") {
                setWireMockScenarioState(scenarioName = quotesScenarioName, state = quotesScenarioState)
            }
            step("Open 'Main Screen'") {
                openMainScreen()
            }
            step("Synchronize addresses") {
                synchronizeAddresses()
            }
            step("Click on token with name: '$tokenName'") {
                onMainScreen { tokenWithTitleAndAddress(tokenName).clickWithAssertion() }
            }
            step("Click on 'Send' button") {
                onTokenDetailsScreen { sendButton().performClick() }
            }
            step("Type '$amountToLeaveRentOnly' in input text field") {
                onSendScreen {
                    amountInputTextField.performClick()
                    amountInputTextField.performTextReplacement(amountToLeaveRentOnly)
                }
            }
            step("Click on 'Next' button") {
                onSendScreen { nextButton.clickWithAssertion() }
            }
            step("Type address in input text field") {
                onSendAddressScreen { addressTextField.performTextReplacement(SOLANA_RECIPIENT_ADDRESS) }
            }
            step("Click on 'Next' button") {
                onSendAddressScreen { nextButton.clickWithAssertion() }
            }
            step("Assert 'Invalid amount warning' is not displayed") {
                checkSendWarning(
                    titleResId = invalidAmountTitleResId,
                    messageResId = invalidAmountMessageResId,
                    amount = rentAmount,
                    isDisplayed = false
                )
            }
        }
    }

    @AllureId("565")
    @DisplayName("Warnings: warning is not displayed, if after send balance is zero (SOLANA)")
    @Test
    fun warningIsNotDisplayedWhenLeaveZeroSol() {
        setupHooks(
            additionalAfterSection = {
                resetWireMockScenarioState(userTokensScenarioName)
                resetWireMockScenarioState(quotesScenarioName)
            }
        ).run {
            step("Set WireMock scenario: '$userTokensScenarioName' to state: '$userTokensScenarioState'") {
                setWireMockScenarioState(scenarioName = userTokensScenarioName, state = userTokensScenarioState)
            }
            step("Set WireMock scenario: '$quotesScenarioName' to state: '$quotesScenarioState'") {
                setWireMockScenarioState(scenarioName = quotesScenarioName, state = quotesScenarioState)
            }
            step("Open 'Main Screen'") {
                openMainScreen()
            }
            step("Synchronize addresses") {
                synchronizeAddresses()
            }
            step("Click on token with name: '$tokenName'") {
                onMainScreen { tokenWithTitleAndAddress(tokenName).clickWithAssertion() }
            }
            step("Click on 'Send' button") {
                onTokenDetailsScreen { sendButton().performClick() }
            }
            step("Type max amount in input text field") {
                onSendScreen {
                    maxButton.performClick()
                }
            }
            step("Click on 'Next' button") {
                onSendScreen { nextButton.clickWithAssertion() }
            }
            step("Type address in input text field") {
                onSendAddressScreen { addressTextField.performTextReplacement(SOLANA_RECIPIENT_ADDRESS) }
            }
            step("Click on 'Next' button") {
                onSendAddressScreen { nextButton.clickWithAssertion() }
            }
            step("Assert 'Invalid amount warning' is not displayed") {
                checkSendWarning(
                    titleResId = invalidAmountTitleResId,
                    messageResId = invalidAmountMessageResId,
                    amount = rentAmount,
                    isDisplayed = false
                )
            }
        }
    }
}