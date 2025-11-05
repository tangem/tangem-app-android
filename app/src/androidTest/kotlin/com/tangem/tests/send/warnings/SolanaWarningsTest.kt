package com.tangem.tests.send.warnings

import com.tangem.common.BaseTestCase
import com.tangem.common.constants.TestConstants.QUOTES_API_SCENARIO
import com.tangem.common.constants.TestConstants.SOLANA_RECIPIENT_ADDRESS
import com.tangem.common.constants.TestConstants.USER_TOKENS_API_SCENARIO
import com.tangem.common.extensions.clickWithAssertion
import com.tangem.common.utils.resetWireMockScenarioState
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
class SolanaWarningsTest : BaseTestCase() {
    private val tokenName = "Solana"
    private val amountToLeaveLessThanRent = "0.0016941"
    private val amountToLeaveGreaterThanRent = "0.0000941"
    private val amountToLeaveRentOnly = "0.00168934"
    private val rentAmount = "SOL 0.00089088"

    private val invalidAmountTitle = getResourceString(R.string.send_notification_invalid_amount_title)
    private val invalidAmountMessage = getResourceString(R.string.send_notification_invalid_amount_rent_fee, rentAmount)

    @AllureId("564")
    @DisplayName("Warnings: warning is displayed, if after send balance is less than rent amount (SOLANA)")
    @Test
    fun warningIsDisplayedWhenLeaveLessThanRent() {
        setupHooks(
            additionalAfterSection = {
                resetWireMockScenarioState(USER_TOKENS_API_SCENARIO)
                resetWireMockScenarioState(QUOTES_API_SCENARIO)
            }
        ).run {
            step("Open 'Send Screen' with token: $tokenName") {
                openSendScreen(tokenName)
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
                    title = invalidAmountTitle,
                    message = invalidAmountMessage
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
                resetWireMockScenarioState(USER_TOKENS_API_SCENARIO)
                resetWireMockScenarioState(QUOTES_API_SCENARIO)
            }
        ).run {
            step("Open 'Send Screen' with token: $tokenName") {
                openSendScreen(tokenName)
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
                    title = invalidAmountTitle,
                    message = invalidAmountMessage,
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
                resetWireMockScenarioState(USER_TOKENS_API_SCENARIO)
                resetWireMockScenarioState(QUOTES_API_SCENARIO)
            }
        ).run {
            step("Open 'Send Screen' with token: $tokenName") {
                openSendScreen(tokenName)
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
                    title = invalidAmountTitle,
                    message = invalidAmountMessage,
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
                resetWireMockScenarioState(USER_TOKENS_API_SCENARIO)
                resetWireMockScenarioState(QUOTES_API_SCENARIO)
            }
        ).run {
            step("Open 'Send Screen' with token: $tokenName") {
                openSendScreen(tokenName)
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
                    title = invalidAmountTitle,
                    message = invalidAmountMessage,
                    isDisplayed = false
                )
            }
        }
    }
}