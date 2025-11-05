package com.tangem.tests.send.warnings

import com.tangem.common.BaseTestCase
import com.tangem.common.constants.TestConstants.AZERO_RECIPIENT_ADDRESS
import com.tangem.common.constants.TestConstants.QUOTES_API_SCENARIO
import com.tangem.common.constants.TestConstants.USER_TOKENS_API_SCENARIO
import com.tangem.common.extensions.clickWithAssertion
import com.tangem.common.utils.resetWireMockScenarioState
import com.tangem.scenarios.checkSendWarning
import com.tangem.scenarios.openSendScreen
import com.tangem.screens.onSendAddressScreen
import com.tangem.screens.onSendConfirmScreen
import com.tangem.screens.onSendScreen
import com.tangem.wallet.R
import dagger.hilt.android.testing.HiltAndroidTest
import io.github.kakaocup.kakao.common.utilities.getResourceString
import io.qameta.allure.kotlin.AllureId
import io.qameta.allure.kotlin.junit4.DisplayName
import org.junit.Test

@HiltAndroidTest
class AzeroWarningsTest : BaseTestCase() {
    private val tokenName = "Aleph Zero"
    private val amountToLeaveLessThanDeposit = "0.099876587544"
    private val amountToLeaveGreaterThanDeposit = "0.09"
    private val depositAmount = "AZERO 0.0000000005"

    private val warningTitle = getResourceString(R.string.send_notification_existential_deposit_title)
    private val warningMessage = getResourceString(
        R.string.send_notification_existential_deposit_text, depositAmount
    )

    @AllureId("4290")
    @DisplayName("Warnings: check deposit warnings in Azero")
    @Test
    fun checkWarning() {
        setupHooks(
            additionalAfterSection = {
                resetWireMockScenarioState(USER_TOKENS_API_SCENARIO)
                resetWireMockScenarioState(QUOTES_API_SCENARIO)
            }
        ).run {
            step("Open 'Send Screen' with token: $tokenName") {
                openSendScreen(tokenName)
            }
            step("Type '$amountToLeaveLessThanDeposit' in input text field") {
                onSendScreen {
                    amountInputTextField.performClick()
                    amountInputTextField.performTextReplacement(amountToLeaveLessThanDeposit)
                }
            }
            step("Click on 'Next' button") {
                onSendScreen { nextButton.clickWithAssertion() }
            }
            step("Type address in input text field") {
                onSendAddressScreen { addressTextField.performTextReplacement(AZERO_RECIPIENT_ADDRESS) }
            }
            step("Click on 'Next' button") {
                onSendAddressScreen { nextButton.clickWithAssertion() }
            }
            step("Assert 'Existential deposit warning' is displayed") {
                checkSendWarning(
                    title = warningTitle,
                    message = warningMessage,
                )
            }
            step("Click on 'Leave $depositAmount' button") {
                onSendConfirmScreen { leaveDepositButton(depositAmount).clickWithAssertion() }
            }
            step("Assert 'Existential deposit warning' is not displayed") {
                checkSendWarning(
                    title = warningTitle,
                    message = warningMessage,
                    isDisplayed = false
                )
            }
            step("Click on 'Amount' field") {
                onSendConfirmScreen { primaryAmount.clickWithAssertion() }
            }
            step("Type '$amountToLeaveGreaterThanDeposit' in input text field") {
                onSendScreen {
                    amountInputTextField.performClick()
                    amountInputTextField.performTextReplacement(amountToLeaveGreaterThanDeposit)
                }
            }
            step("Click on 'Continue' button") {
                onSendScreen { continueButton.clickWithAssertion() }
            }
            step("Assert 'Existential deposit warning' is not displayed") {
                checkSendWarning(
                    title = warningTitle,
                    message = warningMessage,
                    isDisplayed = false
                )
            }
        }
    }
}