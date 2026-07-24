package com.tangem.tests.actionButtons

import com.tangem.common.BaseTestCase
import com.tangem.common.constants.TestConstants.QUOTES_API_SCENARIO
import com.tangem.common.constants.TestConstants.SVS_SEED_PHRASE_12
import com.tangem.common.constants.TestConstants.USER_TOKENS_API_SCENARIO
import com.tangem.common.constants.TestConstants.WAIT_UNTIL_TIMEOUT
import com.tangem.common.constants.TestConstants.WAIT_UNTIL_TIMEOUT_LONG
import com.tangem.common.constants.TestConstants.XRP_RECIPIENT_ADDRESS
import com.tangem.common.extensions.clickWithAssertion
import com.tangem.common.extensions.pullToRefresh
import com.tangem.common.utils.resetWireMockScenarioState
import com.tangem.common.utils.setWireMockScenarioState
import com.tangem.core.ui.R
import com.tangem.scenarios.checkQrCodeBottomSheetScenario
import com.tangem.scenarios.enterAmountAndOpenSendConfirm
import com.tangem.scenarios.goToQrCodeBottomSheet
import com.tangem.scenarios.openMainScreen
import com.tangem.scenarios.openSendFromTokenDetails
import com.tangem.scenarios.openSendScreenWithHotWallet
import com.tangem.scenarios.openSendSuccessScreenViaLongClickOnSendButton
import com.tangem.scenarios.readNetworkFeeAmount
import com.tangem.scenarios.synchronizeAddresses
import com.tangem.scenarios.waitUntilNetworkFeeIsStable
import com.tangem.screens.onAddFundsBottomSheet
import com.tangem.screens.onDialog
import com.tangem.screens.onMainScreen
import com.tangem.screens.onSendScreen
import com.tangem.screens.onSendSuccessScreen
import com.tangem.screens.onSwapStoriesScreen
import com.tangem.screens.onSwapTokenScreen
import com.tangem.screens.onTokenDetailsScreen
import com.tangem.screens.onTransferBottomSheet
import com.tangem.screens.onTxHistoryScreen
import dagger.hilt.android.testing.HiltAndroidTest
import io.github.kakaocup.kakao.common.utilities.getResourceString
import io.qameta.allure.kotlin.AllureId
import io.qameta.allure.kotlin.junit4.DisplayName
import org.junit.Ignore
import org.junit.Test

@HiltAndroidTest
class TokenDetailsScreenActionButtonsTest : BaseTestCase() {

    @AllureId("594")
    @DisplayName("Action buttons (token details screen): validate UI")
    @Test
    @Ignore("[REDACTED_JIRA]")
    fun actionButtonsValidateUiTest() {
        val tokenTitle = "Bitcoin"

        setupHooks().run {
            step("Open 'Main Screen'") {
                openMainScreen()
            }
            step("Synchronize addresses") {
                synchronizeAddresses()
            }
            step("Click on token with name: '$tokenTitle'") {
                waitForIdle()
                onMainScreen { tokenWithTitleAndAddress(tokenTitle).performClick() }
            }
            step("Assert 'Add funds' button is displayed") {
                onTokenDetailsScreen { addFundsButton.assertIsDisplayed() }
            }
            step("Assert 'Swap' button is displayed") {
                onTokenDetailsScreen { swapButton.assertIsDisplayed() }
            }
            step("Assert 'Transfer' button is displayed") {
                onTokenDetailsScreen { transferButton.assertIsDisplayed() }
            }
            step("Click on 'Add funds' button") {
                onTokenDetailsScreen { addFundsButton.clickWithAssertion() }
            }
            step("Assert 'Buy' button in bottom sheet is displayed") {
                onAddFundsBottomSheet { buyButton.assertIsDisplayed() }
            }
            step("Assert 'Swap' button in bottom sheet is displayed") {
                onAddFundsBottomSheet { swapButton.assertIsDisplayed() }
            }
            step("Assert 'Receive' button in bottom sheet is displayed") {
                onAddFundsBottomSheet { receiveButton.assertIsDisplayed() }
            }
            step("Click on 'Close' button in bottom sheet") {
                onAddFundsBottomSheet { closeButton.clickWithAssertion() }
            }
            step("Click on 'Transfer' button") {
                onTokenDetailsScreen { transferButton.clickWithAssertion() }
            }
            step("Assert 'Send' button in bottom sheet is displayed") {
                onTransferBottomSheet { sendButton.assertIsDisplayed() }
            }
            step("Assert 'Swap' button in bottom sheet is displayed") {
                onTransferBottomSheet { swapButton.assertIsDisplayed() }
            }
            step("Assert 'Sell' button in bottom sheet is displayed") {
                onTransferBottomSheet { sellButton.assertIsDisplayed() }
            }
        }
    }

    @AllureId("593")
    @DisplayName("Action buttons (token details screen): check buttons state")
    @Test
    fun checkActionButtonsStateTest() {
        val tokenTitle = "Bitcoin"

        setupHooks().run {
            step("Open 'Main Screen'") {
                openMainScreen()
            }
            step("Synchronize addresses") {
                synchronizeAddresses()
            }
            step("Click on token with name: '$tokenTitle'") {
                waitForIdle()
                onMainScreen { tokenWithTitleAndAddress(tokenTitle).performClick() }
            }
            step("Assert 'Add funds' button is enabled") {
                onTokenDetailsScreen { addFundsButton.assertIsEnabled() }
            }
            step("Assert 'Swap' button is disabled") {
                onTokenDetailsScreen { swapButton.assertIsNotEnabled() }
            }
            step("Assert 'Transfer' button is enabled") {
                onTokenDetailsScreen { transferButton.assertIsEnabled() }
            }
            step("Click on 'Add funds' button") {
                onTokenDetailsScreen { addFundsButton.clickWithAssertion() }
            }
            step("Assert 'Get $tokenTitle' bottom-sheet is displayed") {
                onAddFundsBottomSheet { titleWithTokenName(tokenTitle).assertIsDisplayed() }
            }
            step("Assert 'Buy' button in bottom sheet is enabled") {
                onAddFundsBottomSheet { buyButton.assertIsEnabled() }
            }
            step("Assert 'Swap' button in bottom sheet is not enabled") {
                onAddFundsBottomSheet { swapButton.assertIsNotEnabled() }
            }
            step("Assert 'Receive' button in bottom sheet is enabled") {
                onAddFundsBottomSheet { receiveButton.assertIsEnabled() }
            }
            step("Click on 'Close' button in bottom sheet") {
                onAddFundsBottomSheet { closeButton.clickWithAssertion() }
            }
            step("Click on 'Transfer' button") {
                onTokenDetailsScreen { transferButton.clickWithAssertion() }
            }
            step("Assert 'Send' button in bottom sheet is enabled") {
                onTransferBottomSheet { sendButton.assertIsEnabled() }
            }
            step("Assert 'Swap' button in bottom sheet is disabled") {
                onTransferBottomSheet { swapButton.assertIsNotEnabled() }
            }
            step("Assert 'Sell' button in bottom sheet is disabled") {
                onTransferBottomSheet { sellButton.assertIsNotEnabled() }
            }
        }
    }

    @AllureId("4459")
    @DisplayName("Action buttons (token details screen): check 'Swap' button (success)")
    @Test
    fun checkSwapButtonSuccessTest() {
        val tokenTitle = "Ethereum"
        val tokenSymbol = "ETH"

        setupHooks().run {
            step("Open 'Main Screen'") {
                openMainScreen()
            }
            step("Synchronize addresses") {
                synchronizeAddresses()
            }
            step("Click on token with name: '$tokenTitle'") {
                waitForIdle()
                onMainScreen { tokenWithTitleAndAddress(tokenTitle).performClick() }
            }
            step("Click on 'Swap' button") {
                onTokenDetailsScreen { swapButton.performClick() }
            }
            step("Close 'Stories' screen") {
                onSwapStoriesScreen { closeButton.clickWithAssertion() }
            }
            step("Assert 'Swap' screen title is displayed") {
                onSwapTokenScreen { title.assertIsDisplayed() }
            }
            step("Assert token symbol: '$tokenSymbol' is displayed") {
                onSwapTokenScreen { swapTokenSymbol(tokenSymbol).assertIsDisplayed() }
            }
        }
    }

    @AllureId("3590")
    @DisplayName("Action buttons (token details screen): validate UI")
    @Test
    fun checkReceiveButtonTest() {
        val tokenTitle = "Bitcoin"

        setupHooks().run {
            step("Open 'Main Screen'") {
                openMainScreen()
            }
            step("Synchronize addresses") {
                synchronizeAddresses()
            }
            step("Click on token with name: '$tokenTitle'") {
                waitForIdle()
                onMainScreen { tokenWithTitleAndAddress(tokenTitle).performClick() }
            }
            step("Click on 'Add funds' button") {
                onTokenDetailsScreen { addFundsButton.clickWithAssertion() }
            }
            step("Click on 'Receive' button in bottom sheet") {
                onAddFundsBottomSheet { receiveButton.clickWithAssertion() }
            }
            step("Go to QR code bottom sheet") {
                flakySafely(WAIT_UNTIL_TIMEOUT) {
                    goToQrCodeBottomSheet()
                }
            }
            step("Check QR code bottom sheet") {
                checkQrCodeBottomSheetScenario()
            }
        }
    }

    @AllureId("591")
    @DisplayName("Action buttons (token details screen): send available for funded token, unavailable for empty token")
    @Test
    fun checkSendAvailabilityForFundedAndEmptyTokenTest() {
        val emptyTokenTitle = "Polygon"
        val fundedTokenTitle = "Ethereum"
        val polygonBalanceScenarioName = "polygon_coin_balance"
        val polygonBalanceScenarioState = "ZeroBalance"

        setupHooks(
            additionalAfterSection = {
                resetWireMockScenarioState(polygonBalanceScenarioName)
            }
        ).run {
            step("Set WireMock scenario: '$polygonBalanceScenarioName' to state: '$polygonBalanceScenarioState'") {
                setWireMockScenarioState(polygonBalanceScenarioName, polygonBalanceScenarioState)
            }
            step("Open 'Main Screen'") {
                openMainScreen()
            }
            step("Synchronize addresses") {
                synchronizeAddresses()
            }
            step("Click on token with name: '$emptyTokenTitle'") {
                waitForIdle()
                onMainScreen { tokenWithTitleAndAddress(emptyTokenTitle).clickWithAssertion() }
            }
            step("Assert 'Token details' screen is displayed") {
                onTokenDetailsScreen { screenContainer.assertIsDisplayed() }
            }
            step("Assert 'Transfer' button is not displayed for the empty token") {
                onTokenDetailsScreen { transferButton.assertIsNotDisplayed() }
            }
            step("Go back to 'Main Screen'") {
                device.uiDevice.pressBack()
            }
            step("Assert 'Main Screen' is displayed") {
                onMainScreen { screenContainer.assertIsDisplayed() }
            }
            step("Click on token with name: '$fundedTokenTitle'") {
                onMainScreen { tokenWithTitleAndAddress(fundedTokenTitle).clickWithAssertion() }
            }
            step("Assert 'Transfer' button is displayed for the funded token") {
                onTokenDetailsScreen { transferButton.assertIsDisplayed() }
            }
            step("Open the send flow from token details") {
                openSendFromTokenDetails()
            }
            step("Assert 'Send' screen is displayed") {
                onSendScreen { amountInputTextField.assertIsDisplayed() }
            }
        }
    }

    @AllureId("4465")
    @DisplayName("Action buttons (token details screen): 'Send' blocked while a transaction is active, works after completion")
    @Test
    fun sendBlockedWhileTransactionActiveTest() {
        val tokenName = "XRP Ledger"
        val amount = "1"
        val userTokensState = "XRPHotWalletSvS"
        val quotesState = "Ripple"
        val startedState = "Started"
        val rippleAccountInfoScenario = "ripple_account_info"
        val pendingSendMessagePrefix =
            getResourceString(R.string.token_button_unavailability_reason_pending_transaction_send).substringBefore("%")

        setupHooks(
            additionalAfterSection = {
                resetWireMockScenarioState(USER_TOKENS_API_SCENARIO)
                resetWireMockScenarioState(QUOTES_API_SCENARIO)
                resetWireMockScenarioState(rippleAccountInfoScenario)
            },
        ).run {
            step("Set WireMock scenario: '$USER_TOKENS_API_SCENARIO' to state: '$userTokensState'") {
                setWireMockScenarioState(scenarioName = USER_TOKENS_API_SCENARIO, state = userTokensState)
            }
            step("Set WireMock scenario: '$QUOTES_API_SCENARIO' to state: '$quotesState'") {
                setWireMockScenarioState(scenarioName = QUOTES_API_SCENARIO, state = quotesState)
            }
            step("Set WireMock scenario: '$rippleAccountInfoScenario' to state: '$startedState'") {
                setWireMockScenarioState(scenarioName = rippleAccountInfoScenario, state = startedState)
            }
            step("Open the send flow for '$tokenName' on an existing hot wallet") {
                openSendScreenWithHotWallet(seedPhrase = SVS_SEED_PHRASE_12, tokenName = tokenName)
            }
            step("Enter amount '$amount' and open the 'Send confirm' screen") {
                enterAmountAndOpenSendConfirm(amount = amount, recipientAddress = XRP_RECIPIENT_ADDRESS)
            }
            waitUntilNetworkFeeIsStable { readNetworkFeeAmount() }
            step("Sign, send and open the 'Transaction sent' screen") {
                openSendSuccessScreenViaLongClickOnSendButton()
            }
            step("Click on 'Close' button") {
                onSendSuccessScreen { closeButton.clickWithAssertion() }
            }
            step("Assert 'Token details' screen is displayed") {
                onTokenDetailsScreen { screenContainer.assertIsDisplayed() }
            }
            step("Open the transfer bottom sheet") {
                onTokenDetailsScreen { transferButton.clickWithAssertion() }
            }
            step("Assert 'Send' button is not enabled while the transaction is active") {
                flakySafely(WAIT_UNTIL_TIMEOUT_LONG) {
                    onTransferBottomSheet { sendButton.assertIsNotEnabled() }
                }
            }
            step("Click on 'Send' button") {
                onTransferBottomSheet { sendButton.performClick() }
            }
            step("Assert pending-transaction notification dialog is displayed") {
                onDialog { containerWithText(pendingSendMessagePrefix).assertIsDisplayed() }
            }
            step("Assert 'Send' screen is not opened") {
                onSendScreen { amountInputTextField.assertDoesNotExist() }
            }
            // Tapping the 'Send' row dismisses the transfer bottom sheet (onActionDispatched) before the dialog shows.
            step("Close the notification dialog") {
                onDialog { okButton.clickWithAssertion() }
            }
            step("Pull to refresh to complete the active transaction") {
                pullToRefresh()
            }
            step("Open the transfer bottom sheet again") {
                onTokenDetailsScreen { transferButton.clickWithAssertion() }
            }
            step("Assert 'Send' button is enabled after the transaction is completed") {
                flakySafely(WAIT_UNTIL_TIMEOUT_LONG) {
                    onTransferBottomSheet { sendButton.assertIsEnabled() }
                }
            }
            step("Click on 'Send' button") {
                onTransferBottomSheet { sendButton.performClick() }
            }
            step("Assert 'Send' screen is displayed") {
                flakySafely(WAIT_UNTIL_TIMEOUT_LONG) {
                    onSendScreen { amountInputTextField.assertIsDisplayed() }
                }
            }
        }
    }

    @AllureId("10209")
    @DisplayName("Action buttons (token details screen): 'Send' unavailable for a zero-balance token with an active transaction")
    @Test
    fun sendUnavailableForZeroBalanceWithActiveTransactionTest() {
        val tokenName = "Dogecoin"
        val zeroBalanceState = "ZeroBalance"
        val activeTxHistoryState = "UnconfirmedOutgoing"
        val balanceScenarioName = "dogecoin_balance"
        val txHistoryScenarioName = "dogecoin_tx_history"
        val sendingTitle = getResourceString(R.string.common_sending)

        setupHooks(
            additionalAfterSection = {
                resetWireMockScenarioState(USER_TOKENS_API_SCENARIO)
                resetWireMockScenarioState(QUOTES_API_SCENARIO)
                resetWireMockScenarioState(balanceScenarioName)
                resetWireMockScenarioState(txHistoryScenarioName)
            }
        ).run {
            step("Set WireMock scenario: '$USER_TOKENS_API_SCENARIO' to state: '$tokenName'") {
                setWireMockScenarioState(scenarioName = USER_TOKENS_API_SCENARIO, state = tokenName)
            }
            step("Set WireMock scenario: '$QUOTES_API_SCENARIO' to state: '$tokenName'") {
                setWireMockScenarioState(scenarioName = QUOTES_API_SCENARIO, state = tokenName)
            }
            step("Set WireMock scenario: '$balanceScenarioName' to state: '$zeroBalanceState'") {
                setWireMockScenarioState(scenarioName = balanceScenarioName, state = zeroBalanceState)
            }
            step("Set WireMock scenario: '$txHistoryScenarioName' to state: '$activeTxHistoryState'") {
                setWireMockScenarioState(scenarioName = txHistoryScenarioName, state = activeTxHistoryState)
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
            step("Assert active outgoing '$sendingTitle' transaction block is displayed") {
                flakySafely(WAIT_UNTIL_TIMEOUT_LONG) {
                    onTxHistoryScreen { transactionItem(sendingTitle).assertIsDisplayed() }
                }
            }
            step("Assert 'Transfer' button is not displayed for the zero-balance token") {
                onTokenDetailsScreen { transferButton.assertIsNotDisplayed() }
            }
        }
    }
}