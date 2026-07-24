package com.tangem.tests.tokenDetails

import com.tangem.common.BaseTestCase
import com.tangem.common.constants.TestConstants.DOGECOIN_RECIPIENT_ADDRESS
import com.tangem.common.constants.TestConstants.QUOTES_API_SCENARIO
import com.tangem.common.constants.TestConstants.USER_TOKENS_API_SCENARIO
import com.tangem.common.constants.TestConstants.WAIT_UNTIL_TIMEOUT_LONG
import com.tangem.common.extensions.clickWithAssertion
import com.tangem.common.utils.resetWireMockScenarioState
import com.tangem.common.utils.setWireMockScenarioState
import com.tangem.core.ui.R
import com.tangem.scenarios.openMainScreen
import com.tangem.scenarios.synchronizeAddresses
import com.tangem.screens.onMainScreen
import com.tangem.screens.onTokenDetailsScreen
import com.tangem.screens.onTxHistoryScreen
import com.tangem.utils.toBriefAddressFormat
import dagger.hilt.android.testing.HiltAndroidTest
import io.github.kakaocup.kakao.common.utilities.getResourceString
import io.qameta.allure.kotlin.AllureId
import io.qameta.allure.kotlin.junit4.DisplayName
import org.junit.Test

@HiltAndroidTest
class TokenDetailsTests : BaseTestCase() {

    private val txHistoryScenarioName = "dogecoin_tx_history"

    @AllureId("304")
    @DisplayName("Token details: active outgoing transaction block")
    @Test
    fun activeOutgoingTransactionBlockTest() {
        val tokenName = "Dogecoin"
        val currencySymbol = "DOGE"
        val txHistoryScenarioState = "UnconfirmedOutgoing"
        val sendingTitle = getResourceString(R.string.common_sending)
        val recipientBriefAddress = DOGECOIN_RECIPIENT_ADDRESS.toBriefAddressFormat()

        setupHooks(
            additionalAfterSection = {
                resetWireMockScenarioState(USER_TOKENS_API_SCENARIO)
                resetWireMockScenarioState(QUOTES_API_SCENARIO)
                resetWireMockScenarioState(txHistoryScenarioName)
            }
        ).run {
            step("Set WireMock scenario: '$USER_TOKENS_API_SCENARIO' to state: '$tokenName'") {
                setWireMockScenarioState(scenarioName = USER_TOKENS_API_SCENARIO, state = tokenName)
            }
            step("Set WireMock scenario: '$QUOTES_API_SCENARIO' to state: '$tokenName'") {
                setWireMockScenarioState(scenarioName = QUOTES_API_SCENARIO, state = tokenName)
            }
            step("Set WireMock scenario: '$txHistoryScenarioName' to state: '$txHistoryScenarioState'") {
                setWireMockScenarioState(scenarioName = txHistoryScenarioName, state = txHistoryScenarioState)
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
            step("Assert 'Token details' screen is displayed") {
                onTokenDetailsScreen { screenContainer.assertIsDisplayed() }
            }
            step("Assert active outgoing '$sendingTitle' transaction is displayed") {
                flakySafely(WAIT_UNTIL_TIMEOUT_LONG) {
                    onTxHistoryScreen { transactionItem(sendingTitle).assertIsDisplayed() }
                }
            }
            step("Assert active outgoing transaction status is unconfirmed") {
                onTxHistoryScreen { transactionUnconfirmedStatus(sendingTitle).assertIsDisplayed() }
            }
            step("Assert active outgoing transaction amount is displayed in '$currencySymbol'") {
                onTxHistoryScreen {
                    transactionAmount(sendingTitle).assertIsDisplayed()
                    transactionCurrency(sendingTitle).assertTextEquals(currencySymbol)
                }
            }
            step("Assert active outgoing transaction recipient address '$recipientBriefAddress' is displayed") {
                onTxHistoryScreen { transactionAddress(sendingTitle, recipientBriefAddress).assertIsDisplayed() }
            }
        }
    }
}