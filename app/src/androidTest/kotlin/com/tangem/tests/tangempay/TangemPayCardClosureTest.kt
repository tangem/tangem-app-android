package com.tangem.tests.tangempay

import com.tangem.common.BaseTestCase
import com.tangem.common.constants.TestConstants.TANGEM_PAY_ELIGIBILITY_SCENARIO
import com.tangem.common.constants.TestConstants.WAIT_UNTIL_TIMEOUT
import com.tangem.common.constants.TestConstants.WAIT_UNTIL_TIMEOUT_LONG
import com.tangem.common.utils.getWireMockRequestCount
import com.tangem.common.utils.setWireMockScenarioState
import com.tangem.scenarios.*
import com.tangem.screens.tangempay.*
import dagger.hilt.android.testing.HiltAndroidTest
import io.qameta.allure.kotlin.AllureId
import io.qameta.allure.kotlin.junit4.DisplayName
import org.junit.Assert.assertTrue
import org.junit.Test

@HiltAndroidTest
class TangemPayCardClosureTest : BaseTestCase() {

    private val eligibilityState = "PaeraCustomer"

    private val cardIssueScenario = "tangem_pay_card_issue"
    private val cardIssueTwoCardsState = "TwoCards"

    private val closeScenario = "tangem_pay_card_close"
    private val closeTwoCardsState = "TwoCards"
    private val closeSecondCardFrozenState = "SecondCardFrozen"
    private val closeAfterState = "AfterCardClose"

    private val closeOrderScenario = "tangem_pay_close_card_order"
    private val closeOrderProcessingState = "Started"
    private val closeOrderCompletedState = "Completed"

    private val closeEndpointPattern = "/bff-v2/v1/customer/card/close"
    private val closeOrderEndpointPattern = "/bff-v2/v1/order/mock-close-order-001"

    private val firstCardDigits = "4242"
    private val secondCardDigits = "5252"
    private val secondCardIndex = 1

    @AllureId("10104")
    @DisplayName("Tangem Pay: 'Close card' is inactive for the last card of the payment account")
    @Test
    fun closeCardIsInactiveForTheLastCardTest() {
        setupHooks(
            additionalBeforeSection = {
                setWireMockScenarioState(TANGEM_PAY_ELIGIBILITY_SCENARIO, eligibilityState)
            },
        ).run {
            step("Open Tangem Pay card page") { openTangemPayCardPage() }
            step("Open the card more-actions menu") { openTangemPayCardMenu() }
            // The menu item has no disabled semantics — an inactive one simply carries no click action.
            step("Assert 'Close card' menu item is not clickable") {
                onTangemPayCardPageScreen { closeCardMenuItem.assertHasNoClickAction() }
            }
            step("Assert the last-card hint is displayed under 'Close card'") {
                onTangemPayCardPageScreen { closeCardMenuItemLastCardHint.assertIsDisplayed() }
            }
        }
    }

    @AllureId("10106")
    @DisplayName("Tangem Pay: 'Close card' opens the 'Close your card?' bottom sheet")
    @Test
    fun closeCardOpensConfirmationSheetTest() {
        setupHooks(
            additionalBeforeSection = {
                setWireMockScenarioState(TANGEM_PAY_ELIGIBILITY_SCENARIO, eligibilityState)
                setWireMockScenarioState(cardIssueScenario, cardIssueTwoCardsState)
            },
        ).run {
            step("Open the card page of the card ending with '$secondCardDigits'") {
                openTangemPayCardPageAt(index = secondCardIndex, expectedLastDigits = secondCardDigits)
            }
            step("Open the 'Close your card?' bottom sheet") { openTangemPayCloseCardSheet() }
            step("Assert the 'Close your card?' bottom sheet content") {
                assertTangemPayCloseCardSheetContent()
            }
        }
    }

    @AllureId("10102")
    @DisplayName("Tangem Pay: closing an active card returns to the previous one and drops it from the account")
    @Test
    fun closeActiveCardTest() {
        setupHooks(
            additionalBeforeSection = {
                setWireMockScenarioState(TANGEM_PAY_ELIGIBILITY_SCENARIO, eligibilityState)
                setWireMockScenarioState(closeScenario, closeTwoCardsState)
                setWireMockScenarioState(closeOrderScenario, closeOrderProcessingState)
            },
        ).run {
            step("Open the card page of the card ending with '$secondCardDigits'") {
                openTangemPayCardPageAt(index = secondCardIndex, expectedLastDigits = secondCardDigits)
            }
            step("Open the 'Close your card?' bottom sheet") { openTangemPayCloseCardSheet() }
            val closeRequestsBefore = getWireMockRequestCount("POST", closeEndpointPattern)
            val orderPollsBefore = getWireMockRequestCount("GET", closeOrderEndpointPattern)
            step("Click on 'Close card' button") { confirmTangemPayCardClose() }
            step("Assert the card closure was requested") {
                val closeRequestsAfter = getWireMockRequestCount("POST", closeEndpointPattern)
                assertTrue(
                    "Confirming the closure should POST $closeEndpointPattern",
                    closeRequestsAfter > closeRequestsBefore,
                )
            }
            step("Assert the close order is being polled") {
                awaitSuccess(timeoutMillis = WAIT_UNTIL_TIMEOUT_LONG) {
                    val polls = getWireMockRequestCount("GET", closeOrderEndpointPattern) - orderPollsBefore
                    if (polls <= 0) throw AssertionError("Closing the card should start polling its order")
                }
            }
            // The customer state goes first: the completed order refetches it immediately, and that response
            // is what drops the closed card.
            step("Switch WireMock scenario '$closeScenario' to '$closeAfterState'") {
                setWireMockScenarioState(closeScenario, closeAfterState)
            }
            step("Switch WireMock scenario '$closeOrderScenario' to '$closeOrderCompletedState'") {
                setWireMockScenarioState(closeOrderScenario, closeOrderCompletedState)
            }
            step("Assert the closed card is gone from the payment account") {
                assertTangemPayCardClosed(
                    closedLastDigits = secondCardDigits,
                    remainingLastDigits = firstCardDigits,
                )
            }
        }
    }

    @AllureId("10103")
    @DisplayName("Tangem Pay: a frozen card is closed and the account falls back to the previous card")
    @Test
    fun closeFrozenCardTest() {
        setupHooks(
            additionalBeforeSection = {
                setWireMockScenarioState(TANGEM_PAY_ELIGIBILITY_SCENARIO, eligibilityState)
                setWireMockScenarioState(closeScenario, closeSecondCardFrozenState)
                setWireMockScenarioState(closeOrderScenario, closeOrderProcessingState)
            },
        ).run {
            step("Open the card page of the frozen card ending with '$secondCardDigits'") {
                openTangemPayCardPageAt(index = secondCardIndex, expectedLastDigits = secondCardDigits)
            }
            step("Assert the opened card is frozen") {
                flakySafely(WAIT_UNTIL_TIMEOUT) {
                    onTangemPayCardPageScreen { unfreezeCardRow.assertIsDisplayed() }
                }
            }
            step("Open the 'Close your card?' bottom sheet") { openTangemPayCloseCardSheet() }
            val closeRequestsBefore = getWireMockRequestCount("POST", closeEndpointPattern)
            val orderPollsBefore = getWireMockRequestCount("GET", closeOrderEndpointPattern)
            step("Click on 'Close card' button") { confirmTangemPayCardClose() }
            step("Assert the card closure was requested") {
                val closeRequestsAfter = getWireMockRequestCount("POST", closeEndpointPattern)
                assertTrue(
                    "Confirming the closure should POST $closeEndpointPattern",
                    closeRequestsAfter > closeRequestsBefore,
                )
            }
            step("Assert the close order is being polled") {
                awaitSuccess(timeoutMillis = WAIT_UNTIL_TIMEOUT_LONG) {
                    val polls = getWireMockRequestCount("GET", closeOrderEndpointPattern) - orderPollsBefore
                    if (polls <= 0) throw AssertionError("Closing the card should start polling its order")
                }
            }
            step("Switch WireMock scenario '$closeScenario' to '$closeAfterState'") {
                setWireMockScenarioState(closeScenario, closeAfterState)
            }
            step("Switch WireMock scenario '$closeOrderScenario' to '$closeOrderCompletedState'") {
                setWireMockScenarioState(closeOrderScenario, closeOrderCompletedState)
            }
            step("Assert the closed card is gone from the payment account") {
                assertTangemPayCardClosed(
                    closedLastDigits = secondCardDigits,
                    remainingLastDigits = firstCardDigits,
                )
            }
        }
    }
}