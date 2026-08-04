package com.tangem.tests.tangempay

import com.tangem.common.BaseTestCase
import com.tangem.common.constants.TestConstants.TANGEM_PAY_ELIGIBILITY_SCENARIO
import com.tangem.common.constants.TestConstants.WAIT_UNTIL_TIMEOUT
import com.tangem.common.constants.TestConstants.WAIT_UNTIL_TIMEOUT_LONG
import com.tangem.common.extensions.assertTextContainsSafe
import com.tangem.common.extensions.clickWithAssertion
import com.tangem.common.utils.getWireMockRequestCount
import com.tangem.common.utils.getWireMockRequestCountByQueryParam
import com.tangem.common.utils.resetWireMockScenarioState
import com.tangem.common.utils.resetWireMockScenarios
import com.tangem.common.utils.setWireMockScenarioState
import com.tangem.core.ui.utils.toDateFormatWithTodayYesterday
import com.tangem.scenarios.*
import com.tangem.screens.tangempay.*
import dagger.hilt.android.testing.HiltAndroidTest
import io.qameta.allure.kotlin.AllureId
import io.qameta.allure.kotlin.junit4.DisplayName
import org.junit.Assert.assertTrue
import org.junit.Test

@HiltAndroidTest
class TangemPayTransactionsTest : BaseTestCase() {

    private val historyScenario = "tangem_pay_transaction_history"
    private val balanceScenario = "tangem_pay_balance_update"
    private val eligibilityState = "PaeraCustomer"
    private val balanceInitialState = "InitialBalance"

    private val transactionsPath = "/bff-v2/v1/customer/transactions"

    // The app pages by the last item's id, so the next-page cursor is the id of the 50th first-page item.
    private val nextPageCursor = "page1-50"

    @AllureId("9545")
    @DisplayName("Tangem Pay: opening the payment account requests the transaction history")
    @Test
    fun transactionsRequestedWhenPaymentAccountOpensTest() {
        val spendCompletedState = "SpendCompleted"
        val coffeeMerchant = "Tangem Coffee"

        setupHooks(
            additionalBeforeSection = {
                resetWireMockScenarios()
                setWireMockScenarioState(TANGEM_PAY_ELIGIBILITY_SCENARIO, eligibilityState)
                setWireMockScenarioState(balanceScenario, balanceInitialState)
                setWireMockScenarioState(historyScenario, spendCompletedState)
            },
            additionalAfterSection = {
                resetWireMockScenarioState(TANGEM_PAY_ELIGIBILITY_SCENARIO)
                resetWireMockScenarioState(balanceScenario)
                resetWireMockScenarioState(historyScenario)
            },
        ).run {
            val requestsBefore = getWireMockRequestCount("GET", transactionsPath)
            step("Open Tangem Pay") { openTangemPay() }
            step("Assert '$coffeeMerchant' transaction row is displayed") {
                onTangemPayMainScreen {
                    flakySafely(WAIT_UNTIL_TIMEOUT) {
                        scrollToTransactionWithText(coffeeMerchant)
                        transactionRowWithText(coffeeMerchant).assertIsDisplayed()
                    }
                }
            }
            step("Assert transaction history was requested") {
                val requestsAfter = getWireMockRequestCount("GET", transactionsPath)
                assertTrue(
                    "Opening the payment account should request $transactionsPath",
                    requestsAfter > requestsBefore,
                )
            }
        }
    }

    @AllureId("9532")
    @DisplayName("Tangem Pay: transaction history error state reloads the history")
    @Test
    fun transactionHistoryErrorStateReloadRefetchesHistoryTest() {
        val historyErrorState = "HistoryError"
        val spendCompletedState = "SpendCompleted"
        val coffeeMerchant = "Tangem Coffee"
        val coffeeAmount = "-\$12.34"
        val coffeeCategory = "Restaurants"

        setupHooks(
            additionalBeforeSection = {
                resetWireMockScenarios()
                setWireMockScenarioState(TANGEM_PAY_ELIGIBILITY_SCENARIO, eligibilityState)
                setWireMockScenarioState(balanceScenario, balanceInitialState)
                setWireMockScenarioState(historyScenario, historyErrorState)
            },
            additionalAfterSection = {
                resetWireMockScenarioState(TANGEM_PAY_ELIGIBILITY_SCENARIO)
                resetWireMockScenarioState(balanceScenario)
                resetWireMockScenarioState(historyScenario)
            },
        ).run {
            step("Open Tangem Pay") { openTangemPay() }
            step("Assert transaction history error state is displayed") {
                onTangemPayMainScreen {
                    flakySafely(WAIT_UNTIL_TIMEOUT) {
                        scrollToHistoryErrorBlock()
                        historyErrorBlock.assertIsDisplayed()
                        historyErrorText.assertIsDisplayed()
                        reloadHistoryButton.assertIsDisplayed()
                    }
                }
            }
            step("Switch WireMock scenario '$historyScenario' to '$spendCompletedState'") {
                setWireMockScenarioState(historyScenario, spendCompletedState)
            }
            val requestsBefore = getWireMockRequestCount("GET", transactionsPath)
            step("Click on 'Reload' button") {
                onTangemPayMainScreen { reloadHistoryButton.clickWithAssertion() }
            }
            step("Assert '$coffeeMerchant' row shows amount '$coffeeAmount' and category '$coffeeCategory'") {
                onTangemPayMainScreen {
                    flakySafely(WAIT_UNTIL_TIMEOUT) {
                        scrollToTransactionWithText(coffeeMerchant)
                        transactionRowWithText(coffeeMerchant).assertIsDisplayed()
                        transactionRowWithText(coffeeAmount).assertIsDisplayed()
                        transactionRowWithText(coffeeCategory).assertIsDisplayed()
                    }
                }
            }
            step("Assert Reload requested the history again") {
                val requestsAfter = getWireMockRequestCount("GET", transactionsPath)
                assertTrue(
                    "Reload should request $transactionsPath again",
                    requestsAfter > requestsBefore,
                )
            }
        }
    }

    @AllureId("9546")
    @DisplayName("Tangem Pay: scrolling to the bottom requests the next page with the cursor")
    @Test
    fun nextPageRequestedWithCursorWhenHistoryScrolledToBottomTest() {
        val firstPageState = "HistoryFirstPage"
        val firstPageMerchant = "Merchant 01"
        val secondPageMerchant = "Second Page 1"

        setupHooks(
            additionalBeforeSection = {
                resetWireMockScenarios()
                setWireMockScenarioState(TANGEM_PAY_ELIGIBILITY_SCENARIO, eligibilityState)
                setWireMockScenarioState(balanceScenario, balanceInitialState)
                setWireMockScenarioState(historyScenario, firstPageState)
            },
            additionalAfterSection = {
                resetWireMockScenarioState(TANGEM_PAY_ELIGIBILITY_SCENARIO)
                resetWireMockScenarioState(balanceScenario)
                resetWireMockScenarioState(historyScenario)
            },
        ).run {
            step("Open Tangem Pay") { openTangemPay() }
            step("Assert first-page '$firstPageMerchant' row is displayed") {
                onTangemPayMainScreen {
                    flakySafely(WAIT_UNTIL_TIMEOUT) {
                        scrollToTransactionWithText(firstPageMerchant)
                        transactionRowWithText(firstPageMerchant).assertIsDisplayed()
                    }
                }
            }
            val cursorRequestsBefore = getWireMockRequestCountByQueryParam(
                method = "GET",
                urlPath = transactionsPath,
                queryParam = "cursor",
                queryValue = nextPageCursor,
            )
            step("Scroll the history down to '$secondPageMerchant'") {
                // Scrolling past the end of the first page is what triggers the next-page fetch.
                onTangemPayMainScreen {
                    flakySafely(WAIT_UNTIL_TIMEOUT_LONG) {
                        scrollToTransactionWithText(secondPageMerchant)
                        transactionRowWithText(secondPageMerchant).assertIsDisplayed()
                    }
                }
            }
            step("Assert the next page was requested with cursor '$nextPageCursor'") {
                val cursorRequestsAfter = getWireMockRequestCountByQueryParam(
                    method = "GET",
                    urlPath = transactionsPath,
                    queryParam = "cursor",
                    queryValue = nextPageCursor,
                )
                assertTrue(
                    "Scrolling to the bottom should request $transactionsPath with cursor=$nextPageCursor",
                    cursorRequestsAfter > cursorRequestsBefore,
                )
            }
        }
    }

    @AllureId("9581")
    @DisplayName("Tangem Pay: spend transaction is shown in the list and in the details")
    @Test
    fun spendTransactionShownInListAndDetailsTest() {
        val spendCompletedState = "SpendCompleted"
        val coffeeMerchant = "Tangem Coffee"
        val coffeeAmount = "-\$12.34"
        val coffeeCategory = "Restaurants"
        // Built with the screen's own formatter: the header follows the device locale and timezone.
        val sectionHeader = COFFEE_TX_MILLIS.toDateFormatWithTodayYesterday()

        setupHooks(
            additionalBeforeSection = {
                resetWireMockScenarios()
                setWireMockScenarioState(TANGEM_PAY_ELIGIBILITY_SCENARIO, eligibilityState)
                setWireMockScenarioState(balanceScenario, balanceInitialState)
                setWireMockScenarioState(historyScenario, spendCompletedState)
            },
            additionalAfterSection = {
                resetWireMockScenarioState(TANGEM_PAY_ELIGIBILITY_SCENARIO)
                resetWireMockScenarioState(balanceScenario)
                resetWireMockScenarioState(historyScenario)
            },
        ).run {
            step("Open Tangem Pay") { openTangemPay() }
            step("Assert '$coffeeMerchant' row shows amount '$coffeeAmount' and category '$coffeeCategory'") {
                onTangemPayMainScreen {
                    flakySafely(WAIT_UNTIL_TIMEOUT) {
                        scrollToTransactionWithText(coffeeMerchant)
                        transactionRowWithText(coffeeMerchant).assertIsDisplayed()
                        transactionRowWithText(coffeeAmount).assertIsDisplayed()
                        transactionRowWithText(coffeeCategory).assertIsDisplayed()
                    }
                }
            }
            step("Assert transaction section header '$sectionHeader' is displayed") {
                onTangemPayMainScreen {
                    flakySafely(WAIT_UNTIL_TIMEOUT) {
                        scrollToTransactionWithText(sectionHeader)
                        transactionRowWithText(sectionHeader).assertIsDisplayed()
                    }
                }
            }
            step("Open '$coffeeMerchant' transaction details") { openTangemPayTransactionDetails(coffeeMerchant) }
            step("Assert transaction details show '$coffeeMerchant' purchase, amount and Get Help") {
                onTangemPayTransactionDetailsSheet {
                    flakySafely(WAIT_UNTIL_TIMEOUT) {
                        purchaseTitle.assertIsDisplayed()
                        amount.assertTextContainsSafe(coffeeAmount, substring = true)
                        getHelpButton.assertIsDisplayed()
                    }
                }
            }
        }
    }

    @AllureId("9539")
    @DisplayName("Tangem Pay: completed transaction details show the Completed status")
    @Test
    fun completedTransactionDetailsShowCompletedStatusTest() {
        val spendCompletedState = "SpendCompleted"
        val coffeeMerchant = "Tangem Coffee"
        val coffeeAmount = "-\$12.34"

        setupHooks(
            additionalBeforeSection = {
                resetWireMockScenarios()
                setWireMockScenarioState(TANGEM_PAY_ELIGIBILITY_SCENARIO, eligibilityState)
                setWireMockScenarioState(balanceScenario, balanceInitialState)
                setWireMockScenarioState(historyScenario, spendCompletedState)
            },
            additionalAfterSection = {
                resetWireMockScenarioState(TANGEM_PAY_ELIGIBILITY_SCENARIO)
                resetWireMockScenarioState(balanceScenario)
                resetWireMockScenarioState(historyScenario)
            },
        ).run {
            step("Open Tangem Pay") { openTangemPay() }
            step("Open '$coffeeMerchant' transaction details") { openTangemPayTransactionDetails(coffeeMerchant) }
            step("Assert completed transaction details are displayed") {
                onTangemPayTransactionDetailsSheet {
                    flakySafely(WAIT_UNTIL_TIMEOUT) {
                        purchaseTitle.assertIsDisplayed()
                        amount.assertTextContainsSafe(coffeeAmount, substring = true)
                        completedStatus.assertIsDisplayed()
                        getHelpButton.assertIsDisplayed()
                    }
                }
            }
        }
    }

    private companion object {
        // authorized_at of the mocked "Tangem Coffee" spend (2026-04-23T14:00:00Z).
        const val COFFEE_TX_MILLIS = 1776952800000L
    }
}