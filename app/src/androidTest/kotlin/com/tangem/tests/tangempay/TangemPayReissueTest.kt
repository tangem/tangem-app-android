package com.tangem.tests.tangempay

import com.tangem.common.BaseTestCase
import com.tangem.common.constants.TestConstants.TANGEM_PAY_ELIGIBILITY_SCENARIO
import com.tangem.common.constants.TestConstants.WAIT_UNTIL_TIMEOUT
import com.tangem.common.constants.TestConstants.WAIT_UNTIL_TIMEOUT_VERY_LONG
import com.tangem.common.extensions.assertTextContainsSafe
import com.tangem.common.extensions.clickWithAssertion
import com.tangem.common.extensions.extractText
import com.tangem.common.utils.resetWireMockScenarioState
import com.tangem.common.utils.resetWireMockScenarios
import com.tangem.common.utils.setWireMockScenarioState
import com.tangem.scenarios.*
import com.tangem.screens.onTokenReceiveWarningBottomSheet
import com.tangem.screens.tangempay.*
import dagger.hilt.android.testing.HiltAndroidTest
import io.qameta.allure.kotlin.AllureId
import io.qameta.allure.kotlin.junit4.DisplayName
import org.junit.Assert
import org.junit.Test

@HiltAndroidTest
class TangemPayReissueTest : BaseTestCase() {

    private val eligibilityState = "PaeraCustomer"
    private val balanceScenario = "tangem_pay_balance_update"
    private val balanceInitialState = "InitialBalance"
    private val cardReissueScenario = "tangem_pay_card_reissue"
    private val cardReissueStartedState = "Started"
    private val cardReissueFeeErrorState = "FeeError"
    private val reissueOrderScenario = "tangem_pay_reissue_order"
    private val reissueOrderStartedState = "Started"
    private val reissueOrderCompletedState = "Completed"
    private val reissueScenario = "tangem_pay_reissue"
    private val reissueCompletedState = "Completed"
    private val historyScenario = "tangem_pay_transaction_history"
    private val historyInitialEmptyState = "InitialEmpty"
    private val historyAfterReissueFeeState = "AfterReissueFee"

    @AllureId("9749")
    @DisplayName("Tangem Pay: card reissue bottom sheet displays replace card details")
    @Test
    fun reissueBottomSheetDisplaysReplaceCardDetailsTest() {
        setupHooks(
            additionalBeforeSection = {
                resetWireMockScenarios()
                setWireMockScenarioState(TANGEM_PAY_ELIGIBILITY_SCENARIO, eligibilityState)
                setWireMockScenarioState(balanceScenario, balanceInitialState)
                setWireMockScenarioState(cardReissueScenario, cardReissueStartedState)
            },
            additionalAfterSection = {
                resetWireMockScenarioState(TANGEM_PAY_ELIGIBILITY_SCENARIO)
                resetWireMockScenarioState(balanceScenario)
                resetWireMockScenarioState(cardReissueScenario)
            },
        ).run {
            step("Open Tangem Pay card page") { openTangemPayCardPage() }
            step("Open the 'Replace card' reissue bottom sheet") { openReissueSheet() }
            step("Assert reissue sheet title, description and fee label are displayed") {
                onTangemPayReissueSheet {
                    title.assertIsDisplayed()
                    description.assertIsDisplayed()
                    feeLabel.assertIsDisplayed()
                }
            }
            step("Assert replacement fee value is displayed") {
                flakySafely(WAIT_UNTIL_TIMEOUT) {
                    onTangemPayReissueSheet { feeValue.assertTextContainsSafe("1.00", substring = true) }
                }
            }
        }
    }

    @AllureId("9833")
    @DisplayName("Tangem Pay: error is shown when reissue cost is not fetched")
    @Test
    fun reissueFeeErrorShowsErrorWhenFeeRequestFailsTest() {
        setupHooks(
            additionalBeforeSection = {
                resetWireMockScenarios()
                setWireMockScenarioState(TANGEM_PAY_ELIGIBILITY_SCENARIO, eligibilityState)
                setWireMockScenarioState(cardReissueScenario, cardReissueFeeErrorState)
            },
            additionalAfterSection = {
                resetWireMockScenarioState(TANGEM_PAY_ELIGIBILITY_SCENARIO)
                resetWireMockScenarioState(cardReissueScenario)
            },
        ).run {
            step("Open Tangem Pay card page") { openTangemPayCardPage() }
            step("Open the 'Replace card' reissue bottom sheet") { openReissueSheet() }
            step("Assert fee unreachable error and 'Refresh' button are displayed") {
                flakySafely(WAIT_UNTIL_TIMEOUT) {
                    onTangemPayReissueSheet {
                        feeErrorTitle.assertIsDisplayed()
                        refreshButton.assertIsDisplayed()
                    }
                }
            }
        }
    }

    @AllureId("9752")
    @DisplayName("Tangem Pay: reissue fee transaction appears in history and details")
    @Test
    fun reissueFeeTransactionAppearsInHistoryAndDetailsTest() {
        val feeMerchantName = "Card replacement fee"

        setupHooks(
            additionalBeforeSection = {
                resetWireMockScenarios()
                setWireMockScenarioState(TANGEM_PAY_ELIGIBILITY_SCENARIO, eligibilityState)
                setWireMockScenarioState(balanceScenario, balanceInitialState)
                setWireMockScenarioState(cardReissueScenario, cardReissueStartedState)
                setWireMockScenarioState(reissueOrderScenario, reissueOrderStartedState)
                setWireMockScenarioState(historyScenario, historyInitialEmptyState)
            },
            additionalAfterSection = {
                resetWireMockScenarioState(TANGEM_PAY_ELIGIBILITY_SCENARIO)
                resetWireMockScenarioState(balanceScenario)
                resetWireMockScenarioState(cardReissueScenario)
                resetWireMockScenarioState(reissueOrderScenario)
                resetWireMockScenarioState(historyScenario)
            },
        ).run {
            step("Open Tangem Pay card page") { openTangemPayCardPage() }
            step("Open the 'Replace card' reissue bottom sheet") { openReissueSheet() }
            step("Wait for replacement fee to load") {
                flakySafely(WAIT_UNTIL_TIMEOUT) {
                    onTangemPayReissueSheet { feeValue.assertTextContainsSafe("1.00", substring = true) }
                }
            }
            step("Click on 'Replace card' confirm button") {
                onTangemPayReissueSheet { confirmButton.clickWithAssertion() }
            }
            step("Assert reissue started and the sheet is dismissed") {
                flakySafely(WAIT_UNTIL_TIMEOUT) {
                    onTangemPayCardPageScreen { reissueInProgressBlock.assertIsDisplayed() }
                }
            }
            step("Return to the payment account screen") {
                device.uiDevice.pressBack()
                flakySafely(WAIT_UNTIL_TIMEOUT) {
                    onTangemPayMainScreen { balance.assertIsDisplayed() }
                }
            }
            step("Switch WireMock scenario '$historyScenario' to '$historyAfterReissueFeeState'") {
                setWireMockScenarioState(historyScenario, historyAfterReissueFeeState)
            }
            step("Pull to refresh Tangem Pay") { pullToRefreshTangemPay() }
            step("Assert '$feeMerchantName' transaction is displayed") {
                flakySafely(WAIT_UNTIL_TIMEOUT) {
                    onTangemPayMainScreen { transactionRowWithText(feeMerchantName).assertIsDisplayed() }
                }
            }
            step("Click on '$feeMerchantName' transaction row") {
                onTangemPayMainScreen { transactionRowWithText(feeMerchantName).performClick() }
            }
            step("Assert fee transaction details are displayed") {
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

    @AllureId("9743")
    @DisplayName("Tangem Pay: card reissue end-to-end replaces card with new details")
    @Test
    fun reissueEndToEndReplacesCardWithNewDetailsTest() {
        val reissuedCardLastDigits = "5353"
        val balanceAfterWithdrawState = "AfterWithdraw"

        // AfterWithdraw funds the fee check without a customer/me stub, so reissued customer/me wins.
        setupHooks(
            additionalBeforeSection = {
                resetWireMockScenarios()
                setWireMockScenarioState(TANGEM_PAY_ELIGIBILITY_SCENARIO, eligibilityState)
                setWireMockScenarioState(balanceScenario, balanceAfterWithdrawState)
                setWireMockScenarioState(cardReissueScenario, cardReissueStartedState)
                setWireMockScenarioState(reissueOrderScenario, reissueOrderStartedState)
            },
            additionalAfterSection = {
                resetWireMockScenarioState(TANGEM_PAY_ELIGIBILITY_SCENARIO)
                resetWireMockScenarioState(balanceScenario)
                resetWireMockScenarioState(cardReissueScenario)
                resetWireMockScenarioState(reissueOrderScenario)
                resetWireMockScenarioState(reissueScenario)
            },
        ).run {
            var oldCardNumber = ""
            step("Open Tangem Pay card page") { openTangemPayCardPage() }
            step("Read the current card number") {
                onTangemPayCardPageScreen { cardNumberShort.assertIsDisplayed() }
                onTangemPayCardPageScreen { oldCardNumber = cardNumberShort.extractText() }
            }
            step("Open the 'Replace card' reissue bottom sheet") { openReissueSheet() }
            step("Wait for replacement fee to load") {
                flakySafely(WAIT_UNTIL_TIMEOUT) {
                    onTangemPayReissueSheet { feeValue.assertTextContainsSafe("1.00", substring = true) }
                }
            }
            step("Click on 'Replace card' confirm button") {
                onTangemPayReissueSheet { confirmButton.clickWithAssertion() }
            }
            step("Assert reissue started (sheet dismissed, card is replacing)") {
                flakySafely(WAIT_UNTIL_TIMEOUT) {
                    onTangemPayReissueSheet { confirmButton.assertDoesNotExist() }
                    onTangemPayCardPageScreen { reissueInProgressBlock.assertIsDisplayed() }
                }
            }
            step("Switch WireMock scenario '$reissueOrderScenario' to '$reissueOrderCompletedState'") {
                setWireMockScenarioState(reissueOrderScenario, reissueOrderCompletedState)
            }
            step("Switch WireMock scenario '$reissueScenario' to '$reissueCompletedState'") {
                setWireMockScenarioState(reissueScenario, reissueCompletedState)
            }
            step("Return to the payment account screen") {
                device.uiDevice.pressBack()
                flakySafely(WAIT_UNTIL_TIMEOUT) {
                    onTangemPayMainScreen { balance.assertIsDisplayed() }
                }
            }
            step("Refresh until the card leaves the replacing state") {
                pullToRefreshTangemPay()
                flakySafely(WAIT_UNTIL_TIMEOUT_VERY_LONG) {
                    onTangemPayMainScreen { reissueInProgressBanner.assertDoesNotExist() }
                }
            }
            step("Reopen the reissued card") {
                onTangemPayMainScreen { cardButton.clickWithAssertion() }
            }
            var newCardNumber = ""
            step("Read the new card number") {
                flakySafely(WAIT_UNTIL_TIMEOUT) {
                    onTangemPayCardPageScreen {
                        cardNumberShort.assertTextContainsSafe(reissuedCardLastDigits, substring = true)
                    }
                }
                onTangemPayCardPageScreen { newCardNumber = cardNumberShort.extractText() }
            }
            step("Assert the card number changed after reissue") {
                Assert.assertNotEquals(oldCardNumber, newCardNumber)
            }
        }
    }

    @AllureId("9746")
    @DisplayName("Tangem Pay: top up via card reissue when unable to cover fee")
    @Test
    fun reissueTopUpUnableToCoverFeeAddFundsReceiveTest() {
        val depositNetworkName = "Polygon"

        setupHooks(
            additionalBeforeSection = {
                resetWireMockScenarios()
                setWireMockScenarioState(TANGEM_PAY_ELIGIBILITY_SCENARIO, eligibilityState)
                setWireMockScenarioState(cardReissueScenario, cardReissueStartedState)
            },
            additionalAfterSection = {
                resetWireMockScenarioState(TANGEM_PAY_ELIGIBILITY_SCENARIO)
                resetWireMockScenarioState(cardReissueScenario)
            },
        ).run {
            step("Open Tangem Pay card page") { openTangemPayCardPage() }
            step("Open the 'Replace card' reissue bottom sheet") { openReissueSheet() }
            step("Assert 'Unable to cover fee' state is displayed") {
                flakySafely(WAIT_UNTIL_TIMEOUT) {
                    onTangemPayReissueSheet { insufficientFundsTitle.assertIsDisplayed() }
                }
            }
            step("Click on 'Add funds' button") {
                onTangemPayReissueSheet { addFundsButton.clickWithAssertion() }
            }
            step("Assert 'Add funds' sheet shows Swap and Receive options") {
                flakySafely(WAIT_UNTIL_TIMEOUT) {
                    onTangemPayAddFundsSheet {
                        swapOption.assertIsDisplayed()
                        receiveOption.assertIsDisplayed()
                    }
                }
            }
            step("Click on 'Receive' option") {
                onTangemPayAddFundsSheet { receiveOption.clickWithAssertion() }
            }
            step("Assert deposit info for the '$depositNetworkName' network is displayed") {
                flakySafely(WAIT_UNTIL_TIMEOUT) {
                    onTokenReceiveWarningBottomSheet {
                        bottomSheet.assertIsDisplayed()
                        networkName(depositNetworkName).assertIsDisplayed()
                    }
                }
            }
        }
    }
}