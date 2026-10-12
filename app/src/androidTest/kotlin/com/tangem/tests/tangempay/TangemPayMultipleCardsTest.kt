package com.tangem.tests.tangempay

import com.tangem.common.BaseTestCase
import com.tangem.common.constants.TestConstants.TANGEM_PAY_ELIGIBILITY_SCENARIO
import com.tangem.common.constants.TestConstants.WAIT_UNTIL_TIMEOUT
import com.tangem.common.constants.TestConstants.WAIT_UNTIL_TIMEOUT_LONG
import com.tangem.common.extensions.assertTextContainsSafe
import com.tangem.common.extensions.clickWithAssertion
import com.tangem.common.extensions.getQuantityString
import com.tangem.common.utils.getWireMockRequestCount
import com.tangem.common.utils.setWireMockScenarioState
import com.tangem.core.res.R as CoreResR
import com.tangem.scenarios.*
import com.tangem.screens.tangempay.*
import dagger.hilt.android.testing.HiltAndroidTest
import io.qameta.allure.kotlin.AllureId
import io.qameta.allure.kotlin.junit4.DisplayName
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@HiltAndroidTest
class TangemPayMultipleCardsTest : BaseTestCase() {

    private val eligibilityState = "PaeraCustomer"
    private val balanceScenario = "tangem_pay_balance_update"
    private val balanceInitialState = "InitialBalance"
    private val balanceZeroState = "AfterFullWithdraw"
    private val balanceBelowIssueFeeState = "BelowIssueFee"
    private val balanceAfterIssueFeeState = "AfterCardIssueFee"
    private val twoCardsState = "TwoCards"
    private val threeCardsState = "ThreeCards"
    private val issueOrderScenario = "tangem_pay_card_issue_order"
    private val issueOrderStartedState = "Started"
    private val issueOrdersScenario = "tangem_pay_card_issue_orders"
    private val issueOrdersEmptyState = "Empty"
    private val issueOrdersActiveOrderState = "ActiveOrder"
    private val historyScenario = "tangem_pay_transaction_history"
    private val historyInitialEmptyState = "InitialEmpty"
    private val historyAfterIssueFeeState = "AfterCardIssueFee"

    private val issueFee = "$1.00"
    private val orderEndpoint = "/bff-v2/v1/order"
    private val issuingCardIndex = 1

    @AllureId("9731")
    @DisplayName("Tangem Pay: 'Issue an additional card?' popup offers the issue when the balance covers the fee")
    @Test
    fun issueCardPopupAllowsIssueWhenBalanceCoversFeeTest() {
        setupHooks(
            additionalBeforeSection = {
                setWireMockScenarioState(TANGEM_PAY_ELIGIBILITY_SCENARIO, eligibilityState)
                setWireMockScenarioState(balanceScenario, balanceInitialState)
            }
        ).run {
            step("Open Tangem Pay") { openTangemPay() }
            step("Assert payment account balance contains '10.00'") {
                flakySafely(WAIT_UNTIL_TIMEOUT) {
                    onTangemPayMainScreen { balance.assertTextContainsSafe("10.00", substring = true) }
                }
            }
            step("Open the 'Issue an additional card?' sheet") { openTangemPayIssueCardSheet() }
            step("Assert issue sheet content with the '$issueFee' fee") {
                assertTangemPayIssueCardSheetContent(issueFee)
            }
            step("Assert 'Issue card' button is enabled") {
                onTangemPayIssueCardSheet { issueCardButton.assertIsEnabled() }
            }
        }
    }

    @AllureId("9732")
    @DisplayName("Tangem Pay: 'Issue an additional card?' popup blocks the issue on a zero balance")
    @Test
    fun issueCardPopupBlocksIssueWhenBalanceIsZeroTest() {
        setupHooks(
            additionalBeforeSection = {
                setWireMockScenarioState(TANGEM_PAY_ELIGIBILITY_SCENARIO, eligibilityState)
                setWireMockScenarioState(balanceScenario, balanceZeroState)
            }
        ).run {
            step("Open Tangem Pay") { openTangemPay() }
            step("Assert payment account balance contains '0.00'") {
                flakySafely(WAIT_UNTIL_TIMEOUT) {
                    onTangemPayMainScreen { balance.assertTextContainsSafe("0.00", substring = true) }
                }
            }
            step("Open the 'Issue an additional card?' sheet") { openTangemPayIssueCardSheet() }
            step("Assert issue sheet content with the '$issueFee' fee") {
                assertTangemPayIssueCardSheetContent(issueFee)
            }
            step("Assert the 'Unable to cover fee' state") { assertTangemPayIssueCardUnableToCoverFee() }
        }
    }

    @AllureId("9734")
    @DisplayName("Tangem Pay: issuing a card is unavailable when the balance is below the issue fee")
    @Test
    fun issueCardIsUnavailableWhenBalanceIsBelowFeeTest() {
        setupHooks(
            additionalBeforeSection = {
                setWireMockScenarioState(TANGEM_PAY_ELIGIBILITY_SCENARIO, eligibilityState)
                setWireMockScenarioState(balanceScenario, balanceBelowIssueFeeState)
            }
        ).run {
            step("Open Tangem Pay") { openTangemPay() }
            step("Assert payment account balance contains '0.50'") {
                flakySafely(WAIT_UNTIL_TIMEOUT) {
                    onTangemPayMainScreen { balance.assertTextContainsSafe("0.50", substring = true) }
                }
            }
            step("Open the 'Issue an additional card?' sheet") { openTangemPayIssueCardSheet() }
            step("Assert issue sheet content with the '$issueFee' fee") {
                assertTangemPayIssueCardSheetContent(issueFee)
            }
            step("Assert the 'Unable to cover fee' state") { assertTangemPayIssueCardUnableToCoverFee() }
            step("Click on the disabled 'Issue card' button") {
                onTangemPayIssueCardSheet { issueCardButton.performClick() }
            }
            step("Assert the sheet stays open with the issue still out of reach") {
                onTangemPayIssueCardSheet {
                    title.assertIsDisplayed()
                    issueCardButton.assertIsNotEnabled()
                }
            }
            step("Click on 'Add funds' button") {
                onTangemPayIssueCardSheet { addFundsButton.clickWithAssertion() }
            }
            step("Assert 'Add funds' sheet shows Swap and Receive options") {
                flakySafely(WAIT_UNTIL_TIMEOUT) {
                    onTangemPayAddFundsSheet {
                        swapOption.assertIsDisplayed()
                        receiveOption.assertIsDisplayed()
                    }
                }
            }
        }
    }

    @AllureId("9729")
    @DisplayName("Tangem Pay: issuing a second card charges the fee and shows the issuing status")
    @Test
    fun issueSecondCardShowsIssuingStatusAndFeeTransactionTest() {
        val feeMerchantName = "Card issue fee"

        setupHooks(
            additionalBeforeSection = {
                setWireMockScenarioState(TANGEM_PAY_ELIGIBILITY_SCENARIO, eligibilityState)
                setWireMockScenarioState(balanceScenario, balanceInitialState)
                setWireMockScenarioState(issueOrderScenario, issueOrderStartedState)
                setWireMockScenarioState(issueOrdersScenario, issueOrdersEmptyState)
                setWireMockScenarioState(historyScenario, historyInitialEmptyState)
            }
        ).run {
            step("Open Tangem Pay") { openTangemPay() }
            step("Open the 'Issue an additional card?' sheet") { openTangemPayIssueCardSheet() }
            step("Assert issue sheet content with the '$issueFee' fee") {
                assertTangemPayIssueCardSheetContent(issueFee)
            }
            step("Confirm the card issue") { confirmTangemPayCardIssue() }
            step("Assert the issuing banner description is displayed") {
                onTangemPayMainScreen { issuingCardBannerDescription.assertIsDisplayed() }
            }
            step("Switch WireMock scenario '$balanceScenario' to '$balanceAfterIssueFeeState'") {
                setWireMockScenarioState(balanceScenario, balanceAfterIssueFeeState)
            }
            step("Switch WireMock scenario '$historyScenario' to '$historyAfterIssueFeeState'") {
                setWireMockScenarioState(historyScenario, historyAfterIssueFeeState)
            }
            step("Pull to refresh Tangem Pay") { pullToRefreshTangemPay() }
            step("Assert the balance dropped to '9.00'") {
                flakySafely(WAIT_UNTIL_TIMEOUT_LONG) {
                    onTangemPayMainScreen { balance.assertTextContainsSafe("9.00", substring = true) }
                }
            }
            step("Open the '$feeMerchantName' transaction details") {
                openTangemPayTransactionDetails(feeMerchantName)
            }
            step("Assert the issue fee transaction details are displayed") {
                flakySafely(WAIT_UNTIL_TIMEOUT) {
                    onTangemPayTransactionDetailsSheet {
                        feeTitle.assertIsDisplayed()
                        serviceFeesCategory.assertIsDisplayed()
                        amount.assertTextContainsSafe("1.00", substring = true)
                    }
                }
            }
        }
    }

    @AllureId("10010")
    @DisplayName("Tangem Pay: the card being issued shows the in-progress screen after the order is created")
    @Test
    fun issuingCardShowsProgressScreenTest() {
        setupHooks(
            additionalBeforeSection = {
                setWireMockScenarioState(TANGEM_PAY_ELIGIBILITY_SCENARIO, eligibilityState)
                setWireMockScenarioState(balanceScenario, balanceInitialState)
                setWireMockScenarioState(issueOrderScenario, issueOrderStartedState)
                setWireMockScenarioState(issueOrdersScenario, issueOrdersEmptyState)
            }
        ).run {
            step("Open Tangem Pay") { openTangemPay() }
            step("Open the 'Issue an additional card?' sheet") { openTangemPayIssueCardSheet() }
            step("Confirm the card issue") { confirmTangemPayCardIssue() }
            step("Click on the card being issued") {
                onTangemPayMainScreen { cardButtonAt(issuingCardIndex).performClick() }
            }
            step("Assert the card page shows the issuing progress state") {
                flakySafely(WAIT_UNTIL_TIMEOUT) {
                    onTangemPayCardPageScreen { issuingInProgressBlock.assertIsDisplayed() }
                }
            }
        }
    }

    @AllureId("10018")
    @DisplayName("Tangem Pay: the card counter on the Payment account tile grows with every issued card")
    @Test
    fun cardCounterUpdatesOnPaymentAccountTileTest() {
        val oneCardSubtitle = getQuantityString(CoreResR.plurals.tangempay_cards_count, 1, 1)
        val twoCardsSubtitle = getQuantityString(CoreResR.plurals.tangempay_cards_count, 2, 2)
        val threeCardsSubtitle = getQuantityString(CoreResR.plurals.tangempay_cards_count, 3, 3)

        // The default customer/me already carries a single card, so `tangem_pay_card_issue` stays unset here.
        setupHooks(
            additionalBeforeSection = {
                setWireMockScenarioState(TANGEM_PAY_ELIGIBILITY_SCENARIO, eligibilityState)
                setWireMockScenarioState(issueOrderScenario, issueOrderStartedState)
                setWireMockScenarioState(issueOrdersScenario, issueOrdersEmptyState)
            }
        ).run {
            step("Open Main screen with the Tangem Pay tile") { openMainScreenWithTangemPayTile() }
            step("Assert Main tile shows '$oneCardSubtitle'") {
                flakySafely(WAIT_UNTIL_TIMEOUT_LONG) {
                    onTangemPayMainScreen { tileWithSubtitle(oneCardSubtitle).assertIsDisplayed() }
                }
            }
            step("Issue the second card") { issueAdditionalCardFromMainScreen(twoCardsState) }
            step("Assert Main tile shows '$twoCardsSubtitle'") {
                flakySafely(WAIT_UNTIL_TIMEOUT_LONG) {
                    onTangemPayMainScreen { tileWithSubtitle(twoCardsSubtitle).assertIsDisplayed() }
                }
            }
            step("Issue the third card") { issueAdditionalCardFromMainScreen(threeCardsState) }
            step("Assert Main tile shows '$threeCardsSubtitle'") {
                flakySafely(WAIT_UNTIL_TIMEOUT_LONG) {
                    onTangemPayMainScreen { tileWithSubtitle(threeCardsSubtitle).assertIsDisplayed() }
                }
            }
        }
    }

    @AllureId("10014")
    @DisplayName("Tangem Pay: an active card order found at launch shows the in-progress screen")
    @Test
    fun activeOrderAtLaunchShowsProgressScreenTest() {
        setupHooks(
            additionalBeforeSection = {
                setWireMockScenarioState(TANGEM_PAY_ELIGIBILITY_SCENARIO, eligibilityState)
                setWireMockScenarioState(balanceScenario, balanceInitialState)
                setWireMockScenarioState(issueOrderScenario, issueOrderStartedState)
                setWireMockScenarioState(issueOrdersScenario, issueOrdersActiveOrderState)
            }
        ).run {
            val orderLookupCountBefore = getWireMockRequestCount("GET", orderEndpoint)

            step("Open Tangem Pay") { openTangemPay() }
            step("Assert 'Issuing a new digital card' banner is displayed") {
                flakySafely(WAIT_UNTIL_TIMEOUT_LONG) {
                    onTangemPayMainScreen { issuingCardBanner.assertIsDisplayed() }
                }
            }
            step("Assert 'Add card' button is disabled") {
                onTangemPayMainScreen { addCardButton.assertIsNotEnabled() }
            }
            step("Assert active orders were looked up at launch") {
                val orderLookupCountAfter = getWireMockRequestCount("GET", orderEndpoint)
                assertTrue(
                    "The app should look up active orders via GET $orderEndpoint",
                    orderLookupCountAfter > orderLookupCountBefore,
                )
            }
            step("Click on the card being issued") {
                onTangemPayMainScreen { cardButtonAt(issuingCardIndex).performClick() }
            }
            step("Assert 'Card page' screen is displayed") {
                flakySafely(WAIT_UNTIL_TIMEOUT) { onTangemPayCardPageScreen { moreButton.assertIsDisplayed() } }
            }
            step("Assert the card page shows the issuing progress state") {
                onTangemPayCardPageScreen { issuingInProgressBlock.assertIsDisplayed() }
            }
        }
    }

    @AllureId("10008")
    @DisplayName("Tangem Pay: confirming a card issue with an active order reuses it instead of placing a new one")
    @Test
    fun issueCardWithActiveOrderDoesNotPlaceNewOrderTest() {
        setupHooks(
            additionalBeforeSection = {
                setWireMockScenarioState(TANGEM_PAY_ELIGIBILITY_SCENARIO, eligibilityState)
                setWireMockScenarioState(balanceScenario, balanceInitialState)
                setWireMockScenarioState(issueOrderScenario, issueOrderStartedState)
                setWireMockScenarioState(issueOrdersScenario, issueOrdersEmptyState)
            }
        ).run {
            step("Open Tangem Pay") { openTangemPay() }
            step("Open the 'Issue an additional card?' sheet") { openTangemPayIssueCardSheet() }
            step("Assert issue sheet content with the '$issueFee' fee") {
                assertTangemPayIssueCardSheetContent(issueFee)
            }
            step("Switch WireMock scenario '$issueOrdersScenario' to '$issueOrdersActiveOrderState'") {
                setWireMockScenarioState(issueOrdersScenario, issueOrdersActiveOrderState)
            }
            val placeOrderCountBefore = getWireMockRequestCount("POST", orderEndpoint)
            val orderLookupCountBefore = getWireMockRequestCount("GET", orderEndpoint)
            step("Confirm the card issue") { confirmTangemPayCardIssue() }
            step("Assert active orders were looked up on confirm") {
                val orderLookupCountAfter = getWireMockRequestCount("GET", orderEndpoint)
                assertTrue(
                    "Confirming the issue should look up active orders via GET $orderEndpoint",
                    orderLookupCountAfter > orderLookupCountBefore,
                )
            }
            step("Assert no new order was placed") {
                val placeOrderCountAfter = getWireMockRequestCount("POST", orderEndpoint)
                assertEquals(
                    "Confirming the issue with an active order should not send POST $orderEndpoint",
                    placeOrderCountBefore,
                    placeOrderCountAfter,
                )
            }
            step("Click on the card being issued") {
                onTangemPayMainScreen { cardButtonAt(issuingCardIndex).performClick() }
            }
            step("Assert 'Card page' screen is displayed") {
                flakySafely(WAIT_UNTIL_TIMEOUT) { onTangemPayCardPageScreen { moreButton.assertIsDisplayed() } }
            }
            step("Assert the card page shows the issuing progress state") {
                onTangemPayCardPageScreen { issuingInProgressBlock.assertIsDisplayed() }
            }
        }
    }
}