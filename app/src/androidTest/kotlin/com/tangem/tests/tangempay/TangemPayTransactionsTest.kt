package com.tangem.tests.tangempay

import com.tangem.common.BaseTestCase
import com.tangem.common.constants.TestConstants.TANGEM_PAY_ELIGIBILITY_SCENARIO
import com.tangem.common.constants.TestConstants.WAIT_UNTIL_TIMEOUT
import com.tangem.common.constants.TestConstants.WAIT_UNTIL_TIMEOUT_LONG
import com.tangem.common.extensions.assertTextContainsSafe
import com.tangem.common.extensions.clickWithAssertion
import com.tangem.common.utils.getWireMockRequestCount
import com.tangem.common.utils.getWireMockRequestCountByQueryParam
import com.tangem.common.utils.setWireMockScenarioState
import com.tangem.core.ui.utils.DateTimeFormatters
import com.tangem.core.ui.utils.toDateFormatWithTodayYesterday
import com.tangem.scenarios.*
import com.tangem.screens.tangempay.*
import dagger.hilt.android.testing.HiltAndroidTest
import io.qameta.allure.kotlin.AllureId
import io.qameta.allure.kotlin.junit4.DisplayName
import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import org.junit.Assert.assertTrue
import org.junit.Test

@HiltAndroidTest
class TangemPayTransactionsTest : BaseTestCase() {

    private val historyScenario = "tangem_pay_transaction_history"
    private val balanceScenario = "tangem_pay_balance_update"
    private val eligibilityState = "PaeraCustomer"
    private val balanceInitialState = "InitialBalance"

    // The cashback toggle picks the endpoint: on -> /v1/transactions, off -> /v1/customer/transactions.
    private val transactionsPath = "/bff-v2/v1/transactions"

    // The app pages by the last item's id, so the next-page cursor is the id of the 50th first-page item.
    private val nextPageCursor = "page1-50"

    private val transactionDetailPath = "$transactionsPath/mock-spend-completed-1"

    // Built with the sheet's own formatter: the date follows the device locale and timezone. Read lazily —
    // Joda's timezone provider is initialized by the app, so a field initializer would run too early.
    private val spendTxDate: String
        get() = DateTimeFormatters.formatDate(
            date = DateTime(SPEND_TX_MILLIS, DateTimeZone.getDefault()),
            formatter = DateTimeFormatters.getBestFormatterBySkeleton("MMM dd yyyy, HH:mm"),
        )

    @AllureId("9545")
    @DisplayName("Tangem Pay: opening the payment account requests the transaction history")
    @Test
    fun transactionsRequestedWhenPaymentAccountOpensTest() {
        val spendCompletedState = "SpendCompleted"
        val coffeeMerchant = "Tangem Coffee"

        setupHooks(
            additionalBeforeSection = {
                setWireMockScenarioState(TANGEM_PAY_ELIGIBILITY_SCENARIO, eligibilityState)
                setWireMockScenarioState(balanceScenario, balanceInitialState)
                setWireMockScenarioState(historyScenario, spendCompletedState)
            }
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
                setWireMockScenarioState(TANGEM_PAY_ELIGIBILITY_SCENARIO, eligibilityState)
                setWireMockScenarioState(balanceScenario, balanceInitialState)
                setWireMockScenarioState(historyScenario, historyErrorState)
            }
        ).run {
            step("Open Tangem Pay") { openTangemPay() }
            step("Assert transaction history error state is displayed") {
                onTangemPayMainScreen {
                    flakySafely(WAIT_UNTIL_TIMEOUT) {
                        scrollToHistoryStateBlock()
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
                setWireMockScenarioState(TANGEM_PAY_ELIGIBILITY_SCENARIO, eligibilityState)
                setWireMockScenarioState(balanceScenario, balanceInitialState)
                setWireMockScenarioState(historyScenario, firstPageState)
            }
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
        val sectionHeader = SPEND_TX_MILLIS.toDateFormatWithTodayYesterday()

        setupHooks(
            additionalBeforeSection = {
                setWireMockScenarioState(TANGEM_PAY_ELIGIBILITY_SCENARIO, eligibilityState)
                setWireMockScenarioState(balanceScenario, balanceInitialState)
                setWireMockScenarioState(historyScenario, spendCompletedState)
            }
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
            step("Assert the Card field") {
                assertTangemPayTransactionCardField(MASKED_CARD_NUMBER, CARD_NAME)
            }
        }
    }

    @AllureId("9539")
    @DisplayName("Tangem Pay: completed transaction details show the Completed status and the Card field")
    @Test
    fun completedTransactionDetailsShowCompletedStatusTest() {
        val spendCompletedState = "SpendCompleted"
        val coffeeMerchant = "Tangem Coffee"
        val coffeeAmount = "-\$12.34"

        setupHooks(
            additionalBeforeSection = {
                setWireMockScenarioState(TANGEM_PAY_ELIGIBILITY_SCENARIO, eligibilityState)
                setWireMockScenarioState(balanceScenario, balanceInitialState)
                setWireMockScenarioState(historyScenario, spendCompletedState)
            }
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
            step("Assert the Card field") {
                assertTangemPayTransactionCardField(MASKED_CARD_NUMBER, CARD_NAME)
            }
        }
    }

    @AllureId("9514")
    @DisplayName("Tangem Pay: payment account elements are displayed when there are no transactions")
    @Test
    fun paymentAccountElementsWhenHistoryIsEmptyTest() {
        val emptyHistoryState = "InitialEmpty"

        setupHooks(
            additionalBeforeSection = {
                setWireMockScenarioState(TANGEM_PAY_ELIGIBILITY_SCENARIO, eligibilityState)
                setWireMockScenarioState(balanceScenario, balanceInitialState)
                setWireMockScenarioState(historyScenario, emptyHistoryState)
            }
        ).run {
            step("Open Tangem Pay") { openTangemPay() }
            step("Assert the payment account elements are displayed") {
                assertTangemPayEmptyPaymentAccountElements()
            }
        }
    }

    @AllureId("9528")
    @DisplayName("Tangem Pay: the next page of transactions is shown after scrolling the history down")
    @Test
    fun secondPageTransactionsShownAfterScrollToBottomTest() {
        val firstPageState = "HistoryFirstPage"
        val firstPageMerchant = "Merchant 01"
        val secondPageFirstMerchant = "Second Page 1"
        val secondPageLastMerchant = "Second Page 3"

        setupHooks(
            additionalBeforeSection = {
                setWireMockScenarioState(TANGEM_PAY_ELIGIBILITY_SCENARIO, eligibilityState)
                setWireMockScenarioState(balanceScenario, balanceInitialState)
                setWireMockScenarioState(historyScenario, firstPageState)
            }
        ).run {
            step("Open Tangem Pay") { openTangemPay() }
            step("Assert first-page '$firstPageMerchant' row is displayed") {
                assertTangemPayTransactionRowDisplayed(firstPageMerchant)
            }
            step("Scroll the history down to the second page '$secondPageFirstMerchant' row") {
                // Scrolling past the end of the first page is what triggers the next-page fetch.
                assertTangemPayTransactionRowDisplayed(secondPageFirstMerchant)
            }
            step("Scroll the history down to the last '$secondPageLastMerchant' row") {
                assertTangemPayTransactionRowDisplayed(secondPageLastMerchant)
            }
        }
    }

    @AllureId("9540")
    @DisplayName("Tangem Pay: declined transaction details show the Declined status and its reason")
    @Test
    fun declinedTransactionDetailsShowDeclinedStatusAndReasonTest() {
        val spendDeclinedState = "SpendDeclined"
        val declinedMerchant = "Tangem Declined Shop"
        val declinedCategory = "Electronics"
        val declinedAmount = "-\$25.00"

        setupHooks(
            additionalBeforeSection = {
                setWireMockScenarioState(TANGEM_PAY_ELIGIBILITY_SCENARIO, eligibilityState)
                setWireMockScenarioState(balanceScenario, balanceInitialState)
                setWireMockScenarioState(historyScenario, spendDeclinedState)
            }
        ).run {
            step("Open Tangem Pay") { openTangemPay() }
            step("Open '$declinedMerchant' transaction details") {
                openTangemPayTransactionDetails(declinedMerchant)
            }
            step("Assert the common spend details are displayed") {
                assertTangemPaySpendDetails(
                    expectedDate = spendTxDate,
                    expectedMerchant = declinedMerchant,
                    expectedCategory = declinedCategory,
                    expectedAmount = declinedAmount,
                )
            }
            step("Assert the 'Declined' status, its reason and its icon are displayed") {
                onTangemPayTransactionDetailsSheet {
                    flakySafely(WAIT_UNTIL_TIMEOUT) {
                        declinedStatus.assertIsDisplayed()
                        declinedReason.assertIsDisplayed()
                        statusIcon.assertIsDisplayed()
                    }
                }
            }
            step("Assert 'Card' field is displayed") {
                assertTangemPayTransactionCardField(MASKED_CARD_NUMBER, CARD_NAME)
            }
        }
    }

    @AllureId("9541")
    @DisplayName("Tangem Pay: pending transaction details show the Pending status")
    @Test
    fun pendingTransactionDetailsShowPendingStatusTest() {
        val spendPendingState = "SpendPending"
        val pendingMerchant = "Tangem Pending Shop"
        val pendingCategory = "Groceries"
        val pendingAmount = "-\$18.50"

        setupHooks(
            additionalBeforeSection = {
                setWireMockScenarioState(TANGEM_PAY_ELIGIBILITY_SCENARIO, eligibilityState)
                setWireMockScenarioState(balanceScenario, balanceInitialState)
                setWireMockScenarioState(historyScenario, spendPendingState)
            }
        ).run {
            step("Open Tangem Pay") { openTangemPay() }
            step("Open '$pendingMerchant' transaction details") {
                openTangemPayTransactionDetails(pendingMerchant)
            }
            step("Assert the common spend details are displayed") {
                assertTangemPaySpendDetails(
                    expectedDate = spendTxDate,
                    expectedMerchant = pendingMerchant,
                    expectedCategory = pendingCategory,
                    expectedAmount = pendingAmount,
                )
            }
            step("Assert the 'Pending' status and its icon are displayed") {
                onTangemPayTransactionDetailsSheet {
                    flakySafely(WAIT_UNTIL_TIMEOUT) {
                        pendingStatus.assertIsDisplayed()
                        statusIcon.assertIsDisplayed()
                    }
                }
            }
            step("Assert 'Card' field is displayed") {
                assertTangemPayTransactionCardField(MASKED_CARD_NUMBER, CARD_NAME)
            }
        }
    }

    @AllureId("10665")
    @DisplayName("Tangem Pay: retrying the failed Card field loads the card number and name")
    @Test
    fun cardFieldLoadingErrorRetryShowsCardNumberAndNameTest() {
        val cardDetailErrorState = "CardDetailError"
        val spendCompletedState = "SpendCompleted"
        val coffeeMerchant = "Tangem Coffee"

        setupHooks(
            additionalBeforeSection = {
                setWireMockScenarioState(TANGEM_PAY_ELIGIBILITY_SCENARIO, eligibilityState)
                setWireMockScenarioState(balanceScenario, balanceInitialState)
                setWireMockScenarioState(historyScenario, cardDetailErrorState)
            }
        ).run {
            step("Open Tangem Pay") { openTangemPay() }
            step("Open '$coffeeMerchant' transaction details") {
                openTangemPayTransactionDetails(coffeeMerchant)
            }
            step("Assert 'Card' field loading error is displayed") { assertTangemPayTransactionCardFieldError() }
            step("Switch WireMock scenario '$historyScenario' to '$spendCompletedState'") {
                setWireMockScenarioState(historyScenario, spendCompletedState)
            }
            val detailRequestsBefore = getWireMockRequestCount("GET", transactionDetailPath)
            step("Click on 'Error loading' in the Card field") {
                onTangemPayTransactionDetailsSheet { cardFieldRetryButton.clickWithAssertion() }
            }
            step("Assert 'Card' field is displayed with the card number and name") {
                assertTangemPayTransactionCardField(MASKED_CARD_NUMBER, CARD_NAME)
            }
            step("Assert the retry requested the transaction details again") {
                val detailRequestsAfter = getWireMockRequestCount("GET", transactionDetailPath)
                assertTrue(
                    "Retry should request $transactionDetailPath again",
                    detailRequestsAfter > detailRequestsBefore,
                )
            }
        }
    }

    @AllureId("9559")
    @DisplayName("Tangem Pay: reopening the payment account loads the transaction that arrived meanwhile")
    @Test
    fun newTransactionLoadedWhenPaymentAccountReopenedTest() {
        val emptyHistoryState = "InitialEmpty"
        val spendCompletedState = "SpendCompleted"
        val coffeeMerchant = "Tangem Coffee"

        setupHooks(
            additionalBeforeSection = {
                setWireMockScenarioState(TANGEM_PAY_ELIGIBILITY_SCENARIO, eligibilityState)
                setWireMockScenarioState(balanceScenario, balanceInitialState)
                setWireMockScenarioState(historyScenario, emptyHistoryState)
            }
        ).run {
            step("Open Tangem Pay") { openTangemPay() }
            step("Assert the transaction history is empty") {
                onTangemPayMainScreen {
                    flakySafely(WAIT_UNTIL_TIMEOUT) {
                        scrollToHistoryStateBlock()
                        emptyHistoryText.assertIsDisplayed()
                    }
                }
            }
            // The manual case spends from the card here; the mocked flow swaps the spend for a history state.
            step("Switch WireMock scenario '$historyScenario' to '$spendCompletedState'") {
                setWireMockScenarioState(historyScenario, spendCompletedState)
            }
            val requestsBefore = getWireMockRequestCount("GET", transactionsPath)
            step("Reopen Tangem Pay from the Main screen") { reopenTangemPayFromMain() }
            step("Assert the new '$coffeeMerchant' transaction is displayed") {
                onTangemPayMainScreen {
                    flakySafely(WAIT_UNTIL_TIMEOUT_LONG) {
                        scrollToTransactionWithText(coffeeMerchant)
                        transactionRowWithText(coffeeMerchant).assertIsDisplayed()
                    }
                }
            }
            step("Assert reopening the payment account requested the history again") {
                val requestsAfter = getWireMockRequestCount("GET", transactionsPath)
                assertTrue(
                    "Reopening the payment account should request $transactionsPath again",
                    requestsAfter > requestsBefore,
                )
            }
        }
    }

    @AllureId("11477")
    @DisplayName("Tangem Pay: the transaction details show the new card name after a rename")
    @Test
    fun renamedCardNameShownInTransactionDetailsTest() {
        val spendCompletedState = "SpendCompleted"
        val renameScenario = "tangem_pay_card_rename"
        val renameStartedState = "Started"
        val coffeeMerchant = "Tangem Coffee"
        val newCardName = "Renamed Card"

        // No balance scenario here: once the rename lands, its customer/me stub and the balance one would
        // both claim the same priority.
        setupHooks(
            additionalBeforeSection = {
                setWireMockScenarioState(TANGEM_PAY_ELIGIBILITY_SCENARIO, eligibilityState)
                setWireMockScenarioState(historyScenario, spendCompletedState)
                setWireMockScenarioState(renameScenario, renameStartedState)
            }
        ).run {
            step("Open the card rename screen") { openTangemPayCardRename() }
            step("Enter new card name '$newCardName'") {
                onTangemPayCardRenameScreen { nameField.performTextReplacement(newCardName) }
            }
            step("Click on 'Done' button") {
                onTangemPayCardRenameScreen { doneButton.clickWithAssertion() }
            }
            step("Assert the card name contains '$newCardName'") {
                flakySafely(WAIT_UNTIL_TIMEOUT_LONG) {
                    onTangemPayCardPageScreen {
                        cardNameEditButton.assertTextContainsSafe(newCardName, substring = true)
                    }
                }
            }
            step("Return to the payment account screen") {
                device.uiDevice.pressBack()
                awaitSuccess { onTangemPayMainScreen { balance.assertIsDisplayed() } }
            }
            step("Open '$coffeeMerchant' transaction details") {
                openTangemPayTransactionDetails(coffeeMerchant)
            }
            step("Assert the Card field shows the renamed card") {
                assertTangemPayTransactionCardField(MASKED_CARD_NUMBER, newCardName)
            }
        }
    }

    private companion object {
        // authorized_at shared by every mocked spend (2026-04-23T14:00:00Z).
        const val SPEND_TX_MILLIS = 1776952800000L

        // card_number_end / card_display_name of the mocked single-transaction response.
        const val MASKED_CARD_NUMBER = "*4242"
        const val CARD_NAME = "My Card"
    }
}