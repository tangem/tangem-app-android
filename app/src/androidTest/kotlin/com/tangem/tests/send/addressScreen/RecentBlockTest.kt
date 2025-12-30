package com.tangem.tests.send.addressScreen

import com.tangem.common.BaseTestCase
import com.tangem.common.constants.TestConstants.DOGECOIN_ADDRESS
import com.tangem.common.constants.TestConstants.QUOTES_API_SCENARIO
import com.tangem.common.constants.TestConstants.USER_TOKENS_API_SCENARIO
import com.tangem.common.extensions.SwipeDirection
import com.tangem.common.extensions.clickWithAssertion
import com.tangem.common.extensions.swipeVertical
import com.tangem.common.utils.resetWireMockScenarioState
import com.tangem.common.utils.setWireMockScenarioState
import com.tangem.scenarios.*
import com.tangem.screens.onSendAddressScreen
import com.tangem.screens.onSendConfirmScreen
import com.tangem.screens.onSendScreen
import dagger.hilt.android.testing.HiltAndroidTest
import io.qameta.allure.kotlin.AllureId
import io.qameta.allure.kotlin.junit4.DisplayName
import org.junit.Test

@HiltAndroidTest
class RecentBlockTest : BaseTestCase() {
    private val txHistoryScenarioName = "dogecoin_tx_history"
    private val recentTransactionAmount1 = "DOGE 0.0192"
    private val recentTransactionAmount2 = "DOGE 0.10"
    private val recentTransactionAmount3 = "DOGE 0.005"

    @AllureId("4005")
    @DisplayName("Send (address screen): check address history")
    @Test
    fun sendAddressHistoryTest() {
        val tokenName = "Dogecoin"
        val sendAmount = "1"
        val txHistoryScenarioState = "OutgoingTransaction"

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
            step("Open 'Send Address' screen") {
                openSendAddressScreen(tokenName, sendAmount)
            }
            step("Click on previous address") {
                onSendAddressScreen { recentAddressItem(DOGECOIN_ADDRESS).performClick() }
            }
            step("Assert recipient address is displayed") {
                onSendConfirmScreen { recipientAddress(DOGECOIN_ADDRESS).assertIsDisplayed() }
            }
            step("Click on top bar 'Close' button") {
                waitForIdle()
                onSendConfirmScreen { closeButton.clickWithAssertion() }
            }
            step("Assert 'Send Address' screen is displayed") {
                waitForIdle()
                onSendAddressScreen { container.assertIsDisplayed() }
            }
            step("Assert address text field is displayed") {
                onSendAddressScreen { addressTextField.assertIsDisplayed() }
            }
            step("Assert address text field contains correct recipient address") {
                onSendAddressScreen { addressTextField.assertTextContains(DOGECOIN_ADDRESS) }
            }
            step("Assert 'Recent' title is not displayed") {
                onSendAddressScreen { recentAddressesTitle.assertIsNotDisplayed() }
            }
            step("Assert 'Next' button is enabled") {
                onSendAddressScreen { nextButton.assertIsEnabled() }
            }
            step("Click on 'Cross' button") {
                onSendAddressScreen { clearTextFieldButton.clickWithAssertion() }
            }
            step("Assert recipient text field is empty") {
                onSendAddressScreen { addressTextField.assertTextEquals("") }
            }
            step("Assert 'Recent' title is displayed") {
                onSendAddressScreen { recentAddressesTitle.assertIsDisplayed() }
            }
            step("Assert 'Next' button is disabled") {
                onSendAddressScreen { nextButton.assertIsNotEnabled() }
            }
        }
    }

    @AllureId("4597")
    @DisplayName("Send (address screen): check 'Recent' block, transaction history doesn't supported")
    @Test
    fun recentBlockTransactionHistoryDoesNotSupportedTest() {
        val tokenName = "Polkadot"
        val sendAmount = "1"

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
            step("Assert 'Send Address' container is displayed") {
                onSendAddressScreen { container.assertIsDisplayed() }
            }
            step("Assert 'Recent' title is not displayed") {
                onSendAddressScreen { recentAddressesTitle.assertIsNotDisplayed() }
            }
        }
    }

    @AllureId("4596")
    @DisplayName("Send (address screen): check 'Recent' block, transaction history is empty")
    @Test
    fun recentBlockTransactionHistoryEmptyTest() {
        val tokenName = "Dogecoin"
        val sendAmount = "1"
        val txHistoryScenarioState = "Empty"

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
            step("Open 'Send Address' screen") {
                openSendAddressScreen(tokenName, sendAmount)
            }
            step("Assert 'Recent' title is not displayed") {
                onSendAddressScreen { recentAddressesTitle.assertIsNotDisplayed() }
            }
            step("Assert recent address item is not displayed") {
                onSendAddressScreen { recentAddressItem(DOGECOIN_ADDRESS).assertIsNotDisplayed() }
            }
        }
    }

    @AllureId("4598")
    @DisplayName("Send (address screen): check 'Recent' block, transaction history does not load")
    @Test
    fun recentBlockTransactionHistoryDoesNotLoadTest() {
        val tokenName = "Dogecoin"
        val sendAmount = "1"
        val txHistoryScenarioState = "Error"
        val scenarioState = "Dogecoin"

        setupHooks(
            additionalAfterSection = {
                resetWireMockScenarioState(USER_TOKENS_API_SCENARIO)
                resetWireMockScenarioState(QUOTES_API_SCENARIO)
                resetWireMockScenarioState(txHistoryScenarioName)
            }
        ).run {
            step("Set WireMock scenario: '$USER_TOKENS_API_SCENARIO' to state: '$scenarioState'") {
                setWireMockScenarioState(scenarioName = USER_TOKENS_API_SCENARIO, state = scenarioState)
            }
            step("Set WireMock scenario: '$QUOTES_API_SCENARIO' to state: '$scenarioState'") {
                setWireMockScenarioState(scenarioName = QUOTES_API_SCENARIO, state = scenarioState)
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
            step("Open 'Send Address' screen") {
                openSendAddressScreen(tokenName, sendAmount)
            }
            step("Assert 'Recent' title is not displayed") {
                onSendAddressScreen { recentAddressesTitle.assertIsNotDisplayed() }
            }
            step("Assert recent address item is not displayed") {
                onSendAddressScreen { recentAddressItem(DOGECOIN_ADDRESS).assertIsNotDisplayed() }
            }
        }
    }

    @AllureId("4572")
    @DisplayName("Send (address screen): check 'Recent' block for >10 transactions")
    @Test
    fun checkRecentBlockMoreThanTenTransactionsTest() {
        val tokenName = "Dogecoin"
        val sendAmount = "1"
        val txHistoryScenarioState = "11OutgoingTransactions"
        val recipientAddressBase = "DJ2TaZ5vvp3mBLugUpKjVM3pRBLi4uYaq"
        val shortenedRecipientAddress = "DJ2TaZ5vvp3mBLugU...Li4uYaq123456789b"

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
            step("Open 'Send Address' screen") {
                openSendAddressScreen(tokenName, sendAmount)
            }
            step("Assert 'Recent' title is displayed") {
                onSendAddressScreen { recentAddressesTitle.assertIsDisplayed() }
            }
            step("Check recent address item №1") {
                checkRecentAddressItem(address = DOGECOIN_ADDRESS, description = recentTransactionAmount1)
            }
            step("Check recent address item №2") {
                checkRecentAddressItem(address = shortenedRecipientAddress, description = recentTransactionAmount2)
            }
            step("Check recent address item №3") {
                checkRecentAddressItem(address = recipientAddressBase + "k", description = recentTransactionAmount2)
            }
            step("Check recent address item №4") {
                checkRecentAddressItem(address = recipientAddressBase + "c", description = recentTransactionAmount2)
            }
            step("Check recent address item №5") {
                checkRecentAddressItem(address = recipientAddressBase + "d", description = recentTransactionAmount2)
            }
            step("Check recent address item №6") {
                checkRecentAddressItem(address = recipientAddressBase + "e", description = recentTransactionAmount2)
            }
            step("Swipe up") {
                waitForIdle()
                onSendAddressScreen {
                    swipeVertical(SwipeDirection.UP, startHeightRatio = 0.6f)
                    swipeVertical(SwipeDirection.UP, startHeightRatio = 0.6f)
                }
            }
            step("Check recent address item №7") {
                checkRecentAddressItem(address = recipientAddressBase + "f", description = recentTransactionAmount2)
            }
            step("Check recent address item №8") {
                checkRecentAddressItem(address = recipientAddressBase + "g", description = recentTransactionAmount2)
            }
            step("Check recent address item №9") {
                checkRecentAddressItem(address = recipientAddressBase + "o", description = recentTransactionAmount2)
            }
            step("Check recent address item №10") {
                checkRecentAddressItem(address = recipientAddressBase + "p", description = recentTransactionAmount2)
            }
            step("Assert recent address item №11 is not displayed") {
                onSendAddressScreen { recentAddressItem(recipientAddressBase + "a").assertIsNotDisplayed() }
            }
            step("Click on recent address item №1") {
                onSendAddressScreen { recentAddressItem(DOGECOIN_ADDRESS).performClick() }
            }
            step("Assert recipient address is displayed on 'Send Confirm' screen") {
                onSendConfirmScreen { recipientAddress(DOGECOIN_ADDRESS).assertIsDisplayed() }
            }
        }
    }

    @AllureId("4676")
    @DisplayName("Send (address screen): check 'Recent' block for <10 transactions")
    @Test
    fun checkRecentBlockLessThanTenTransactionsTest() {
        val tokenName = "Dogecoin"
        val sendAmount = "1"
        val txHistoryScenarioState = "2OutgoingTransactions"

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
            step("Open 'Send Address' screen") {
                openSendAddressScreen(tokenName, sendAmount)
            }
            step("Assert 'Recent' title is displayed") {
                onSendAddressScreen { recentAddressesTitle.assertIsDisplayed() }
            }
            step("Check recent address item №1") {
                checkRecentAddressItem(address = DOGECOIN_ADDRESS, description = recentTransactionAmount1)
            }
            step("Check recent address item №2") {
                checkRecentAddressItem(address = DOGECOIN_ADDRESS, description = recentTransactionAmount3)
            }
            step("Click on recent address item") {
                onSendAddressScreen { recentAddressItem(DOGECOIN_ADDRESS).performClick() }
            }
            step("Assert recipient address is displayed on 'Send Confirm' screen") {
                onSendConfirmScreen { recipientAddress(DOGECOIN_ADDRESS).assertIsDisplayed() }
            }
        }
    }
}