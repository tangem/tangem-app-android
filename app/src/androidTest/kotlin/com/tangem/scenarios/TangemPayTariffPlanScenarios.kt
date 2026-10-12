package com.tangem.scenarios

import androidx.test.espresso.Espresso
import com.tangem.common.BaseTestCase
import com.tangem.common.constants.TestConstants.TANGEM_PAY_ELIGIBILITY_SCENARIO
import com.tangem.common.constants.TestConstants.WAIT_UNTIL_TIMEOUT
import com.tangem.common.constants.TestConstants.WAIT_UNTIL_TIMEOUT_LONG
import com.tangem.common.extensions.clickWithAssertion
import com.tangem.common.extensions.isDisplayedSafely
import com.tangem.common.utils.getWireMockRequestCount
import com.tangem.common.utils.setWireMockScenarioState
import com.tangem.core.res.R as CoreResR
import com.tangem.screens.tangempay.*
import io.github.kakaocup.kakao.common.utilities.getResourceString
import io.qameta.allure.kotlin.Allure.step
import org.junit.Assert

private const val PLAN_SCENARIO = "tangem_pay_tariff_plan"
private const val TRANSITIONS_SCENARIO = "tangem_pay_tariff_plan_transitions"
private const val ORDERS_SCENARIO = "tangem_pay_tariff_plan_orders"
private const val ORDER_SCENARIO = "tangem_pay_tariff_plan_order"
private const val OFFERS_SCENARIO = "tangem_pay_tariff_plan_offers"

/** The Basic transition order is polled by its own id, so it has a scenario of its own. */
private const val BASIC_ORDER_SCENARIO = "tangem_pay_tariff_plan_basic_order"

/** The card-issue order list is drained explicitly, so an issue order never masks the tariff-plan one. */
private const val CARD_ISSUE_ORDERS_SCENARIO = "tangem_pay_card_issue_orders"

private const val HISTORY_SCENARIO = "tangem_pay_transaction_history"

private const val ELIGIBILITY_PAERA_CUSTOMER_STATE = "PaeraCustomer"
private const val HISTORY_INITIAL_EMPTY_STATE = "InitialEmpty"

private const val ORDERS_EMPTY_STATE = "Empty"
private const val ORDERS_AWAITING_DEPOSIT_STATE = "AwaitingDeposit"
private const val ORDERS_PLUS_PROCESSING_STATE = "PlusProcessing"
private const val ORDER_STARTED_STATE = "Started"
private const val ORDER_AWAITING_DEPOSIT_STATE = "AwaitingDeposit"
private const val ORDER_COMPLETED_STATE = "Completed"
private const val BASIC_ORDER_PROCESSING_STATE = "Processing"
private const val BASIC_ORDER_COMPLETED_STATE = "Completed"
private const val HISTORY_AFTER_PLAN_FEE_STATE = "AfterPlanFee"
private const val PLAN_BASIC_STATE = "Basic"

private const val PLACE_ORDER_ENDPOINT = "/bff-v2/v1/order"
private const val CANCEL_ORDER_ENDPOINT = "/bff-v2/v1/order/mock-tariff-plan-order-001/cancel"
private const val PENDING_TRANSITION_ENDPOINT = "/bff-v2/v1/customer/tariff-plan/pending-transition"
private const val CANCEL_PENDING_TRANSITION_ENDPOINT = "/bff-v2/v1/customer/tariff-plan/pending-transition/cancel"

/**
 * WireMock state every tariff-plan test starts from: an eligible Pay customer on an empty transaction
 * history, plus the tariff plan itself. [offers] is only set by the cards-limit flows.
 */
fun setTangemPayTariffPlanScenarios(
    plan: String,
    transitions: String,
    orders: String = ORDERS_EMPTY_STATE,
    order: String = ORDER_STARTED_STATE,
    offers: String? = null,
) {
    step("Set WireMock scenario '$TANGEM_PAY_ELIGIBILITY_SCENARIO' to '$ELIGIBILITY_PAERA_CUSTOMER_STATE'") {
        setWireMockScenarioState(TANGEM_PAY_ELIGIBILITY_SCENARIO, ELIGIBILITY_PAERA_CUSTOMER_STATE)
    }
    step("Set WireMock scenario '$HISTORY_SCENARIO' to '$HISTORY_INITIAL_EMPTY_STATE'") {
        setWireMockScenarioState(HISTORY_SCENARIO, HISTORY_INITIAL_EMPTY_STATE)
    }
    step("Set WireMock scenario '$PLAN_SCENARIO' to '$plan'") {
        setWireMockScenarioState(PLAN_SCENARIO, plan)
    }
    step("Set WireMock scenario '$TRANSITIONS_SCENARIO' to '$transitions'") {
        setWireMockScenarioState(TRANSITIONS_SCENARIO, transitions)
    }
    step("Set WireMock scenario '$ORDERS_SCENARIO' to '$orders'") {
        setWireMockScenarioState(ORDERS_SCENARIO, orders)
    }
    step("Set WireMock scenario '$ORDER_SCENARIO' to '$order'") {
        setWireMockScenarioState(ORDER_SCENARIO, order)
    }
    step("Set WireMock scenario '$CARD_ISSUE_ORDERS_SCENARIO' to '$ORDERS_EMPTY_STATE'") {
        setWireMockScenarioState(CARD_ISSUE_ORDERS_SCENARIO, ORDERS_EMPTY_STATE)
    }
    // Only Processing and Completed are mapped, so the reset default would leave the order poll unanswered.
    step("Set WireMock scenario '$BASIC_ORDER_SCENARIO' to '$BASIC_ORDER_COMPLETED_STATE'") {
        setWireMockScenarioState(BASIC_ORDER_SCENARIO, BASIC_ORDER_COMPLETED_STATE)
    }
    offers?.let {
        step("Set WireMock scenario '$OFFERS_SCENARIO' to '$it'") {
            setWireMockScenarioState(OFFERS_SCENARIO, it)
        }
    }
}

/**
 * Moves the customer to [plan]. Staged before the action that triggers it, because the app refetches
 * customer info as soon as the request returns.
 */
fun stageTangemPayPlanState(plan: String) {
    step("Switch WireMock scenario '$PLAN_SCENARIO' to '$plan'") {
        setWireMockScenarioState(PLAN_SCENARIO, plan)
    }
}

/** Puts the plan transition order in 'awaiting deposit', the state behind the top-up banner. */
fun stageTangemPayPlanFeeAwaitingDeposit(plan: String = PLAN_BASIC_STATE) {
    stageTangemPayPlanState(plan)
    step("Switch WireMock scenario '$ORDERS_SCENARIO' to '$ORDERS_AWAITING_DEPOSIT_STATE'") {
        setWireMockScenarioState(ORDERS_SCENARIO, ORDERS_AWAITING_DEPOSIT_STATE)
    }
    step("Switch WireMock scenario '$ORDER_SCENARIO' to '$ORDER_AWAITING_DEPOSIT_STATE'") {
        setWireMockScenarioState(ORDER_SCENARIO, ORDER_AWAITING_DEPOSIT_STATE)
    }
}

/** Completes the plan transition order and lands the customer on [plan]. */
fun stageTangemPayPlanOrderCompleted(plan: String) {
    stageTangemPayPlanState(plan)
    step("Switch WireMock scenario '$ORDER_SCENARIO' to '$ORDER_COMPLETED_STATE'") {
        setWireMockScenarioState(ORDER_SCENARIO, ORDER_COMPLETED_STATE)
    }
    step("Switch WireMock scenario '$ORDERS_SCENARIO' to '$ORDERS_EMPTY_STATE'") {
        setWireMockScenarioState(ORDERS_SCENARIO, ORDERS_EMPTY_STATE)
    }
}

/** Only the polled order: the list is flipped by the POST stub, so staging it here would race that. */
fun stageTangemPayBasicOrderPolling() {
    step("Switch WireMock scenario '$BASIC_ORDER_SCENARIO' to '$BASIC_ORDER_PROCESSING_STATE'") {
        setWireMockScenarioState(BASIC_ORDER_SCENARIO, BASIC_ORDER_PROCESSING_STATE)
    }
}

/** Finishes the Basic card issuance the account was waiting on. */
fun completeTangemPayBasicOrderIssuing() {
    step("Switch WireMock scenario '$BASIC_ORDER_SCENARIO' to '$BASIC_ORDER_COMPLETED_STATE'") {
        setWireMockScenarioState(BASIC_ORDER_SCENARIO, BASIC_ORDER_COMPLETED_STATE)
    }
    step("Switch WireMock scenario '$ORDERS_SCENARIO' to '$ORDERS_EMPTY_STATE'") {
        setWireMockScenarioState(ORDERS_SCENARIO, ORDERS_EMPTY_STATE)
    }
}

/** Moves the paid Plus transition order from 'awaiting deposit' to 'processing'. */
fun stageTangemPayPlusOrderProcessing() {
    step("Switch WireMock scenario '$ORDER_SCENARIO' to '$ORDER_STARTED_STATE'") {
        setWireMockScenarioState(ORDER_SCENARIO, ORDER_STARTED_STATE)
    }
    step("Switch WireMock scenario '$ORDERS_SCENARIO' to '$ORDERS_PLUS_PROCESSING_STATE'") {
        setWireMockScenarioState(ORDERS_SCENARIO, ORDERS_PLUS_PROCESSING_STATE)
    }
}

/** Completes the Plus transition order and puts the plan fee it charged into the transaction history. */
fun completeTangemPayPlusOrderWithPlanFee(plan: String) {
    step("Switch WireMock scenario '$HISTORY_SCENARIO' to '$HISTORY_AFTER_PLAN_FEE_STATE'") {
        setWireMockScenarioState(HISTORY_SCENARIO, HISTORY_AFTER_PLAN_FEE_STATE)
    }
    stageTangemPayPlanOrderCompleted(plan)
}

/** Called after the cancel tap: the cancel stub flips the polled order, the list is drained here. */
fun drainTangemPayPlanOrders() {
    step("Switch WireMock scenario '$ORDERS_SCENARIO' to '$ORDERS_EMPTY_STATE'") {
        setWireMockScenarioState(ORDERS_SCENARIO, ORDERS_EMPTY_STATE)
    }
}

fun getTangemPayPlanOrderRequestCount(): Int = getWireMockRequestCount("POST", PLACE_ORDER_ENDPOINT)

fun getTangemPayCancelPlanOrderRequestCount(): Int = getWireMockRequestCount("POST", CANCEL_ORDER_ENDPOINT)

fun getTangemPayPendingTransitionRequestCount(): Int =
    getWireMockRequestCount("POST", PENDING_TRANSITION_ENDPOINT)

fun getTangemPayCancelPendingTransitionRequestCount(): Int =
    getWireMockRequestCount("POST", CANCEL_PENDING_TRANSITION_ENDPOINT)

/** The tap only starts the request, so the journal is polled before the exactly-once check. */
fun BaseTestCase.assertTangemPayTariffRequestSentOnce(name: String, countBefore: Int, count: () -> Int) {
    awaitSuccess {
        if (count() - countBefore < 1) throw AssertionError("The $name request should be sent")
    }
    Assert.assertEquals("The $name request should be sent once", 1, count() - countBefore)
}

/** From the Main screen, opens plan selection through the 'Select plan' Tangem Pay tile. */
fun BaseTestCase.openTangemPaySelectPlanFromOnboarding() {
    val selectPlanSubtitle = getResourceString(CoreResR.string.tangempay_main_select_plan)

    step("Click on Tangem Pay '$selectPlanSubtitle' tile") {
        awaitSuccess(timeoutMillis = WAIT_UNTIL_TIMEOUT_LONG) {
            onTangemPayMainScreen { tileWithSubtitle(selectPlanSubtitle).assertIsDisplayed() }
        }
        onTangemPayMainScreen { tileWithSubtitle(selectPlanSubtitle).performClick() }
    }
    step("Assert 'Select plan' screen is displayed") {
        awaitSuccess { onTangemPaySelectPlanScreen { title.assertIsDisplayed() } }
    }
}

/** Opens 'Current plan' from the top bar menu; [assertMenuItems] also checks the items it offers. */
fun BaseTestCase.openTangemPayCurrentPlan(assertMenuItems: Boolean = false) {
    step("Click on 'More' button") {
        awaitSuccess { onTangemPayMainScreen { moreActionsButton.assertIsDisplayed() } }
        onTangemPayMainScreen { moreActionsButton.performClick() }
    }
    if (assertMenuItems) {
        step("Assert the more-actions menu offers 'Current plan' and 'Support'") {
            awaitSuccess { onTangemPayMainScreen { currentPlanMenuItem.assertIsDisplayed() } }
            onTangemPayMainScreen { paySupportMenuItem.assertIsDisplayed() }
        }
    }
    step("Click on 'Current plan' menu item") {
        awaitSuccess { onTangemPayMainScreen { currentPlanMenuItem.assertIsDisplayed() } }
        onTangemPayMainScreen { currentPlanMenuItem.performClick() }
    }
    step("Assert 'Current plan' screen is displayed") {
        awaitSuccess { onTangemPayCurrentPlanScreen { screen.assertIsDisplayed() } }
    }
}

/** From the open 'Current plan' screen, opens plan selection. */
fun BaseTestCase.openTangemPayChangePlan() {
    step("Click on 'Change plan' button") {
        awaitSuccess { onTangemPayCurrentPlanScreen { changePlanButton.assertIsDisplayed() } }
        onTangemPayCurrentPlanScreen { changePlanButton.performClick() }
    }
    step("Assert 'Select plan' screen is displayed") {
        awaitSuccess { onTangemPaySelectPlanScreen { title.assertIsDisplayed() } }
    }
}

/** On the open plan selection, opens the comparison sheet and waits for [expectedPlanName] to render. */
fun BaseTestCase.openTangemPayComparePlans(expectedPlanName: String) {
    step("Click on 'Compare plans' button") {
        onTangemPaySelectPlanScreen { compareButton.clickWithAssertion() }
    }
    step("Assert 'Compare plans' sheet is displayed") {
        awaitSuccess { onTangemPayComparePlansSheet { planRow(expectedPlanName).assertIsDisplayed() } }
    }
}

/** On the open plan selection, taps 'Select' and waits for the confirmation step. */
fun BaseTestCase.selectTangemPayPlanForConfirmation() {
    step("Click on 'Select' button") {
        onTangemPaySelectPlanScreen { selectButton.clickWithAssertion() }
    }
    step("Assert plan confirmation button is displayed") {
        awaitSuccess { onTangemPaySelectPlanScreen { confirmButton.assertIsDisplayed() } }
    }
    step("Assert plan cancellation button is displayed") {
        onTangemPaySelectPlanScreen { cancelButton.assertIsDisplayed() }
    }
}

/** On the open payment account, taps 'Add card'. The caller asserts which cards-limit popup comes up. */
fun BaseTestCase.openTangemPayCardsLimitSheet() {
    step("Click on 'Add card' button") {
        awaitSuccess { onTangemPayMainScreen { addCardButton.assertIsDisplayed() } }
        onTangemPayMainScreen { addCardButton.performClick() }
    }
}

/** Leaves the 'Current plan' screen and waits for the payment account behind it. */
fun BaseTestCase.pressBackFromCurrentPlan() {
    // The plan model drops back while it is still finishing a request, and nothing retries a swallowed
    // press — so the loop presses again, but only while the account it leads to is still not up.
    composeTestRule.waitUntil(timeoutMillis = WAIT_UNTIL_TIMEOUT_LONG) {
        if (runCatching { onTangemPayMainScreen { balance.assertIsDisplayed() } }.isSuccess) {
            return@waitUntil true
        }
        onTangemPayCurrentPlanScreen { if (screen.isDisplayedSafely()) Espresso.pressBack() }
        false
    }
}

fun BaseTestCase.assertTangemPayCurrentPlanName(expectedPlanName: String) {
    awaitSuccess(timeoutMillis = WAIT_UNTIL_TIMEOUT) {
        onTangemPayCurrentPlanScreen { planName(expectedPlanName).assertIsDisplayed() }
    }
}

/** Waits for the top-up banner to leave the payment account — the proof a transition order is gone. */
fun BaseTestCase.assertTangemPayAwaitingDepositBannerHidden(planFee: String) {
    awaitSuccess(timeoutMillis = WAIT_UNTIL_TIMEOUT_LONG) {
        onTangemPayMainScreen { awaitingDepositBannerTitle(planFee).assertDoesNotExist() }
    }
}

/** Waits for the card-issuing banner, the payment account's proof a transition order is in progress. */
fun BaseTestCase.assertTangemPayIssuingCardBanner() {
    step("Assert 'Issuing new digital card' banner is displayed") {
        awaitSuccess(timeoutMillis = WAIT_UNTIL_TIMEOUT_LONG) {
            onTangemPayMainScreen { issuingCardBanner.assertIsDisplayed() }
        }
    }
}

/** Waits for the card-issuing banner to leave the payment account — the order it stood for is done. */
fun BaseTestCase.assertTangemPayIssuingCardBannerHidden() {
    step("Assert 'Issuing new digital card' banner is not displayed") {
        awaitSuccess(timeoutMillis = WAIT_UNTIL_TIMEOUT_LONG) {
            onTangemPayMainScreen { issuingCardBanner.assertDoesNotExist() }
        }
    }
}

/** From the top-up banner, opens the 'Add funds' sheet and closes it again. */
fun BaseTestCase.checkTangemPayAddFundsFromTopUpBanner() {
    step("Click on 'Add funds' button on the top-up banner") {
        awaitSuccess { onTangemPayMainScreen { awaitingDepositBannerAddFundsButton.assertIsDisplayed() } }
        onTangemPayMainScreen { awaitingDepositBannerAddFundsButton.performClick() }
    }
    // 'Add funds' labels the action button and the banner button too, so the sheet is recognised by its rows.
    step("Assert 'Add funds' sheet is displayed") {
        awaitSuccess { onTangemPayAddFundsSheet { optionAt(index = 0).assertIsDisplayed() } }
    }
    step("Click on 'Close' button") {
        onTangemPayAddFundsSheet { closeButton.clickWithAssertion() }
    }
    step("Assert 'Add funds' sheet is not displayed") {
        awaitSuccess { onTangemPayAddFundsSheet { optionAt(index = 0).assertDoesNotExist() } }
    }
}