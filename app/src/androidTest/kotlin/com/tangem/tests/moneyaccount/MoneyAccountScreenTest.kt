package com.tangem.tests.moneyaccount

import androidx.test.InstrumentationRegistry.getTargetContext
import com.tangem.common.BaseTestCase
import com.tangem.common.constants.TestConstants.TANGEM_PAY_ELIGIBILITY_SCENARIO
import com.tangem.common.constants.TestConstants.USER_TOKENS_API_SCENARIO
import com.tangem.common.constants.TestConstants.WAIT_UNTIL_TIMEOUT
import com.tangem.common.constants.TestConstants.WAIT_UNTIL_TIMEOUT_LONG
import com.tangem.common.extensions.bringAppToForeground
import com.tangem.common.extensions.collapseAppByHomeButton
import com.tangem.common.utils.getWireMockRequestCountByQueryParam
import com.tangem.common.utils.setWireMockScenarioState
import com.tangem.core.configtoggle.FeatureToggles
import com.tangem.core.res.R as CoreResR
import com.tangem.scenarios.confirmTangemPayWithdrawByHolding
import com.tangem.scenarios.enterWithdrawAmount
import com.tangem.scenarios.openMoneyAccount
import com.tangem.scenarios.openMoneyAccountSendHub
import com.tangem.scenarios.refreshMoneyAccountUntilBalance
import com.tangem.scenarios.refreshMoneyAccountUntilTransaction
import com.tangem.scenarios.selectTokenInAccount
import com.tangem.scenarios.topUpMoneyAccountFromWallet
import com.tangem.screens.moneyaccount.onMoneyAccountScreen
import com.tangem.screens.onSwapSuccessScreen
import com.tangem.screens.onSwapTokenScreen
import com.tangem.screens.onTxHistoryScreen
import com.tangem.screens.tangempay.onTangemPayWithdrawHubScreen
import dagger.hilt.android.testing.HiltAndroidTest
import io.github.kakaocup.kakao.common.utilities.getResourceString
import io.qameta.allure.kotlin.AllureId
import io.qameta.allure.kotlin.junit4.DisplayName
import org.junit.Assert.assertTrue
import org.junit.Test

@HiltAndroidTest
class MoneyAccountScreenTest : BaseTestCase() {

    override val featureToggleOverrides = mapOf(
        FeatureToggles.TWI_1525_MONEY_ACCOUNTS_ENABLED.rawName to true,
    )

    private val eligibilityState = "PaeraCustomer"
    private val moneyAccountScenario = "tangem_pay_money_account"
    private val moneyAccountActiveState = "AccountActive"

    @AllureId("13579")
    @DisplayName("Money account screen: the title is 'USD account'")
    @Test
    fun screenTitleIsUsdAccountTest() {
        val title = getResourceString(CoreResR.string.virtual_account_title)

        setupHooks().run {
            step("Set WireMock scenario '$TANGEM_PAY_ELIGIBILITY_SCENARIO' to '$eligibilityState'") {
                setWireMockScenarioState(TANGEM_PAY_ELIGIBILITY_SCENARIO, eligibilityState)
            }
            step("Set WireMock scenario '$moneyAccountScenario' to '$moneyAccountActiveState'") {
                setWireMockScenarioState(moneyAccountScenario, moneyAccountActiveState)
            }
            step("Open 'Money account' screen") { openMoneyAccount() }
            step("Assert screen title '$title' is displayed") {
                onMoneyAccountScreen {
                    screenTitle.assertIsDisplayed()
                    screenTitle.assertTextEquals(title)
                }
            }
        }
    }

    @AllureId("13591")
    @DisplayName("Money account screen: empty history has no 'Explore' button")
    @Test
    fun emptyHistoryHasNoExploreButtonTest() {
        setupHooks().run {
            step("Set WireMock scenario '$TANGEM_PAY_ELIGIBILITY_SCENARIO' to '$eligibilityState'") {
                setWireMockScenarioState(TANGEM_PAY_ELIGIBILITY_SCENARIO, eligibilityState)
            }
            step("Set WireMock scenario '$moneyAccountScenario' to '$moneyAccountActiveState'") {
                setWireMockScenarioState(moneyAccountScenario, moneyAccountActiveState)
            }
            step("Open 'Money account' screen") { openMoneyAccount() }
            step("Assert empty history text is displayed") {
                onMoneyAccountScreen { flakySafely(WAIT_UNTIL_TIMEOUT) { emptyHistoryText.assertIsDisplayed() } }
            }
            step("Assert 'Explore' button is not displayed") {
                onMoneyAccountScreen { historyExploreButton.assertDoesNotExist() }
            }
        }
    }

    @AllureId("13586")
    @DisplayName("Money account screen: pull-to-refresh reloads the balance and the history")
    @Test
    fun pullToRefreshReloadsBalanceAndHistoryTest() {
        val usdcBalanceScenario = "money_account_usdc_balance"
        val usdcHistoryScenario = "money_account_usdc_history"
        val afterDepositState = "AfterDeposit"
        val initialBalance = "$25.00"
        val balanceAfterDeposit = "$35.00"
        val receivedTitle = getResourceString(CoreResR.string.common_received)

        setupHooks().run {
            step("Set WireMock scenario '$TANGEM_PAY_ELIGIBILITY_SCENARIO' to '$eligibilityState'") {
                setWireMockScenarioState(TANGEM_PAY_ELIGIBILITY_SCENARIO, eligibilityState)
            }
            step("Set WireMock scenario '$moneyAccountScenario' to '$moneyAccountActiveState'") {
                setWireMockScenarioState(moneyAccountScenario, moneyAccountActiveState)
            }
            step("Open 'Money account' screen") { openMoneyAccount() }
            step("Assert balance '$initialBalance' is displayed") {
                onMoneyAccountScreen { flakySafely(WAIT_UNTIL_TIMEOUT) { balance.assertTextEquals(initialBalance) } }
            }
            step("Assert empty history text is displayed") {
                onMoneyAccountScreen { flakySafely(WAIT_UNTIL_TIMEOUT) { emptyHistoryText.assertIsDisplayed() } }
            }
            step("Switch WireMock scenario '$usdcBalanceScenario' to '$afterDepositState'") {
                setWireMockScenarioState(usdcBalanceScenario, afterDepositState)
            }
            step("Switch WireMock scenario '$usdcHistoryScenario' to '$afterDepositState'") {
                setWireMockScenarioState(usdcHistoryScenario, afterDepositState)
            }
            step("Pull to refresh 'Money account' screen") { refreshMoneyAccountUntilTransaction(receivedTitle) }
            step("Assert incoming '$receivedTitle' transaction is displayed") {
                onTxHistoryScreen { transactionItem(receivedTitle).assertIsDisplayed() }
            }
            step("Pull to refresh 'Money account' screen") { refreshMoneyAccountUntilBalance(balanceAfterDeposit) }
            step("Assert balance '$balanceAfterDeposit' is displayed") {
                onMoneyAccountScreen { balance.assertTextEquals(balanceAfterDeposit) }
            }
        }
    }

    @AllureId("13368")
    @DisplayName("Money account screen: the history does not reload when the account status refreshes")
    @Test
    fun historyDoesNotReloadOnStatusRefreshTest() {
        val storiesScenario = "stories_first_time_swap_v2"
        val storiesErrorState = "Error"
        val userTokensUsdcState = "TangemPayTransferUsdc"
        val usdcBalanceScenario = "money_account_usdc_balance"
        val usdcBalanceAfterDepositState = "AfterDeposit"
        val usdcHistoryScenario = "money_account_usdc_history"
        val usdcHistoryManyState = "Many"
        val usdcHistoryManyAfterDepositState = "ManyAfterDeposit"
        val etherscanApiPath = "/api.etherscan.io/v2/api"
        val moneyAccountAddress = "0x5aAb90356b4cd7Fb64f7F70dC6fe063A7B52f011"
        val receivedTitle = getResourceString(CoreResR.string.common_received)
        val newestSenderEnd = "0001"
        val middleSenderEnd = "0008"
        val transferredTitle = getResourceString(CoreResR.string.common_transferred)
        val depositAmount = "10"
        val balanceAfterDeposit = "$35.00"
        val packageName = getTargetContext().packageName

        setupHooks(
            additionalBeforeAppLaunchSection = { setWireMockScenarioState(storiesScenario, storiesErrorState) },
        ).run {
            step("Set WireMock scenario '$TANGEM_PAY_ELIGIBILITY_SCENARIO' to '$eligibilityState'") {
                setWireMockScenarioState(TANGEM_PAY_ELIGIBILITY_SCENARIO, eligibilityState)
            }
            step("Set WireMock scenario '$moneyAccountScenario' to '$moneyAccountActiveState'") {
                setWireMockScenarioState(moneyAccountScenario, moneyAccountActiveState)
            }
            step("Set WireMock scenario '$USER_TOKENS_API_SCENARIO' to '$userTokensUsdcState'") {
                setWireMockScenarioState(USER_TOKENS_API_SCENARIO, userTokensUsdcState)
            }
            step("Set WireMock scenario '$usdcHistoryScenario' to '$usdcHistoryManyState'") {
                setWireMockScenarioState(usdcHistoryScenario, usdcHistoryManyState)
            }
            step("Open 'Money account' screen") { openMoneyAccount() }
            step("Assert newest '$receivedTitle' transaction is displayed") {
                onTxHistoryScreen {
                    flakySafely(WAIT_UNTIL_TIMEOUT) {
                        transactionItemWithText(receivedTitle, newestSenderEnd).assertIsDisplayed()
                    }
                }
            }
            val historyRequestsBefore = getAccountHistoryRequestCount(etherscanApiPath, moneyAccountAddress)
            step("Pull to refresh 'Money account' screen") { onMoneyAccountScreen { swipeDownToRefresh() } }
            step("Assert newest '$receivedTitle' transaction stays displayed during the refresh") {
                var isShown = false
                onTxHistoryScreen { isShown = isTransactionItemWithTextShown(receivedTitle, newestSenderEnd) }
                assertTrue("History was reset by the refresh", isShown)
            }
            step("Assert the refresh reached the backend") {
                flakySafely(WAIT_UNTIL_TIMEOUT) {
                    val historyRequestsAfter = getAccountHistoryRequestCount(etherscanApiPath, moneyAccountAddress)
                    if (historyRequestsAfter <= historyRequestsBefore) throw AssertionError("Refresh did not happen")
                }
            }
            step("Assert newest '$receivedTitle' transaction is still displayed") {
                onTxHistoryScreen { transactionItemWithText(receivedTitle, newestSenderEnd).assertIsDisplayed() }
            }
            step("Scroll history to the middle") { onMoneyAccountScreen { scrollToText(middleSenderEnd) } }
            step("Assert middle '$receivedTitle' transaction is displayed") {
                onTxHistoryScreen { transactionItemWithText(receivedTitle, middleSenderEnd).assertIsDisplayed() }
            }
            step("Collapse the app") { collapseAppByHomeButton() }
            step("Bring the app to foreground") { bringAppToForeground(packageName) }
            step("Assert middle '$receivedTitle' transaction is still displayed") {
                onTxHistoryScreen {
                    flakySafely(WAIT_UNTIL_TIMEOUT) {
                        transactionItemWithText(receivedTitle, middleSenderEnd).assertIsDisplayed()
                    }
                }
            }
            step("Scroll 'Money account' screen to the top") { onMoneyAccountScreen { scrollToTop() } }
            step("Top up the account with '$depositAmount' USDC from the wallet") {
                topUpMoneyAccountFromWallet(depositAmount)
            }
            step("Switch WireMock scenario '$usdcBalanceScenario' to '$usdcBalanceAfterDepositState'") {
                setWireMockScenarioState(usdcBalanceScenario, usdcBalanceAfterDepositState)
            }
            step("Switch WireMock scenario '$usdcHistoryScenario' to '$usdcHistoryManyAfterDepositState'") {
                setWireMockScenarioState(usdcHistoryScenario, usdcHistoryManyAfterDepositState)
            }
            step("Scroll history to the new deposit") {
                onMoneyAccountScreen { flakySafely(WAIT_UNTIL_TIMEOUT_LONG) { scrollToText(transferredTitle) } }
            }
            step("Assert new deposit '$transferredTitle' transaction is displayed") {
                onTxHistoryScreen { transactionItem(transferredTitle).assertIsDisplayed() }
            }
            step("Assert previous '$receivedTitle' transaction is displayed") {
                onTxHistoryScreen { transactionItemWithText(receivedTitle, newestSenderEnd).assertIsDisplayed() }
            }
            step("Scroll 'Money account' screen to the top") { onMoneyAccountScreen { scrollToTop() } }
            step("Pull to refresh 'Money account' screen") { refreshMoneyAccountUntilBalance(balanceAfterDeposit) }
            step("Assert balance '$balanceAfterDeposit' is displayed") {
                onMoneyAccountScreen { balance.assertTextEquals(balanceAfterDeposit) }
            }
        }
    }

    @AllureId("13584")
    @DisplayName("Money account history: an outgoing transfer appears without an app restart")
    @Test
    fun outgoingTransferAppearsWithoutRestartTest() {
        val storiesScenario = "stories_first_time_swap_v2"
        val storiesErrorState = "Error"
        val userTokensUsdcState = "TangemPayTransferUsdc"
        val usdcHistoryScenario = "money_account_usdc_history"
        val usdcHistoryOutgoingState = "Outgoing"
        val usdc = "USDC"
        val mainAccountName = getResourceString(CoreResR.string.account_main_account_title)
        val withdrawAmount = "2"
        val transferredTitle = getResourceString(CoreResR.string.common_transferred)

        setupHooks(
            additionalBeforeAppLaunchSection = { setWireMockScenarioState(storiesScenario, storiesErrorState) },
        ).run {
            step("Set WireMock scenario '$TANGEM_PAY_ELIGIBILITY_SCENARIO' to '$eligibilityState'") {
                setWireMockScenarioState(TANGEM_PAY_ELIGIBILITY_SCENARIO, eligibilityState)
            }
            step("Set WireMock scenario '$moneyAccountScenario' to '$moneyAccountActiveState'") {
                setWireMockScenarioState(moneyAccountScenario, moneyAccountActiveState)
            }
            step("Set WireMock scenario '$USER_TOKENS_API_SCENARIO' to '$userTokensUsdcState'") {
                setWireMockScenarioState(USER_TOKENS_API_SCENARIO, userTokensUsdcState)
            }
            step("Open 'Money account' screen") { openMoneyAccount() }
            step("Assert empty history text is displayed") {
                onMoneyAccountScreen { flakySafely(WAIT_UNTIL_TIMEOUT) { emptyHistoryText.assertIsDisplayed() } }
            }
            step("Open 'Send' hub") { openMoneyAccountSendHub() }
            step("Click on 'Within your portfolio' card") {
                onTangemPayWithdrawHubScreen { withinPortfolioCard.performClick() }
            }
            step("Click on 'Choose token' button") {
                onSwapTokenScreen {
                    flakySafely(WAIT_UNTIL_TIMEOUT_LONG) { chooseTokenButton.assertIsDisplayed() }
                    chooseTokenButton.performClick()
                }
            }
            step("Choose '$usdc' of '$mainAccountName' as receive token") {
                selectTokenInAccount(accountName = mainAccountName, tokenName = usdc)
            }
            step("Enter withdraw amount '$withdrawAmount'") { enterWithdrawAmount(withdrawAmount) }
            step("Confirm the transfer by holding the button") { confirmTangemPayWithdrawByHolding() }
            step("Assert 'Transfer in progress' screen is displayed") {
                flakySafely(WAIT_UNTIL_TIMEOUT_LONG) { onSwapSuccessScreen { transferInProgressTitle.assertIsDisplayed() } }
            }
            step("Switch WireMock scenario '$usdcHistoryScenario' to '$usdcHistoryOutgoingState'") {
                setWireMockScenarioState(usdcHistoryScenario, usdcHistoryOutgoingState)
            }
            step("Click on 'Close' button") { onSwapSuccessScreen { closeButton.performClick() } }
            step("Assert 'Send' hub screen is displayed") {
                onTangemPayWithdrawHubScreen { flakySafely { withinPortfolioCard.assertIsDisplayed() } }
            }
            step("Click on system 'Back' button") { device.uiDevice.pressBack() }
            step("Scroll history to the outgoing '$transferredTitle' transaction") {
                onMoneyAccountScreen { flakySafely(WAIT_UNTIL_TIMEOUT_LONG) { scrollToText(transferredTitle) } }
            }
            step("Assert outgoing '$transferredTitle' transaction is displayed") {
                onTxHistoryScreen { transactionItem(transferredTitle).assertIsDisplayed() }
            }
        }
    }
}

private fun getAccountHistoryRequestCount(path: String, address: String): Int =
    getWireMockRequestCountByQueryParam(method = "GET", urlPath = path, queryParam = "address", queryValue = address)