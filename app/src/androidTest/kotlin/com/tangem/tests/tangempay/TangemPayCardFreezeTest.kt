package com.tangem.tests.tangempay

import com.tangem.common.BaseTestCase
import com.tangem.common.constants.TestConstants.TANGEM_PAY_ELIGIBILITY_SCENARIO
import com.tangem.common.extensions.assertSnackbarWithText
import com.tangem.common.utils.resetWireMockScenarioState
import com.tangem.common.utils.resetWireMockScenarios
import com.tangem.common.utils.setWireMockScenarioState
import com.tangem.core.res.R as CoreResR
import com.tangem.scenarios.freezeCardFromCardPage
import com.tangem.scenarios.openTangemPay
import com.tangem.scenarios.openTangemPayCardPage
import com.tangem.scenarios.unfreezeCardFromCardPage
import com.tangem.screens.tangempay.onTangemPayCardPageScreen
import com.tangem.screens.tangempay.onTangemPayMainScreen
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
                resetWireMockScenarios()
                setWireMockScenarioState(TANGEM_PAY_ELIGIBILITY_SCENARIO, eligibilityState)
                setWireMockScenarioState(freezeScenario, freezeErrorState)
            },
            additionalAfterSection = {
                resetWireMockScenarioState(TANGEM_PAY_ELIGIBILITY_SCENARIO)
                resetWireMockScenarioState(freezeScenario)
            },
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
            step("Assert frozen badge is displayed") {
                awaitSuccess { onTangemPayCardPageScreen { cardFrozenBadge.assertIsDisplayed() } }
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
                resetWireMockScenarios()
                setWireMockScenarioState(TANGEM_PAY_ELIGIBILITY_SCENARIO, eligibilityState)
                setWireMockScenarioState(freezeScenario, unfreezeErrorState)
            },
            additionalAfterSection = {
                resetWireMockScenarioState(TANGEM_PAY_ELIGIBILITY_SCENARIO)
                resetWireMockScenarioState(freezeScenario)
            },
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
                resetWireMockScenarios()
                setWireMockScenarioState(TANGEM_PAY_ELIGIBILITY_SCENARIO, eligibilityState)
                setWireMockScenarioState(freezeScenario, frozenState)
            },
            additionalAfterSection = {
                resetWireMockScenarioState(TANGEM_PAY_ELIGIBILITY_SCENARIO)
                resetWireMockScenarioState(freezeScenario)
            },
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
}