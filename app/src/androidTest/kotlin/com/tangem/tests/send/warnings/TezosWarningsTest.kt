package com.tangem.tests.send.warnings

import com.tangem.common.BaseTestCase
import com.tangem.common.constants.TestConstants.QUOTES_API_SCENARIO
import com.tangem.common.constants.TestConstants.TEZOS_RECIPIENT_ADDRESS
import com.tangem.common.constants.TestConstants.USER_TOKENS_API_SCENARIO
import com.tangem.common.extensions.SwipeDirection
import com.tangem.common.extensions.clickWithAssertion
import com.tangem.common.extensions.swipeVertical
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
class TezosWarningsTest : BaseTestCase() {
    private val tokenName = "Tezos"
    private val reduceAmount = "0.000001"
    private val sendAmount = "0.01"

    private val feeIsHighTitle = getResourceString(R.string.send_notification_high_fee_title)
    private val feeIsHighMessage =
        getResourceString(R.string.send_notification_high_fee_text, tokenName, reduceAmount)

    @AllureId("4229")
    @DisplayName("Warnings: warning is displayed when sending max amount")
    @Test
    fun warningIsDisplayedWhenSendMaxAmount() {
        setupHooks(
            additionalAfterSection = {
                resetWireMockScenarioState(USER_TOKENS_API_SCENARIO)
                resetWireMockScenarioState(QUOTES_API_SCENARIO)
            }
        ).run {
            step("Open 'Send Screen' with token: $tokenName") {
                openSendScreen(tokenName)
            }
            step("Click 'Max amount' button") {
                onSendScreen {
                    maxButton.clickWithAssertion()
                }
            }
            step("Click on 'Next' button") {
                onSendScreen { nextButton.clickWithAssertion() }
            }
            step("Type address in input text field") {
                onSendAddressScreen { addressTextField.performTextReplacement(TEZOS_RECIPIENT_ADDRESS) }
            }
            step("Click on 'Next' button") {
                onSendAddressScreen { nextButton.clickWithAssertion() }
            }
            step("Swipe up to see the warning") {
                swipeVertical(SwipeDirection.UP)
            }
            step("Assert 'Fee is high warning' is displayed") {
                checkSendWarning(
                    title = feeIsHighTitle,
                    message = feeIsHighMessage,
                    sendButtonIsDisabled = false
                )
            }
            step("Click on 'Reduce' button") {
                onSendConfirmScreen { reduceAmountButton(reduceAmount).clickWithAssertion() }
            }
            step("Assert 'Fee is high warning' is not displayed") {
                checkSendWarning(
                    title = feeIsHighTitle,
                    message = feeIsHighMessage,
                    isDisplayed = false
                )
            }
        }
    }

    @AllureId("4230")
    @DisplayName("Warnings: warning is not displayed when sending not max amount")
    @Test
    fun warningIsNotDisplayedWhenSendNotMaxAmount() {
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
                onSendAddressScreen { addressTextField.performTextReplacement(TEZOS_RECIPIENT_ADDRESS) }
            }
            step("Click on 'Next' button") {
                onSendAddressScreen { nextButton.clickWithAssertion() }
            }
            step("Assert 'Fee is high warning' is not displayed") {
                checkSendWarning(
                    title = feeIsHighTitle,
                    message = feeIsHighMessage,
                    isDisplayed = false
                )
            }
        }
    }
}