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
import com.tangem.common.utils.resetWireMockScenarios
import com.tangem.common.utils.setWireMockScenarioState
import com.tangem.core.res.R as CoreResR
import com.tangem.scenarios.*
import com.tangem.screens.*
import com.tangem.screens.tangempay.*
import dagger.hilt.android.testing.HiltAndroidTest
import io.github.kakaocup.kakao.common.utilities.getResourceString
import io.qameta.allure.kotlin.AllureId
import io.qameta.allure.kotlin.junit4.DisplayName
import org.junit.Test

@HiltAndroidTest
class TangemPayTopUpTest : BaseTestCase() {

    @AllureId("4973")
    @DisplayName("Tangem Pay: top up swaps Bitcoin to USDC and appends deposit to history")
    @Test
    fun topUpFromTangemPaySwapsBitcoinToUSDCAppendsDepositToHistoryTest() {
        val bitcoinScenario = "bitcoin_utxo"
        val expressAssetsScenario = "express_api_assets"
        val balanceScenario = "tangem_pay_balance_update"
        val historyScenario = "tangem_pay_transaction_history"
        val eligibilityState = "PaeraCustomer"
        val bitcoinBalanceState = "BalanceHotWalletSvS"
        val expressAssetsState = "BitcoinExchangeEnabled"
        val balanceInitialState = "InitialBalance"
        val balanceAfterState = "AfterDeposit"
        val historyInitialState = "InitialEmpty"
        val historyAfterState = "AfterDeposit"
        val swapFromAmount = "0.001"
        val depositLabel = getResourceString(CoreResR.string.tangem_pay_deposit)

        setupHooks(
            additionalBeforeSection = {
                resetWireMockScenarios()
                setWireMockScenarioState(TANGEM_PAY_ELIGIBILITY_SCENARIO, eligibilityState)
                setWireMockScenarioState(bitcoinScenario, bitcoinBalanceState)
                setWireMockScenarioState(expressAssetsScenario, expressAssetsState)
                setWireMockScenarioState(balanceScenario, balanceInitialState)
                setWireMockScenarioState(historyScenario, historyInitialState)
            },
            additionalAfterSection = {
                resetWireMockScenarioState(TANGEM_PAY_ELIGIBILITY_SCENARIO)
                resetWireMockScenarioState(bitcoinScenario)
                resetWireMockScenarioState(expressAssetsScenario)
                resetWireMockScenarioState(balanceScenario)
                resetWireMockScenarioState(historyScenario)
            },
        ).run {
            openTangemPay()
            step("Assert initial balance contains '10'") {
                onTangemPayMainScreen { balance.assertTextContainsSafe("10", substring = true) }
            }
            step("Click on 'Top Up' action chip") {
                waitForIdle()
                onTangemPayMainScreen { topUpButton.clickWithAssertion() }
            }
            step("Assert 'Add Funds' sheet is displayed") {
                onTangemPayAddFundsSheet { title.assertIsDisplayed() }
            }
            step("Click on 'Swap' option") {
                onTangemPayAddFundsSheet { swapOption.clickWithAssertion() }
            }
            step("Click on 'Close' button on Swap stories") {
                onSwapStoriesScreen { closeButton.performClick() }
            }
            step("Assert 'Swap' screen is displayed (USDC pre-filled as destination)") {
                onSwapTokenScreen { title.assertIsDisplayed() }
            }
            step("Click on 'Choose token' button (from)") {
                onSwapTokenScreen { chooseTokenButton.clickWithAssertion() }
            }
            step("Click on 'Main account'") {
                onSwapSelectTokenScreen { tokenWithName("Main account").clickWithAssertion() }
            }
            step("Click on token 'Bitcoin'") {
                waitForIdle()
                onSwapSelectTokenScreen { tokenWithName("Bitcoin").clickWithAssertion() }
            }
            step("Enter swap amount '$swapFromAmount'") {
                onSwapTokenScreen {
                    textInput.performClick()
                    textInput.performTextReplacement(swapFromAmount)
                }
            }
            step("Dismiss keyboard") {
                Espresso.closeSoftKeyboard()
                waitForIdle()
            }
            step("Wait until provider quote + fee are loaded") {
                onSwapTokenScreen {
                    flakySafely(WAIT_UNTIL_TIMEOUT_LONG) {
                        networkFeeBlock.assertIsDisplayed()
                        feeAmount.assertIsDisplayed()
                    }
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
            step("Assert balance updated to '\$110.00'") {
                onTangemPayMainScreen {
                    flakySafely(WAIT_UNTIL_TIMEOUT_LONG) {
                        balance.assertTextContainsSafe("110", substring = true)
                    }
                }
            }
            step("Assert '$depositLabel' transaction visible in history") {
                onTangemPayMainScreen {
                    transactionRowWithText(depositLabel).assertIsDisplayed()
                }
            }
        }
    }
}