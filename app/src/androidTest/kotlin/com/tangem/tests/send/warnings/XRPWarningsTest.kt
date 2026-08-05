package com.tangem.tests.send.warnings

import com.tangem.common.BaseTestCase
import com.tangem.common.constants.TestConstants.QUOTES_API_SCENARIO
import com.tangem.common.constants.TestConstants.USER_TOKENS_API_SCENARIO
import com.tangem.common.constants.TestConstants.XRP_ACTIVATED_RECIPIENT_ADDRESS
import com.tangem.common.constants.TestConstants.XRP_NON_ACTIVATED_RECIPIENT_ADDRESS
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
class XRPWarningsTest : BaseTestCase() {
    private val tokenName = "XRP Ledger"
    private val mockStateName = "XRP"
    private val lessThanReserveAmount = "0.5"
    private val equalToReserveAmount = "1"
    private val reserveAmount = "XRP 1.00"
    private val greaterThanReserveAmount = "2"

    private val warningTitle =
        getResourceString(R.string.send_notification_invalid_reserve_amount_title, reserveAmount)
    private val warningMessage = getResourceString(R.string.send_notification_invalid_reserve_amount_text)

    @AllureId("4255")
    @DisplayName("Warnings: check warning, when sending less than reserve")
    @Test
    fun checkWarningWhenSendingLessThanReserve() {
        // Other tests leave ripple_custom_derivation in a state where the non-activated address is funded.
        val rippleCustomDerivationScenario = "ripple_custom_derivation"
        val rippleAccountInfoScenario = "ripple_account_info"
        val rippleAccountLinesScenario = "ripple_account_lines"

        setupHooks(
            additionalAfterSection = {
                resetWireMockScenarioState(USER_TOKENS_API_SCENARIO)
                resetWireMockScenarioState(QUOTES_API_SCENARIO)
                resetWireMockScenarioState(rippleCustomDerivationScenario)
                resetWireMockScenarioState(rippleAccountInfoScenario)
                resetWireMockScenarioState(rippleAccountLinesScenario)
            }
        ).run {
            step("Reset WireMock scenario: '$rippleCustomDerivationScenario' to its initial state") {
                resetWireMockScenarioState(rippleCustomDerivationScenario)
            }
            step("Reset WireMock scenario: '$rippleAccountInfoScenario' to its initial state") {
                resetWireMockScenarioState(rippleAccountInfoScenario)
            }
            step("Reset WireMock scenario: '$rippleAccountLinesScenario' to its initial state") {
                resetWireMockScenarioState(rippleAccountLinesScenario)
            }
            step("Open 'Send Screen' with token: $tokenName") {
                openSendScreen(tokenName, mockStateName)
            }
            step("Type '$lessThanReserveAmount' in input text field") {
                onSendScreen {
                    amountInputTextField.performClick()
                    amountInputTextField.performTextReplacement(lessThanReserveAmount)
                }
            }
            step("Click on 'Next' button") {
                onSendScreen { nextButton.clickWithAssertion() }
            }
            step("Type non activated address in input text field") {
                onSendAddressScreen { addressTextField.performTextReplacement(XRP_NON_ACTIVATED_RECIPIENT_ADDRESS) }
            }
            step("Click on 'Next' button") {
                onSendAddressScreen { nextButton.clickWithAssertion() }
            }
            step("Assert 'Invalid reserve amount warning' is displayed") {
                checkSendWarning(
                    title = warningTitle,
                    message = warningMessage
                )
            }
            step("Click on 'Address' field") {
                onSendConfirmScreen { recipientAddress(XRP_NON_ACTIVATED_RECIPIENT_ADDRESS).clickWithAssertion() }
            }
            step("Type an activated address in input text field") {
                onSendAddressScreen { addressTextField.performTextReplacement(XRP_ACTIVATED_RECIPIENT_ADDRESS) }
            }
            step("Click on 'Next' button") {
                onSendScreen { continueButton.clickWithAssertion() }
            }
            step("Assert 'Invalid reserve amount warning' is not displayed") {
                checkSendWarning(
                    title = warningTitle,
                    message = warningMessage,
                    isDisplayed = false
                )
            }
        }
    }

    @AllureId("4285")
    @DisplayName("Warnings: check warning when sending amount equal to reserve")
    @Test
    fun checkWarningWhenSendingAmountEqualToReserve() {
        // Other tests leave ripple_custom_derivation in a state where the non-activated address is funded.
        val rippleCustomDerivationScenario = "ripple_custom_derivation"
        val rippleAccountInfoScenario = "ripple_account_info"
        val rippleAccountLinesScenario = "ripple_account_lines"

        setupHooks(
            additionalAfterSection = {
                resetWireMockScenarioState(USER_TOKENS_API_SCENARIO)
                resetWireMockScenarioState(QUOTES_API_SCENARIO)
                resetWireMockScenarioState(rippleCustomDerivationScenario)
                resetWireMockScenarioState(rippleAccountInfoScenario)
                resetWireMockScenarioState(rippleAccountLinesScenario)
            }
        ).run {
            step("Reset WireMock scenario: '$rippleCustomDerivationScenario' to its initial state") {
                resetWireMockScenarioState(rippleCustomDerivationScenario)
            }
            step("Reset WireMock scenario: '$rippleAccountInfoScenario' to its initial state") {
                resetWireMockScenarioState(rippleAccountInfoScenario)
            }
            step("Reset WireMock scenario: '$rippleAccountLinesScenario' to its initial state") {
                resetWireMockScenarioState(rippleAccountLinesScenario)
            }
            step("Open 'Send Screen' with token: $tokenName") {
                openSendScreen(tokenName, mockStateName)
            }
            step("Type '$equalToReserveAmount' in input text field") {
                onSendScreen {
                    amountInputTextField.performClick()
                    amountInputTextField.performTextReplacement(equalToReserveAmount)
                }
            }
            step("Click on 'Next' button") {
                onSendScreen { nextButton.clickWithAssertion() }
            }
            step("Type non activated address in input text field") {
                onSendAddressScreen { addressTextField.performTextReplacement(XRP_NON_ACTIVATED_RECIPIENT_ADDRESS) }
            }
            step("Click on 'Next' button") {
                onSendAddressScreen { nextButton.clickWithAssertion() }
            }
            step("Assert 'Invalid reserve amount warning' is not displayed") {
                checkSendWarning(
                    title = warningTitle,
                    message = warningMessage,
                    isDisplayed = false
                )
            }
            step("Click on 'Address' field") {
                onSendConfirmScreen { recipientAddress(XRP_NON_ACTIVATED_RECIPIENT_ADDRESS).clickWithAssertion() }
            }
            step("Type an activated address in input text field") {
                onSendAddressScreen { addressTextField.performTextReplacement(XRP_ACTIVATED_RECIPIENT_ADDRESS) }
            }
            step("Click on 'Next' button") {
                onSendScreen { continueButton.clickWithAssertion() }
            }
            step("Assert 'Invalid reserve amount warning' is not displayed") {
                checkSendWarning(
                    title = warningTitle,
                    message = warningMessage,
                    isDisplayed = false
                )
            }
        }
    }

    @AllureId("4284")
    @DisplayName("Warnings: check warning when sending greater than reserve")
    @Test
    fun checkWarningWhenSendingGreaterThanReserve() {
        // Other tests leave ripple_custom_derivation in a state where the non-activated address is funded.
        val rippleCustomDerivationScenario = "ripple_custom_derivation"
        val rippleAccountInfoScenario = "ripple_account_info"
        val rippleAccountLinesScenario = "ripple_account_lines"

        setupHooks(
            additionalAfterSection = {
                resetWireMockScenarioState(USER_TOKENS_API_SCENARIO)
                resetWireMockScenarioState(QUOTES_API_SCENARIO)
                resetWireMockScenarioState(rippleCustomDerivationScenario)
                resetWireMockScenarioState(rippleAccountInfoScenario)
                resetWireMockScenarioState(rippleAccountLinesScenario)
            }
        ).run {
            step("Reset WireMock scenario: '$rippleCustomDerivationScenario' to its initial state") {
                resetWireMockScenarioState(rippleCustomDerivationScenario)
            }
            step("Reset WireMock scenario: '$rippleAccountInfoScenario' to its initial state") {
                resetWireMockScenarioState(rippleAccountInfoScenario)
            }
            step("Reset WireMock scenario: '$rippleAccountLinesScenario' to its initial state") {
                resetWireMockScenarioState(rippleAccountLinesScenario)
            }
            step("Open 'Send Screen' with token: $tokenName") {
                openSendScreen(tokenName, mockStateName)
            }
            step("Type '$greaterThanReserveAmount' in input text field") {
                onSendScreen {
                    amountInputTextField.performClick()
                    amountInputTextField.performTextReplacement(greaterThanReserveAmount)
                }
            }
            step("Click on 'Next' button") {
                onSendScreen { nextButton.clickWithAssertion() }
            }
            step("Type non activated address in input text field") {
                onSendAddressScreen { addressTextField.performTextReplacement(XRP_NON_ACTIVATED_RECIPIENT_ADDRESS) }
            }
            step("Click on 'Next' button") {
                onSendAddressScreen { nextButton.clickWithAssertion() }
            }
            step("Assert 'Invalid reserve amount warning' is not displayed") {
                checkSendWarning(
                    title = warningTitle,
                    message = warningMessage,
                    isDisplayed = false
                )
            }
            step("Click on 'Address' field") {
                onSendConfirmScreen { recipientAddress(XRP_NON_ACTIVATED_RECIPIENT_ADDRESS).clickWithAssertion() }
            }
            step("Type an activated address in input text field") {
                onSendAddressScreen { addressTextField.performTextReplacement(XRP_ACTIVATED_RECIPIENT_ADDRESS) }
            }
            step("Click on 'Next' button") {
                onSendScreen { continueButton.clickWithAssertion() }
            }
            step("Assert 'Invalid reserve amount warning' is not displayed") {
                checkSendWarning(
                    title = warningTitle,
                    message = warningMessage,
                    isDisplayed = false
                )
            }
        }
    }
}