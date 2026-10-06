package com.tangem.tests.tangempay

import com.tangem.common.BaseTestCase
import com.tangem.common.constants.TestConstants.TANGEM_PAY_ELIGIBILITY_SCENARIO
import com.tangem.common.constants.TestConstants.WAIT_UNTIL_TIMEOUT
import com.tangem.common.extensions.assertSnackbarWithText
import com.tangem.common.extensions.assertTextContainsSafe
import com.tangem.common.utils.setWireMockScenarioState
import com.tangem.core.res.R as CoreResR
import com.tangem.scenarios.assertTangemPayTransactionRowDisplayed
import com.tangem.scenarios.freezeCardFromCardPage
import com.tangem.scenarios.openTangemPay
import com.tangem.scenarios.openTangemPayCardPage
import com.tangem.scenarios.openTangemPayCardPageFromPaymentAccount
import com.tangem.scenarios.openTangemPayTransactionDetails
import com.tangem.scenarios.reopenTangemPayFromMain
import com.tangem.scenarios.unfreezeCardFromCardPage
import com.tangem.screens.tangempay.onTangemPayCardPageScreen
import com.tangem.screens.tangempay.onTangemPayMainScreen
import com.tangem.screens.tangempay.onTangemPayTransactionDetailsSheet
import dagger.hilt.android.testing.HiltAndroidTest
import io.github.kakaocup.kakao.common.utilities.getResourceString
import io.qameta.allure.kotlin.AllureId
import io.qameta.allure.kotlin.junit4.DisplayName
import org.junit.Test

@HiltAndroidTest
class TangemPayCardFreezeTest : BaseTestCase() {

    private val eligibilityState = "PaeraCustomer"
    private val freezeScenario = "tangem_pay_card_freeze"

    @AllureId("9555")
    @DisplayName("Tangem Pay: freeze error shows a toast and the second attempt freezes the card")
    @Test
    fun freezeCardShowsErrorToastThenRetrySucceedsTest() {
        val freezeErrorState = "FreezeError"
        val freezeErrorText = getResourceString(CoreResR.string.tangem_pay_freeze_card_failed)

        setupHooks(
            additionalBeforeSection = {
                setWireMockScenarioState(TANGEM_PAY_ELIGIBILITY_SCENARIO, eligibilityState)
                setWireMockScenarioState(freezeScenario, freezeErrorState)
            }
        ).run {
            step("Open Tangem Pay card page") { openTangemPayCardPage() }
            step("Assert card is active (Freeze row is displayed)") {
                onTangemPayCardPageScreen { freezeCardRowActive.assertIsDisplayed() }
            }
            step("Freeze card via confirmation sheet") { freezeCardFromCardPage() }
            step("Assert freeze failure toast is displayed") { assertSnackbarWithText(freezeErrorText) }
            step("Assert card is still active (Freeze row is displayed)") {
                awaitSuccess { onTangemPayCardPageScreen { freezeCardRowActive.assertIsDisplayed() } }
            }
            step("Retry freeze card via confirmation sheet") { freezeCardFromCardPage() }
            step("Assert card is frozen (Unfreeze row is displayed)") {
                awaitSuccess { onTangemPayCardPageScreen { unfreezeCardRow.assertIsDisplayed() } }
            }
        }
    }

    @AllureId("9556")
    @DisplayName("Tangem Pay: unfreeze error shows a toast and the second attempt unfreezes the card")
    @Test
    fun unfreezeCardShowsErrorToastThenRetrySucceedsTest() {
        val unfreezeErrorState = "UnfreezeError"
        val unfreezeErrorText = getResourceString(CoreResR.string.tangem_pay_unfreeze_card_failed)

        setupHooks(
            additionalBeforeSection = {
                setWireMockScenarioState(TANGEM_PAY_ELIGIBILITY_SCENARIO, eligibilityState)
                setWireMockScenarioState(freezeScenario, unfreezeErrorState)
            }
        ).run {
            step("Open Tangem Pay card page") { openTangemPayCardPage() }
            step("Assert card is frozen (Unfreeze row is displayed)") {
                onTangemPayCardPageScreen { unfreezeCardRow.assertIsDisplayed() }
            }
            step("Unfreeze card via confirmation sheet") { unfreezeCardFromCardPage() }
            step("Assert unfreeze failure toast is displayed") { assertSnackbarWithText(unfreezeErrorText) }
            step("Assert card is still frozen (Unfreeze row is displayed)") {
                awaitSuccess { onTangemPayCardPageScreen { unfreezeCardRow.assertIsDisplayed() } }
            }
            step("Retry unfreeze card via confirmation sheet") { unfreezeCardFromCardPage() }
            step("Assert card is active (Freeze row is displayed)") {
                awaitSuccess { onTangemPayCardPageScreen { freezeCardRowActive.assertIsDisplayed() } }
            }
        }
    }

    @AllureId("9608")
    @DisplayName("Tangem Pay: Withdraw and Add funds are disabled when the card is frozen")
    @Test
    fun withdrawAndAddFundsDisabledWhenCardFrozenTest() {
        val frozenState = "Frozen"

        setupHooks(
            additionalBeforeSection = {
                setWireMockScenarioState(TANGEM_PAY_ELIGIBILITY_SCENARIO, eligibilityState)
                setWireMockScenarioState(freezeScenario, frozenState)
            }
        ).run {
            step("Open Tangem Pay") { openTangemPay() }
            // ACTION_BUTTON exposes no Disabled semantics; a disabled one carries no click action.
            step("Assert 'Withdraw' button is disabled") {
                onTangemPayMainScreen { withdrawButton.assertHasNoClickAction() }
            }
            // Add funds is gated only by the frozen state, isolating it from Withdraw's balance check.
            step("Assert 'Add funds' button is disabled") {
                onTangemPayMainScreen { topUpButton.assertHasNoClickAction() }
            }
        }
    }

    @AllureId("11478")
    @DisplayName("Tangem Pay: a frozen card shows the snowflake indicator on Payment account")
    @Test
    fun frozenCardShowsSnowflakeIndicatorOnPaymentAccountTest() {
        val cardActiveState = "Started"

        setupHooks(
            additionalBeforeSection = {
                setWireMockScenarioState(TANGEM_PAY_ELIGIBILITY_SCENARIO, eligibilityState)
                setWireMockScenarioState(freezeScenario, cardActiveState)
            }
        ).run {
            step("Open Tangem Pay card page") { openTangemPayCardPage() }
            step("Assert card is active (Freeze row is displayed)") {
                onTangemPayCardPageScreen { freezeCardRowActive.assertIsDisplayed() }
            }
            step("Freeze card via confirmation sheet") { freezeCardFromCardPage() }
            step("Assert card is frozen (Unfreeze row is displayed)") {
                awaitSuccess { onTangemPayCardPageScreen { unfreezeCardRow.assertIsDisplayed() } }
            }
            step("Click on 'Back' button") { device.uiDevice.pressBack() }
            step("Assert frozen indicator is displayed on the card") {
                flakySafely(WAIT_UNTIL_TIMEOUT) {
                    onTangemPayMainScreen { cardFrozenIcon.assertIsDisplayed() }
                }
            }
        }
    }

    @AllureId("9533")
    @DisplayName("Tangem Pay: a payment on a frozen card shows up as a declined \$0.00 transaction")
    @Test
    fun paymentOnFrozenCardShownAsDeclinedZeroAmountTransactionTest() {
        val cardActiveState = "Started"
        val historyScenario = "tangem_pay_transaction_history"
        val emptyHistoryState = "InitialEmpty"
        val declinedOnFrozenCardState = "SpendDeclinedFrozenCard"
        val declinedMerchant = "Tangem Frozen Card Shop"
        val declinedAmount = "\$0.00"

        setupHooks(
            additionalBeforeSection = {
                setWireMockScenarioState(TANGEM_PAY_ELIGIBILITY_SCENARIO, eligibilityState)
                setWireMockScenarioState(freezeScenario, cardActiveState)
                setWireMockScenarioState(historyScenario, emptyHistoryState)
            }
        ).run {
            step("Open Tangem Pay") { openTangemPay() }
            step("Open Tangem Pay card page") { openTangemPayCardPageFromPaymentAccount() }
            step("Freeze card via confirmation sheet") { freezeCardFromCardPage() }
            step("Assert card is frozen (Unfreeze row is displayed)") {
                awaitSuccess { onTangemPayCardPageScreen { unfreezeCardRow.assertIsDisplayed() } }
            }
            step("Set WireMock scenario '$historyScenario' to '$declinedOnFrozenCardState'") {
                setWireMockScenarioState(historyScenario, declinedOnFrozenCardState)
            }
            step("Click on 'Back' button") { device.uiDevice.pressBack() }
            step("Assert payment account balance is displayed") {
                flakySafely(WAIT_UNTIL_TIMEOUT) {
                    onTangemPayMainScreen { balance.assertIsDisplayed() }
                }
            }
            step("Reopen Tangem Pay from Main") { reopenTangemPayFromMain() }
            step("Assert '$declinedMerchant' transaction row is displayed") {
                assertTangemPayTransactionRowDisplayed(declinedMerchant)
            }
            step("Assert '$declinedMerchant' transaction row shows '$declinedAmount'") {
                onTangemPayMainScreen { transactionRowWithAmount(declinedMerchant, declinedAmount).assertIsDisplayed() }
            }
            step("Open '$declinedMerchant' transaction details") {
                openTangemPayTransactionDetails(declinedMerchant)
            }
            step("Assert transaction details show '$declinedAmount' and the 'Declined' status") {
                onTangemPayTransactionDetailsSheet {
                    flakySafely(WAIT_UNTIL_TIMEOUT) {
                        amount.assertTextContainsSafe(declinedAmount, substring = true)
                        declinedStatus.assertIsDisplayed()
                    }
                }
            }
        }
    }
}