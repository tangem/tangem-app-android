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
class KaspaWarningsTest : BaseTestCase() {
    private val tokenName = "Kaspa"
    private val amountToSendUsingLessThanUTXOLimit = "8.0"
    private val amountToSendUsingMoreThanUTXOLimit = "8.7"
    private val amountToSendUsingExactlyUTXOLimit = "8.19968"
    private val utxoLimitAmount = "84"
    private val availableToSendAmount = "KAS 8.19968"

    private val kaspaUTXOScenarioName = "kaspa_utxo"
    private val moreThanLimitState = "more_than_84_android"

    private val warningTitle = getResourceString(R.string.send_notification_transaction_limit_title)
    private val warningMessage = getResourceString(
        R.string.send_notification_transaction_limit_text,
        tokenName, utxoLimitAmount, availableToSendAmount
    )

    @AllureId("4223")
    @DisplayName("Warnings: check kaspa utxo warning, when sending less than 84 utxo")
    @Test
    fun checkWarningWhenSendingLessThanUTXOLimit() {
        setupHooks(
            additionalAfterSection = {
                resetWireMockScenarioState(USER_TOKENS_API_SCENARIO)
                resetWireMockScenarioState(QUOTES_API_SCENARIO)
            }
        ).run {
            step("Set WireMock scenario: '$kaspaUTXOScenarioName' to state: '$moreThanLimitState'") {
                setWireMockScenarioState(scenarioName = kaspaUTXOScenarioName, state = moreThanLimitState)
            }
            step("Open 'Send Screen' with token: $tokenName") {
                openSendScreen(tokenName)
            }
            step("Type '$amountToSendUsingLessThanUTXOLimit' in input text field") {
                onSendScreen {
                    amountInputTextField.performClick()
                    amountInputTextField.performTextReplacement(amountToSendUsingLessThanUTXOLimit)
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
            step("Assert 'UTXO limit warning' is NOT displayed") {
                checkSendWarning(
                    title = warningTitle,
                    message = warningMessage,
                    isDisplayed = false
                )
            }
        }
    }

    @AllureId("4225")
    @DisplayName("Warnings: check kaspa utxo warning, when sending more than 84 utxo")
    @Test
    fun checkWarningWhenSendingMoreThanUTXOLimit() {
        setupHooks(
            additionalAfterSection = {
                resetWireMockScenarioState(USER_TOKENS_API_SCENARIO)
                resetWireMockScenarioState(QUOTES_API_SCENARIO)
            }
        ).run {
            step("Set WireMock scenario: '$kaspaUTXOScenarioName' to state: '$moreThanLimitState'") {
                setWireMockScenarioState(scenarioName = kaspaUTXOScenarioName, state = moreThanLimitState)
            }
            step("Open 'Send Screen' with token: $tokenName") {
                openSendScreen(tokenName)
            }
            step("Type '$amountToSendUsingMoreThanUTXOLimit' in input text field") {
                onSendScreen {
                    amountInputTextField.performClick()
                    amountInputTextField.performTextReplacement(amountToSendUsingMoreThanUTXOLimit)
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
            step("Assert 'UTXO limit warning' is displayed") {
                checkSendWarning(
                    title = warningTitle,
                    message = warningMessage
                )
            }
        }
    }

    @AllureId("4224")
    @DisplayName("Warnings: check kaspa utxo warning, when sending exactly 84 utxo")
    @Test
    fun checkWarningWhenSendingEqualToUTXOLimit() {
        setupHooks(
            additionalAfterSection = {
                resetWireMockScenarioState(USER_TOKENS_API_SCENARIO)
                resetWireMockScenarioState(QUOTES_API_SCENARIO)
            }
        ).run {
            step("Set WireMock scenario: '$kaspaUTXOScenarioName' to state: '$moreThanLimitState'") {
                setWireMockScenarioState(scenarioName = kaspaUTXOScenarioName, state = moreThanLimitState)
            }
            step("Open 'Send Screen' with token: $tokenName") {
                openSendScreen(tokenName)
            }
            step("Type '$amountToSendUsingExactlyUTXOLimit' in input text field") {
                onSendScreen {
                    amountInputTextField.performClick()
                    amountInputTextField.performTextReplacement(amountToSendUsingExactlyUTXOLimit)
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
            step("Assert 'UTXO limit warning' is displayed") {
                checkSendWarning(
                    title = warningTitle,
                    message = warningMessage,
                    isDisplayed = false
                )
            }
        }
    }
}
