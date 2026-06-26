package com.tangem.tests.send.reasonBlock

import com.tangem.common.BaseTestCase
import com.tangem.common.extensions.clickWithAssertion
import com.tangem.common.utils.resetWireMockScenarioState
import com.tangem.common.utils.setWireMockScenarioState
import com.tangem.core.ui.R
import com.tangem.scenarios.openMainScreen
import com.tangem.scenarios.synchronizeAddresses
import com.tangem.screens.onDialog
import com.tangem.screens.onMainScreen
import com.tangem.screens.onTokenDetailsScreen
import com.tangem.screens.onTransferBottomSheet
import dagger.hilt.android.testing.HiltAndroidTest
import io.github.kakaocup.kakao.common.utilities.getResourceString
import io.qameta.allure.kotlin.AllureId
import io.qameta.allure.kotlin.junit4.DisplayName
import org.junit.Test

@HiltAndroidTest
class ReasonBlockTest : BaseTestCase() {

    @AllureId("3616")
    @DisplayName("Reason block: Send is unavailable if user has pending transaction")
    @Test
    fun reasonBlockSendUnavailableWithPendingTransactionTest() {
        val txHistoryScenarioName = "dogecoin_tx_history"
        val txHistoryState = "EmptyWithPendingTransaction"
        val walletsScenarioName = "user_tokens_api"
        val walletsState = "Dogecoin"
        val token = "Dogecoin"
        val reasonText = getResourceString(R.string.token_button_unavailability_reason_pending_transaction_send)
            .substringBefore("%s")

        setupHooks(
            additionalAfterSection = {
                resetWireMockScenarioState(txHistoryScenarioName)
                resetWireMockScenarioState(walletsScenarioName)
            }
        ).run {
            step("Set Wiremock scenario: $txHistoryScenarioName to state $txHistoryState") {
                setWireMockScenarioState(scenarioName = txHistoryScenarioName, state = txHistoryState)
            }
            step("Set Wiremock scenario: $walletsScenarioName to state $walletsState") {
                setWireMockScenarioState(scenarioName = walletsScenarioName, state = walletsState)
            }
            step("Open 'Main Screen'") {
                openMainScreen()
            }
            step("Synchronize addresses") {
                synchronizeAddresses()
            }
            step("Click on token with name $token") {
                onMainScreen { tokenWithTitleAndAddress(token).clickWithAssertion() }
            }
            step("Click on 'Transfer' button") {
                onTokenDetailsScreen { transferButton.clickWithAssertion() }
            }
            step("Verify 'Send' button is disabled") {
                onTransferBottomSheet { sendButton.assertIsNotEnabled() }
            }
            step("Click on 'Send' button") {
                onTransferBottomSheet { sendButton.clickWithAssertion() }
            }
            step("Assert pending-transaction reason dialog is displayed") {
                onDialog { containerWithText(reasonText).assertIsDisplayed() }
            }
        }
    }

    @AllureId("3615")
    @DisplayName("Reason block: Token withdrawal is unavailable if there are no fee coverage")
    @Test
    fun reasonBlockTokenWithdrawalUnavailableWithoutFeeCoverage() {
        val userWalletsScenarioName = "user_tokens_api"
        val userWalletsState = "SolanaUSDC"
        val solBalanceScenarioName = "GetAccountInfoSol"
        val solBalanceState = "ZeroBalance"
        val token = "USDC"
        val feeCurrencyName = "Solana"
        val feeCurrencySymbol = "SOL"

        setupHooks(
            additionalAfterSection = {
                resetWireMockScenarioState(userWalletsScenarioName)
                resetWireMockScenarioState(solBalanceScenarioName)
            }
        ).run {
            step("Set Wiremock scenario: $userWalletsScenarioName to state $userWalletsState") {
                setWireMockScenarioState(scenarioName = userWalletsScenarioName, state = userWalletsState)
            }
            step("Set Wiremock scenario: $solBalanceScenarioName to state $solBalanceState") {
                setWireMockScenarioState(scenarioName = solBalanceScenarioName, state = solBalanceState)
            }
            step("Open 'Main Screen'") {
                openMainScreen()
            }
            step("Synchronize addresses") {
                synchronizeAddresses()
            }
            step("Click on token with name $token") {
                onMainScreen { tokenWithTitleAndAddress(token).clickWithAssertion() }
            }
            step("Assert 'Insufficient $feeCurrencySymbol for fee' notification is displayed") {
                onTokenDetailsScreen {
                    networkFeeNotificationTitle(feeCurrencyName).assertIsDisplayed()
                    networkFeeNotificationMessage(
                        currencyName = token,
                        networkName = feeCurrencyName,
                        feeCurrencyName = feeCurrencyName,
                        feeCurrencySymbol = feeCurrencySymbol,
                    ).assertIsDisplayed()
                }
            }
            step("Click on 'Go to $feeCurrencySymbol' button") {
                onTokenDetailsScreen { goToBuyCurrencyButton(feeCurrencySymbol).clickWithAssertion() }
            }
            step("Assert $feeCurrencyName token screen is opened") {
                onTokenDetailsScreen { tokenTitle(feeCurrencyName).assertIsDisplayed() }
            }
        }
    }
}