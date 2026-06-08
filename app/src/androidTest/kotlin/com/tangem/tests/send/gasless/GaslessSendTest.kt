package com.tangem.tests.send.gasless

import com.tangem.common.BaseTestCase
import com.tangem.common.constants.TestConstants.ETHEREUM_RECIPIENT_ADDRESS
import com.tangem.common.constants.TestConstants.QUOTES_API_SCENARIO
import com.tangem.common.constants.TestConstants.SVS_SEED_PHRASE_12
import com.tangem.common.constants.TestConstants.USER_TOKENS_API_SCENARIO
import com.tangem.common.constants.TestConstants.WAIT_UNTIL_TIMEOUT_LONG
import com.tangem.common.extensions.clickWithAssertion
import com.tangem.common.utils.resetWireMockScenarioState
import com.tangem.common.utils.setWireMockScenarioState
import com.tangem.core.ui.R
import com.tangem.scenarios.*
import com.tangem.screens.*
import dagger.hilt.android.testing.HiltAndroidTest
import io.github.kakaocup.kakao.common.utilities.getResourceString
import io.qameta.allure.kotlin.AllureId
import io.qameta.allure.kotlin.junit4.DisplayName
import org.junit.Test

/**
 * Gasless send lifecycle: signing and broadcasting a stablecoin-fee transaction (hot wallet),
 * the max-amount fee reservation, and the completed gasless transaction in the token history.
 */
@HiltAndroidTest
class GaslessSendTest : BaseTestCase() {

    private val scenarioState = "PolygonUSDC"
    private val tokenName = "USDC"
    private val currencySymbol = "USDC"
    private val nativeTokenName = "Polygon"
    private val hotWalletTokensState = "PolygonUSDCHotWallet"
    private val tokenAmount = "1"

    @AllureId("5069")
    @DisplayName("Gasless: max amount reserves the stablecoin fee and stays sendable")
    @Test
    fun checkMaxAmountSendTest() {
        val feeCoverageTitle = getResourceString(R.string.send_network_fee_warning_title)
        val feeCoverageMessagePart = getResourceString(R.string.common_network_fee_warning_content, "", "")
            .substringBefore("(")
            .trim()

        setupHooks(
            additionalAfterSection = {
                resetWireMockScenarioState(USER_TOKENS_API_SCENARIO)
                resetWireMockScenarioState(QUOTES_API_SCENARIO)
            }
        ).run {
            step("Open the send flow for '$tokenName' on an existing hot wallet") {
                openGaslessSendScreenWithHotWallet(
                    seedPhrase = SVS_SEED_PHRASE_12,
                    tokenName = tokenName,
                    userTokensState = hotWalletTokensState,
                    quotesState = scenarioState,
                )
            }
            step("Click on 'Max' button") {
                onSendScreen { maxButton.performClick() }
            }
            step("Click on 'Next' button") {
                onSendScreen { nextButton.clickWithAssertion() }
            }
            step("Enter the recipient and open the 'Send confirm' screen") {
                enterRecipientAndOpenSendConfirm(ETHEREUM_RECIPIENT_ADDRESS)
            }
            step("Pay the network fee with '$tokenName' via the fee selector") {
                selectStablecoinAsFeeToken(coinName = nativeTokenName, tokenName = tokenName)
            }
            step("Click on 'Apply' button") {
                onSendFeeSelectorBottomSheet { applyButton.performClick() }
            }
            step("Assert 'Network fee coverage' notification title is displayed") {
                flakySafely(WAIT_UNTIL_TIMEOUT_LONG) {
                    onSendConfirmScreen { warningTitle(feeCoverageTitle).assertIsDisplayed() }
                }
            }
            step("Assert 'Network fee coverage' notification text is displayed (amount reduced by fee)") {
                onSendConfirmScreen { warningMessageContaining(feeCoverageMessagePart).assertIsDisplayed() }
            }
            step("Assert 'Send' button is enabled (enough left for the fee)") {
                onSendConfirmScreen { sendButton.assertIsEnabled() }
            }
            step("Sign, send and open the 'Transaction sent' screen") {
                openSendSuccessScreenViaLongClickOnSendButton()
            }
        }
    }

    @AllureId("5065")
    @DisplayName("Gasless: sign and send a stablecoin transaction with the stablecoin fee")
    @Test
    fun checkSignAndSendGaslessTransactionTest() {
        setupHooks(
            additionalAfterSection = {
                resetWireMockScenarioState(USER_TOKENS_API_SCENARIO)
                resetWireMockScenarioState(QUOTES_API_SCENARIO)
            }
        ).run {
            step("Open the send flow for '$tokenName' on an existing hot wallet") {
                openGaslessSendScreenWithHotWallet(
                    seedPhrase = SVS_SEED_PHRASE_12,
                    tokenName = tokenName,
                    userTokensState = hotWalletTokensState,
                    quotesState = scenarioState,
                )
            }
            step("Enter amount '$tokenAmount' and open the 'Send confirm' screen") {
                enterAmountAndOpenSendConfirm(amount = tokenAmount, recipientAddress = ETHEREUM_RECIPIENT_ADDRESS)
            }
            step("Pay the network fee with '$tokenName' via the fee selector") {
                selectStablecoinAsFeeToken(coinName = nativeTokenName, tokenName = tokenName)
            }
            step("Click on 'Apply' button") {
                onSendFeeSelectorBottomSheet { applyButton.performClick() }
            }
            step("Assert gasless fee is paid in '$currencySymbol' and 'Send' is enabled") {
                flakySafely(WAIT_UNTIL_TIMEOUT_LONG) {
                    onSendConfirmScreen {
                        feeBlockCurrency(currencySymbol).assertIsDisplayed()
                        sendButton.assertIsEnabled()
                    }
                }
            }
            step("Sign, send and open the 'Transaction sent' screen") {
                openSendSuccessScreenViaLongClickOnSendButton()
            }
        }
    }

    @AllureId("5066")
    @DisplayName("Gasless: completed gasless transaction is shown in token transaction history")
    @Test
    fun checkGaslessTransactionInHistoryTest() {
        val sentAmount = "1.00"
        val gaslessFeeAmount = "0.10"
        val sentTitle = getResourceString(R.string.common_sent)
        val gaslessFeeTitle = getResourceString(R.string.gasless_transaction_fee)

        setupHooks(
            additionalAfterSection = {
                resetWireMockScenarioState(USER_TOKENS_API_SCENARIO)
                resetWireMockScenarioState(QUOTES_API_SCENARIO)
            }
        ).run {
            step("Set WireMock scenario '$USER_TOKENS_API_SCENARIO' to '$scenarioState'") {
                setWireMockScenarioState(scenarioName = USER_TOKENS_API_SCENARIO, state = scenarioState)
            }
            step("Set WireMock scenario '$QUOTES_API_SCENARIO' to '$scenarioState'") {
                setWireMockScenarioState(scenarioName = QUOTES_API_SCENARIO, state = scenarioState)
            }
            step("Open 'Main' screen") {
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
            step("Wait for gasless '$gaslessFeeTitle' transaction in history") {
                flakySafely(WAIT_UNTIL_TIMEOUT_LONG) {
                    onTxHistoryScreen { transactionItem(gaslessFeeTitle).assertIsDisplayed() }
                }
            }
            step("Assert '$sentTitle' transaction is displayed") {
                onTxHistoryScreen { transactionItem(sentTitle).assertIsDisplayed() }
            }
            step("Assert '$sentTitle' amount '$sentAmount' is displayed in '$currencySymbol'") {
                onTxHistoryScreen {
                    transactionAmount(sentTitle).assertTextContains(sentAmount, substring = true)
                    transactionCurrency(sentTitle).assertTextEquals(currencySymbol)
                }
            }
            step("Assert gasless '$gaslessFeeTitle' amount '$gaslessFeeAmount' is displayed in '$currencySymbol'") {
                onTxHistoryScreen {
                    transactionAmount(gaslessFeeTitle).assertTextContains(gaslessFeeAmount, substring = true)
                    transactionCurrency(gaslessFeeTitle).assertTextEquals(currencySymbol)
                }
            }
            step("Assert gasless '$gaslessFeeTitle' status is confirmed") {
                onTxHistoryScreen { transactionConfirmedStatus(gaslessFeeTitle).assertIsDisplayed() }
            }
        }
    }
}