package com.tangem.tests.tangempay

import androidx.test.espresso.Espresso
import com.tangem.common.BaseTestCase
import com.tangem.common.constants.TestConstants.TANGEM_PAY_ACCESS_CODE
import com.tangem.common.constants.TestConstants.TANGEM_PAY_ELIGIBILITY_SCENARIO
import com.tangem.common.constants.TestConstants.WAIT_UNTIL_TIMEOUT_LONG
import com.tangem.common.constants.TestConstants.WAIT_UNTIL_TIMEOUT_VERY_LONG
import com.tangem.common.extensions.assertTextContainsSafe
import com.tangem.common.extensions.clickWithAssertion
import com.tangem.common.utils.resetWireMockScenarioState
import com.tangem.common.utils.setWireMockScenarioState
import com.tangem.core.res.R as CoreResR
import com.tangem.scenarios.*
import com.tangem.screens.*
import com.tangem.screens.tangempay.*
import dagger.hilt.android.testing.HiltAndroidTest
import io.github.kakaocup.kakao.common.utilities.getResourceString
import io.qameta.allure.kotlin.AllureId
import io.qameta.allure.kotlin.junit4.DisplayName
import org.junit.Ignore
import org.junit.Test

@HiltAndroidTest
class TangemPayWithdrawTest : BaseTestCase() {

    @AllureId("4972")
    @DisplayName("Tangem Pay: withdraw swaps USDC to Bitcoin and appends withdrawal to history")
    @Ignore("[REDACTED_JIRA]")
    @Test
    fun withdrawFromTangemPay_SwapsUSDCToBitcoin_AppendsWithdrawalToHistory() {
        val bitcoinScenario = "bitcoin_utxo"
        val expressAssetsScenario = "express_api_assets"
        val exchangeStatusScenario = "exchange_status_provider"
        val balanceScenario = "tangem_pay_balance_update"
        val historyScenario = "tangem_pay_transaction_history"
        val eligibilityState = "PaeraCustomer"
        val bitcoinStartedState = "Started"
        val expressAssetsState = "BitcoinExchangeEnabled"
        val exchangeStatusState = "Changelly"
        val balanceInitialState = "InitialBalance"
        val balanceAfterState = "AfterWithdraw"
        val historyInitialState = "InitialEmpty"
        val historyAfterState = "AfterWithdraw"
        val withdrawAmount = "5"
        val withdrawalLabel = getResourceString(CoreResR.string.tangem_pay_withdrawal)

        setupHooks(
            additionalBeforeSection = {
                setWireMockScenarioState(TANGEM_PAY_ELIGIBILITY_SCENARIO, eligibilityState)
                setWireMockScenarioState(bitcoinScenario, bitcoinStartedState)
                setWireMockScenarioState(expressAssetsScenario, expressAssetsState)
                setWireMockScenarioState(exchangeStatusScenario, exchangeStatusState)
                setWireMockScenarioState(balanceScenario, balanceInitialState)
                setWireMockScenarioState(historyScenario, historyInitialState)
            },
            additionalAfterSection = {
                resetWireMockScenarioState(TANGEM_PAY_ELIGIBILITY_SCENARIO)
                resetWireMockScenarioState(bitcoinScenario)
                resetWireMockScenarioState(expressAssetsScenario)
                resetWireMockScenarioState(exchangeStatusScenario)
                resetWireMockScenarioState(balanceScenario)
                resetWireMockScenarioState(historyScenario)
            },
        ).run {
            openTangemPay()
            step("Assert initial balance contains '10'") {
                onTangemPayMainScreen { balance.assertTextContainsSafe("10", substring = true) }
            }
            step("Click on 'Withdraw' action chip") {
                onTangemPayMainScreen { withdrawButton.clickWithAssertion() }
            }
            step("Acknowledge withdrawal note sheet") {
                onTangemPayWithdrawNoteSheet {
                    title.assertIsDisplayed()
                    gotItButton.clickWithAssertion()
                }
            }
            step("Click on 'Close' button on Swap stories") {
                onSwapStoriesScreen { closeButton.performClick() }
            }
            step("Assert 'Swap' screen is displayed (USDC pre-filled as source)") {
                onSwapTokenScreen { title.assertIsDisplayed() }
            }
            step("Click on 'Choose token' button (to)") {
                onSwapTokenScreen { chooseTokenButton.clickWithAssertion() }
            }
            step("Click on 'Main account'") {
                onSwapSelectTokenScreen { tokenWithName("Main account").clickWithAssertion() }
            }
            step("Click on token 'Bitcoin'") {
                waitForIdle()
                onSwapSelectTokenScreen { tokenWithName("Bitcoin").clickWithAssertion() }
            }
            step("Enter withdraw amount '$withdrawAmount'") {
                onSwapTokenScreen {
                    textInput.performClick()
                    textInput.performTextReplacement(withdrawAmount)
                }
            }
            step("Dismiss keyboard") {
                Espresso.closeSoftKeyboard()
                waitForIdle()
            }
            step("Wait until network fee row is rendered (HoldToConfirm enabled)") {
                onSwapTokenScreen {
                    flakySafely(WAIT_UNTIL_TIMEOUT_LONG) { networkFeeTitle.assertIsDisplayed() }
                }
            }
            step("Confirm swap by holding the button") {
                confirmSwapByHolding(accessCode = TANGEM_PAY_ACCESS_CODE)
            }
            step("Wait for 'Swap in progress' screen") {
                onSwapSuccessScreen {
                    flakySafely(WAIT_UNTIL_TIMEOUT_VERY_LONG) { title.assertIsDisplayed() }
                }
            }
            step("Click on 'Close' button") {
                onSwapSuccessScreen { closeButton.performClick() }
            }
            step("Switch WireMock scenario '$balanceScenario' to '$balanceAfterState'") {
                setWireMockScenarioState(balanceScenario, balanceAfterState)
            }
            step("Switch WireMock scenario '$historyScenario' to '$historyAfterState'") {
                setWireMockScenarioState(historyScenario, historyAfterState)
            }
            step("Pull to refresh Tangem Pay") { pullToRefreshTangemPay() }
            step("Assert balance updated to '\$5.00'") {
                onTangemPayMainScreen {
                    flakySafely(WAIT_UNTIL_TIMEOUT_LONG) {
                        balance.assertTextContainsSafe("5", substring = true)
                    }
                }
            }
            step("Assert '$withdrawalLabel' transaction visible in history") {
                onTangemPayMainScreen {
                    transactionRowWithText(withdrawalLabel).assertIsDisplayed()
                }
            }
        }
    }
}