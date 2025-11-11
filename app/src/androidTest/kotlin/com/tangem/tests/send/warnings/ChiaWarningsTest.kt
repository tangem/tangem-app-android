package com.tangem.tests.send.warnings

import com.tangem.common.BaseTestCase
import com.tangem.common.constants.TestConstants.CHIA_RECIPIENT_ADDRESS
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
import org.junit.Ignore
import org.junit.Test

@HiltAndroidTest
@Ignore("[REDACTED_JIRA]")
class ChiaWarningsTest : BaseTestCase() {
    private val tokenName = "Chia Network"
    private val mockStateName = "Chia"
    private val amountToSendUsingLessThanUTXOLimit = "20"
    private val amountToSendUsingMoreThanUTXOLimit = "51"
    private val amountToSendUsingExactlyUTXOLimit = "50"
    private val utxoLimitAmount = "50"

    private val chiaUTXOScenarioName = "chia_get_coin_records"
    private val moreThanLimitState = "more_than_50_android"

    private val warningTitle = getResourceString(R.string.send_notification_transaction_limit_title)
    private val warningMessage = getResourceString(
        R.string.send_notification_transaction_limit_text,
        tokenName, utxoLimitAmount, utxoLimitAmount
    )

    @AllureId("4226")
    @DisplayName("Warnings: check warning, when sending less than 50 utxo")
    @Test
    fun checkWarningWhenSendingLessThanUTXOLimit() {
        setupHooks(
            additionalAfterSection = {
                resetWireMockScenarioState(USER_TOKENS_API_SCENARIO)
                resetWireMockScenarioState(QUOTES_API_SCENARIO)
            }
        ).run {
            step("Set WireMock scenario: '$chiaUTXOScenarioName' to state: '$moreThanLimitState'") {
                setWireMockScenarioState(scenarioName = chiaUTXOScenarioName, state = moreThanLimitState)
            }
            step("Open 'Send Screen' with token: $tokenName") {
                openSendScreen(tokenName, mockStateName)
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
                onSendAddressScreen { addressTextField.performTextReplacement(CHIA_RECIPIENT_ADDRESS) }
            }
            step("Click on 'Next' button") {
                onSendAddressScreen { nextButton.clickWithAssertion() }
            }
            step("Assert 'UTXO limit warning' is not displayed") {
                checkSendWarning(
                    title = warningTitle,
                    message = warningMessage,
                    isDisplayed = false
                )
            }
        }
    }

    @AllureId("4227")
    @DisplayName("Warnings: check warning is not displayed, when sending exactly 50 utxo")
    @Test
    fun checkWarningWhenSendingEqualToUTXOLimit() {
        setupHooks(
            additionalAfterSection = {
                resetWireMockScenarioState(USER_TOKENS_API_SCENARIO)
                resetWireMockScenarioState(QUOTES_API_SCENARIO)
            }
        ).run {
            step("Set WireMock scenario: '$chiaUTXOScenarioName' to state: '$moreThanLimitState'") {
                setWireMockScenarioState(scenarioName = chiaUTXOScenarioName, state = moreThanLimitState)
            }
            step("Open 'Send Screen' with token: $tokenName") {
                openSendScreen(tokenName, mockStateName)
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
                onSendAddressScreen { addressTextField.performTextReplacement(CHIA_RECIPIENT_ADDRESS) }
            }
            step("Click on 'Next' button") {
                onSendAddressScreen { nextButton.clickWithAssertion() }
            }
            step("Assert 'UTXO limit warning' is not displayed") {
                checkSendWarning(
                    title = warningTitle,
                    message = warningMessage,
                    isDisplayed = false
                )
            }
        }
    }

    @AllureId("4228")
    @DisplayName("Warnings: check warning is not displayed, when sending more than 50 utxo")
    @Test
    fun checkWarningWhenSendingMoreThanUTXOLimit() {
        setupHooks(
            additionalAfterSection = {
                resetWireMockScenarioState(USER_TOKENS_API_SCENARIO)
                resetWireMockScenarioState(QUOTES_API_SCENARIO)
            }
        ).run {
            step("Set WireMock scenario: '$chiaUTXOScenarioName' to state: '$moreThanLimitState'") {
                setWireMockScenarioState(scenarioName = chiaUTXOScenarioName, state = moreThanLimitState)
            }
            step("Open 'Send Screen' with token: $tokenName") {
                openSendScreen(tokenName, mockStateName)
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
                onSendAddressScreen { addressTextField.performTextReplacement(CHIA_RECIPIENT_ADDRESS) }
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
}