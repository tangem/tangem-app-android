package com.tangem.tests.send.warnings

import com.tangem.common.BaseTestCase
import com.tangem.common.constants.TestConstants.DOGECOIN_RECIPIENT_ADDRESS
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
class DogecoinWarningsTest : BaseTestCase() {
    private val tokenName = "Dogecoin"
    private val amountToLeaveLessThanDust = "5.78654978"
    private val amountToLeaveMoreThanDust = "5.7"
    private val amountGreaterThanDust = "0.02"
    private val amountLessThanDust = "0.005"
    private val dustAmount = "DOGE 0.01"
    private val invalidAmountTitle = getResourceString(R.string.send_notification_invalid_amount_title)
    private val invalidAmountMessage = getResourceString(
        R.string.send_notification_invalid_minimum_amount_text,
        dustAmount, dustAmount
    )

    @AllureId("4213")
    @DisplayName("Warnings: warning is displayed, if after send balance is less than dust amount (Dogecoin)")
    @Test
    fun warningIsDisplayedWhenLeaveLessThanDust() {
        setupHooks(
            additionalAfterSection = {
                resetWireMockScenarioState(USER_TOKENS_API_SCENARIO)
                resetWireMockScenarioState(QUOTES_API_SCENARIO)
            }
        ).run {
            step("Open 'Send Screen' with token: $tokenName") {
                openSendScreen(tokenName)
            }
            step("Type '$amountToLeaveLessThanDust' in input text field") {
                onSendScreen {
                    amountInputTextField.performClick()
                    amountInputTextField.performTextReplacement(amountToLeaveLessThanDust)
                }
            }
            step("Click on 'Next' button") {
                onSendScreen { nextButton.clickWithAssertion() }
            }
            step("Type address in input text field") {
                onSendAddressScreen { addressTextField.performTextReplacement(DOGECOIN_RECIPIENT_ADDRESS) }
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

    @AllureId("4214")
    @DisplayName("Warnings: warning is NOT displayed, if after send balance is more than dust amount (Dogecoin)")
    @Test
    fun warningIsNotDisplayedWhenLeaveMoreThanDust() {
        setupHooks(
            additionalAfterSection = {
                resetWireMockScenarioState(USER_TOKENS_API_SCENARIO)
                resetWireMockScenarioState(QUOTES_API_SCENARIO)
            }
        ).run {
            step("Open 'Send Screen' with token: $tokenName") {
                openSendScreen(tokenName)
            }
            step("Type '$amountToLeaveMoreThanDust' in input text field") {
                onSendScreen {
                    amountInputTextField.performClick()
                    amountInputTextField.performTextReplacement(amountToLeaveMoreThanDust)
                }
            }
            step("Click on 'Next' button") {
                onSendScreen { nextButton.clickWithAssertion() }
            }
            step("Type address in input text field") {
                onSendAddressScreen { addressTextField.performTextReplacement(DOGECOIN_RECIPIENT_ADDRESS) }
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

    @AllureId("4211")
    @DisplayName("Warnings: warning is NOT displayed, when sending more than dust amount (Dogecoin)")
    @Test
    fun warningIsNotDisplayedWhenSendingMoreThanDust() {
        setupHooks(
            additionalAfterSection = {
                resetWireMockScenarioState(USER_TOKENS_API_SCENARIO)
                resetWireMockScenarioState(QUOTES_API_SCENARIO)
            }
        ).run {
            step("Open 'Send Screen' with token: $tokenName") {
                openSendScreen(tokenName)
            }
            step("Type '$amountGreaterThanDust' in input text field") {
                onSendScreen {
                    amountInputTextField.performClick()
                    amountInputTextField.performTextReplacement(amountGreaterThanDust)
                }
            }
            step("Click on 'Next' button") {
                onSendScreen { nextButton.clickWithAssertion() }
            }
            step("Type address in input text field") {
                onSendAddressScreen { addressTextField.performTextReplacement(DOGECOIN_RECIPIENT_ADDRESS) }
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

    @AllureId("4212")
    @DisplayName("Warnings: warning is displayed, when sending less than dust amount (Dogecoin)")
    @Test
    fun warningIsDisplayedWhenSendingLessThanDust() {
        setupHooks(
            additionalAfterSection = {
                resetWireMockScenarioState(USER_TOKENS_API_SCENARIO)
                resetWireMockScenarioState(QUOTES_API_SCENARIO)
            }
        ).run {
            step("Open 'Send Screen' with token: $tokenName") {
                openSendScreen(tokenName)
            }
            step("Type '$amountLessThanDust' in input text field") {
                onSendScreen {
                    amountInputTextField.performClick()
                    amountInputTextField.performTextReplacement(amountLessThanDust)
                }
            }
            step("Click on 'Next' button") {
                onSendScreen { nextButton.clickWithAssertion() }
            }
            step("Type address in input text field") {
                onSendAddressScreen { addressTextField.performTextReplacement(DOGECOIN_RECIPIENT_ADDRESS) }
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
}