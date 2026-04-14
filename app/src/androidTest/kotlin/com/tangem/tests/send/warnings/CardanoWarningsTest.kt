package com.tangem.tests.send.warnings

import com.tangem.common.BaseTestCase
import com.tangem.common.constants.TestConstants.CARDANO_ADDRESS
import com.tangem.common.constants.TestConstants.QUOTES_API_SCENARIO
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
class CardanoWarningsTest : BaseTestCase() {
    private val tokenName = "Cardano"
    private val minAmount = "ADA 1.00"

    private val invalidAmountTitle = getResourceString(R.string.send_notification_invalid_amount_title)
    private val invalidAmountMessage =
        getResourceString(R.string.send_notification_invalid_minimum_amount_text, minAmount, minAmount)

    @AllureId("4204")
    @DisplayName("Warnings: check warning, when remains less than 1 ADA")
    @Test
    fun afterTransactionRemainsLessThanMinimumAmountTest() {
        val sendAmount = "19"

        setupHooks(
            additionalAfterSection = {
                resetWireMockScenarioState(USER_TOKENS_API_SCENARIO)
                resetWireMockScenarioState(QUOTES_API_SCENARIO)
            }
        ).run {

            step("Open 'Send Screen' with token: $tokenName") {
                openSendScreen(tokenName)
            }
            step("Type '$sendAmount' in input text field") {
                onSendScreen {
                    amountInputTextField.performClick()
                    amountInputTextField.performTextReplacement(sendAmount)
                }
            }
            step("Click on 'Next' button") {
                onSendScreen { nextButton.clickWithAssertion() }
            }
            step("Type address in input text field") {
                onSendAddressScreen { addressTextField.performTextReplacement(CARDANO_ADDRESS) }
            }
            step("Click on 'Next' button") {
                onSendAddressScreen { nextButton.clickWithAssertion() }
            }
            step("Assert 'Invalid amount' warning is displayed") {
                checkSendWarning(
                    title = invalidAmountTitle,
                    message = invalidAmountMessage,
                    isDisplayed = true
                )
            }
        }
    }

    @AllureId("4207")
    @DisplayName("Warnings: check warning, when amount more than 1 ADA")
    @Test
    fun transactionAmountMoreThanOneTest() {
        val sendAmount = "2.5"

        setupHooks(
            additionalAfterSection = {
                resetWireMockScenarioState(USER_TOKENS_API_SCENARIO)
                resetWireMockScenarioState(QUOTES_API_SCENARIO)
            }
        ).run {

            step("Open 'Send Screen' with token: $tokenName") {
                openSendScreen(tokenName)
            }
            step("Type '$sendAmount' in input text field") {
                onSendScreen {
                    amountInputTextField.performClick()
                    amountInputTextField.performTextReplacement(sendAmount)
                }
            }
            step("Click on 'Next' button") {
                onSendScreen { nextButton.clickWithAssertion() }
            }
            step("Type address in input text field") {
                onSendAddressScreen { addressTextField.performTextReplacement(CARDANO_ADDRESS) }
            }
            step("Click on 'Next' button") {
                onSendAddressScreen { nextButton.clickWithAssertion() }
            }
            step("Assert 'Invalid amount' warning is not displayed") {
                checkSendWarning(
                    title = invalidAmountTitle,
                    message = invalidAmountMessage,
                    isDisplayed = false
                )
            }
        }
    }

    @AllureId("4210")
    @DisplayName("Warnings: check warning, when remains more than 1 ADA")
    @Test
    fun afterTransactionRemainsMoreThanMinimumAmountTest() {
        val sendAmount = "18"

        setupHooks(
            additionalAfterSection = {
                resetWireMockScenarioState(USER_TOKENS_API_SCENARIO)
                resetWireMockScenarioState(QUOTES_API_SCENARIO)
            }
        ).run {

            step("Open 'Send Screen' with token: $tokenName") {
                openSendScreen(tokenName)
            }
            step("Type '$sendAmount' in input text field") {
                onSendScreen {
                    amountInputTextField.performClick()
                    amountInputTextField.performTextReplacement(sendAmount)
                }
            }
            step("Click on 'Next' button") {
                onSendScreen { nextButton.clickWithAssertion() }
            }
            step("Type address in input text field") {
                onSendAddressScreen { addressTextField.performTextReplacement(CARDANO_ADDRESS) }
            }
            step("Click on 'Next' button") {
                onSendAddressScreen { nextButton.clickWithAssertion() }
            }
            step("Assert 'Invalid amount' warning is not displayed") {
                checkSendWarning(
                    title = invalidAmountTitle,
                    message = invalidAmountMessage,
                    isDisplayed = false
                )
            }
        }
    }
}