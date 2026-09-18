package com.tangem.tests.tangempay

import com.tangem.common.BaseTestCase
import com.tangem.common.constants.TestConstants.WAIT_UNTIL_TIMEOUT_LONG
import com.tangem.common.extensions.clickWithAssertion
import com.tangem.core.res.R as CoreResR
import com.tangem.scenarios.*
import com.tangem.screens.tangempay.*
import dagger.hilt.android.testing.HiltAndroidTest
import io.github.kakaocup.kakao.common.utilities.getResourceString
import io.qameta.allure.kotlin.AllureId
import io.qameta.allure.kotlin.junit4.DisplayName
import org.junit.Ignore
import org.junit.Test

@HiltAndroidTest
class TangemPayTariffPlansTest : BaseTestCase() {

    // WireMock scenario states the flows are driven by.
    private val planSelectNeededState = "PlanSelectNeeded"
    private val planBasicState = "Basic"
    private val planBasicInsufficientBalanceState = "BasicInsufficientBalance"
    private val planBasicThreeCardsState = "BasicThreeCards"
    private val planPlusState = "Plus"
    private val planPlusFiveCardsState = "PlusFiveCards"
    private val planDowngradePendingState = "DowngradePending"
    private val planTransitioningState = "Transitioning"

    private val transitionsOnboardingState = "Onboarding"
    private val transitionsFromBasicState = "FromBasic"
    private val transitionsFromPlusState = "FromPlus"
    private val transitionsNoneState = "NoTransitions"

    private val ordersAwaitingDepositState = "AwaitingDeposit"
    private val orderAwaitingDepositState = "AwaitingDeposit"
    private val offersNoOffersState = "NoOffers"

    // Plan names, fees and description items are served by the mocked backend, not by string resources.
    private val basicPlan = "Basic"
    private val plusPlan = "Plus"
    private val plusProgram = "Visa Signature"
    private val plusFee = "$29.99"
    private val basicDailyLimitPoint = "$10,000 daily spending limit"
    private val basicFeePoint = "$0 / month"
    private val plusFeePoint = "$29.99 / month"
    private val visaProgrammeAttribute = "Visa Programme"
    private val planFeeAttribute = "Plan fee"
    private val maxCardsIssuedAttribute = "Max cards issued"
    private val basicProgramValue = "Platinum"
    private val basicFeeValue = "$0.00"
    private val basicMaxCards = "3"
    private val plusProgramValue = "Signature"
    private val plusMaxCards = "5"

    @AllureId("10382")
    @DisplayName("Tangem Pay: comparing plans during onboarding lists both plans")
    @Ignore("[REDACTED_TASK_KEY]: onboarding comparison drops the ACTIVATION transitions, so only Basic is listed")
    @Test
    fun comparePlansDuringOnboardingTest() {
        setupHooks(
            additionalBeforeSection = {
                setTangemPayTariffPlanScenarios(
                    plan = planSelectNeededState,
                    transitions = transitionsOnboardingState,
                )
            }
        ).run {
            step("Open Main screen with the Tangem Pay tile") { openMainScreenWithTangemPayTile() }
            step("Open plan selection from onboarding") { openTangemPaySelectPlanFromOnboarding() }
            step("Open 'Compare plans' sheet") { openTangemPayComparePlans(basicPlan) }
            step("Assert the '$planFeeAttribute' comparison section is displayed") {
                onTangemPayComparePlansSheet { sectionTitle(planFeeAttribute).assertIsDisplayed() }
            }
            step("Assert '$basicPlan' and '$plusPlan' rows are displayed") {
                onTangemPayComparePlansSheet {
                    planRow(basicPlan).assertIsDisplayed()
                    planRow(plusPlan).assertIsDisplayed()
                }
            }
        }
    }

    @AllureId("10375")
    @DisplayName("Tangem Pay: selecting Basic during onboarding activates the Basic plan")
    @Test
    fun selectBasicPlanDuringOnboardingTest() {
        setupHooks(
            additionalBeforeSection = {
                setTangemPayTariffPlanScenarios(
                    plan = planSelectNeededState,
                    transitions = transitionsOnboardingState,
                )
            }
        ).run {
            step("Open Main screen with the Tangem Pay tile") { openMainScreenWithTangemPayTile() }
            step("Open plan selection from onboarding") { openTangemPaySelectPlanFromOnboarding() }
            step("Assert '$basicPlan' plan details are displayed") {
                onTangemPaySelectPlanScreen {
                    title.assertTextEquals(basicPlan)
                    point(basicDailyLimitPoint).assertIsDisplayed()
                    point(basicFeePoint).assertIsDisplayed()
                }
            }
            val ordersBefore = getTangemPayPlanOrderRequestCount()
            step("Complete the plan order in WireMock") { stageTangemPayPlanOrderCompleted(planBasicState) }
            step("Click on 'Select' button") {
                onTangemPaySelectPlanScreen { selectButton.clickWithAssertion() }
            }
            step("Assert payment account balance is displayed") {
                awaitSuccess(timeoutMillis = WAIT_UNTIL_TIMEOUT_LONG) {
                    onTangemPayMainScreen { balance.assertIsDisplayed() }
                }
            }
            step("Open 'Current plan' screen") { openTangemPayCurrentPlan() }
            assertTangemPayCurrentPlanName(basicPlan)
            assertTangemPayTariffRequestSentOnce("plan order", ordersBefore, ::getTangemPayPlanOrderRequestCount)
        }
    }

    @AllureId("10376")
    @DisplayName("Tangem Pay: the Plus plan fee order picked during onboarding can be cancelled")
    @Test
    fun selectPlusPlanCancelledBeforePaymentTest() {
        setupHooks(
            additionalBeforeSection = {
                setTangemPayTariffPlanScenarios(
                    plan = planSelectNeededState,
                    transitions = transitionsOnboardingState,
                )
            }
        ).run {
            step("Open Main screen with the Tangem Pay tile") { openMainScreenWithTangemPayTile() }
            step("Open plan selection from onboarding") { openTangemPaySelectPlanFromOnboarding() }
            step("Swipe to the '$plusPlan' plan") {
                onTangemPaySelectPlanScreen {
                    scrollToPlanAt(index = 1)
                    awaitSuccess { title.assertTextEquals(plusPlan) }
                    point(plusFeePoint).assertIsDisplayed()
                }
            }
            step("Put the plan fee order in 'awaiting deposit' in WireMock") {
                stageTangemPayPlanFeeAwaitingDeposit()
            }
            step("Click on 'Select' button") {
                onTangemPaySelectPlanScreen { selectButton.clickWithAssertion() }
            }
            step("Assert 'Top-up your account' banner is displayed") {
                awaitSuccess(timeoutMillis = WAIT_UNTIL_TIMEOUT_LONG) {
                    onTangemPayMainScreen { awaitingDepositBannerTitle(plusFee).assertIsDisplayed() }
                }
            }
            val cancelsBefore = getTangemPayCancelPlanOrderRequestCount()
            step("Open 'Current plan' screen") { openTangemPayCurrentPlan() }
            step("Assert the awaiting-payment notification is displayed") {
                awaitSuccess {
                    onTangemPayCurrentPlanScreen {
                        notificationText(
                            getResourceString(
                                CoreResR.string.tangempay_current_plan_awaiting_deposit_notification,
                                plusPlan,
                            ),
                        ).assertIsDisplayed()
                    }
                }
            }
            step("Click on 'Cancel $plusPlan, move to $basicPlan' button") {
                onTangemPayCurrentPlanScreen { notificationButton.clickWithAssertion() }
            }
            step("Drain the plan order list in WireMock") { drainTangemPayPlanOrders() }
            assertTangemPayTariffRequestSentOnce(
                name = "order cancellation",
                countBefore = cancelsBefore,
                count = ::getTangemPayCancelPlanOrderRequestCount,
            )
            step("Assert the plan notification is not displayed") {
                awaitSuccess(timeoutMillis = WAIT_UNTIL_TIMEOUT_LONG) {
                    onTangemPayCurrentPlanScreen { notification.assertDoesNotExist() }
                }
            }
            step("Press back to the payment account") { pressBackFromCurrentPlan() }
            assertTangemPayAwaitingDepositBannerHidden(plusFee)
        }
    }

    @AllureId("10377")
    @DisplayName("Tangem Pay: the Plus plan is activated once the plan fee order completes")
    @Test
    fun selectPlusPlanIssuesCardAfterTopUpTest() {
        setupHooks(
            additionalBeforeSection = {
                setTangemPayTariffPlanScenarios(
                    plan = planSelectNeededState,
                    transitions = transitionsOnboardingState,
                )
            }
        ).run {
            step("Open Main screen with the Tangem Pay tile") { openMainScreenWithTangemPayTile() }
            step("Open plan selection from onboarding") { openTangemPaySelectPlanFromOnboarding() }
            step("Swipe to the '$plusPlan' plan") {
                onTangemPaySelectPlanScreen {
                    scrollToPlanAt(index = 1)
                    awaitSuccess { title.assertTextEquals(plusPlan) }
                }
            }
            step("Put the plan fee order in 'awaiting deposit' in WireMock") {
                stageTangemPayPlanFeeAwaitingDeposit()
            }
            step("Click on 'Select' button") {
                onTangemPaySelectPlanScreen { selectButton.clickWithAssertion() }
            }
            step("Assert 'Top-up your account' banner is displayed") {
                awaitSuccess(timeoutMillis = WAIT_UNTIL_TIMEOUT_LONG) {
                    onTangemPayMainScreen { awaitingDepositBannerTitle(plusFee).assertIsDisplayed() }
                }
            }
            step("Complete the plan order in WireMock") { stageTangemPayPlanOrderCompleted(planPlusState) }
            assertTangemPayAwaitingDepositBannerHidden(plusFee)
            step("Open 'Current plan' screen") { openTangemPayCurrentPlan() }
            assertTangemPayCurrentPlanName(plusPlan)
        }
    }

    @AllureId("10383")
    @DisplayName("Tangem Pay: comparing plans while changing a plan lists both plans")
    @Test
    fun comparePlansWhenChangingPlanTest() {
        setupHooks(
            additionalBeforeSection = {
                setTangemPayTariffPlanScenarios(
                    plan = planBasicState,
                    transitions = transitionsFromBasicState,
                )
            }
        ).run {
            step("Open Tangem Pay payment account") { openTangemPay() }
            step("Open 'Current plan' screen") { openTangemPayCurrentPlan() }
            step("Open plan selection") { openTangemPayChangePlan() }
            step("Open 'Compare plans' sheet") { openTangemPayComparePlans(basicPlan) }
            step("Assert the '$maxCardsIssuedAttribute' comparison section is displayed") {
                onTangemPayComparePlansSheet {
                    scrollToSection(maxCardsIssuedAttribute)
                    sectionTitle(maxCardsIssuedAttribute).assertIsDisplayed()
                }
            }
            step("Assert '$basicPlan' and '$plusPlan' rows are displayed") {
                onTangemPayComparePlansSheet {
                    planRow(basicPlan).assertIsDisplayed()
                    planRow(plusPlan).assertIsDisplayed()
                }
            }
        }
    }

    @AllureId("10378")
    @DisplayName("Tangem Pay: upgrading to Plus without funds asks to top up the account")
    @Test
    fun upgradeToPlusWithInsufficientBalanceTest() {
        setupHooks(
            additionalBeforeSection = {
                setTangemPayTariffPlanScenarios(
                    plan = planBasicInsufficientBalanceState,
                    transitions = transitionsFromBasicState,
                )
            }
        ).run {
            step("Open Tangem Pay payment account") { openTangemPay() }
            step("Open 'Current plan' screen") { openTangemPayCurrentPlan() }
            assertTangemPayCurrentPlanName(basicPlan)
            step("Assert the plan sections and rows are displayed") {
                onTangemPayCurrentPlanScreen {
                    sectionHeader(getResourceString(CoreResR.string.tangempay_current_plan_section_card))
                        .assertIsDisplayed()
                    sectionHeader(getResourceString(CoreResR.string.tangempay_current_plan_section_plan))
                        .assertIsDisplayed()
                    planRow(label = visaProgrammeAttribute, value = basicProgramValue).assertIsDisplayed()
                    planRow(label = planFeeAttribute, value = basicFeeValue).assertIsDisplayed()
                    planRow(label = maxCardsIssuedAttribute, value = basicMaxCards).assertIsDisplayed()
                }
            }
            step("Open plan selection") { openTangemPayChangePlan() }
            step("Assert '$plusPlan' plan is displayed") {
                onTangemPaySelectPlanScreen { title.assertTextEquals(plusPlan) }
            }
            step("Click on 'Select' button") { selectTangemPayPlanForConfirmation() }
            step("Assert the upgrade confirmation is displayed") {
                onTangemPaySelectPlanScreen {
                    title.assertTextEquals(
                        getResourceString(
                            CoreResR.string.tangempay_select_plan_confirm_upgrade_title,
                            plusProgram,
                        ),
                    )
                    point(
                        getResourceString(
                            CoreResR.string.tangempay_select_plan_confirm_point_monthly_fee,
                            plusFee,
                        ),
                    ).assertIsDisplayed()
                }
            }
            step("Put the plan fee order in 'awaiting deposit' in WireMock") {
                stageTangemPayPlanFeeAwaitingDeposit(plan = planBasicInsufficientBalanceState)
            }
            step("Click on 'Upgrade plan' button") {
                onTangemPaySelectPlanScreen { confirmButton.clickWithAssertion() }
            }
            step("Assert 'Top-up your account' banner is displayed") {
                awaitSuccess(timeoutMillis = WAIT_UNTIL_TIMEOUT_LONG) {
                    onTangemPayMainScreen { awaitingDepositBannerTitle(plusFee).assertIsDisplayed() }
                }
            }
        }
    }

    @AllureId("10380")
    @DisplayName("Tangem Pay: upgrading to Plus with funds activates the Plus plan")
    @Test
    fun upgradeToPlusWithSufficientBalanceTest() {
        setupHooks(
            additionalBeforeSection = {
                setTangemPayTariffPlanScenarios(
                    plan = planBasicState,
                    transitions = transitionsFromBasicState,
                )
            }
        ).run {
            step("Open Tangem Pay payment account") { openTangemPay() }
            step("Open 'Current plan' screen") { openTangemPayCurrentPlan() }
            step("Open plan selection") { openTangemPayChangePlan() }
            step("Assert '$plusPlan' plan is displayed") {
                onTangemPaySelectPlanScreen { title.assertTextEquals(plusPlan) }
            }
            step("Click on 'Select' button") { selectTangemPayPlanForConfirmation() }
            val ordersBefore = getTangemPayPlanOrderRequestCount()
            step("Complete the plan order in WireMock") { stageTangemPayPlanOrderCompleted(planPlusState) }
            step("Click on 'Upgrade plan' button") {
                onTangemPaySelectPlanScreen { confirmButton.clickWithAssertion() }
            }
            step("Assert payment account balance is displayed") {
                awaitSuccess(timeoutMillis = WAIT_UNTIL_TIMEOUT_LONG) {
                    onTangemPayMainScreen { balance.assertIsDisplayed() }
                }
            }
            step("Open 'Current plan' screen") { openTangemPayCurrentPlan() }
            assertTangemPayCurrentPlanName(plusPlan)
            assertTangemPayTariffRequestSentOnce("plan order", ordersBefore, ::getTangemPayPlanOrderRequestCount)
        }
    }

    @AllureId("10411")
    @DisplayName("Tangem Pay: cancelling the unpaid upgrade keeps the customer on Basic")
    @Test
    fun cancelUpgradeWithInsufficientBalanceTest() {
        setupHooks(
            additionalBeforeSection = {
                setTangemPayTariffPlanScenarios(
                    plan = planBasicInsufficientBalanceState,
                    transitions = transitionsFromBasicState,
                    orders = ordersAwaitingDepositState,
                    order = orderAwaitingDepositState,
                )
            }
        ).run {
            step("Open Tangem Pay payment account") { openTangemPay() }
            step("Assert 'Top-up your account' banner is displayed") {
                awaitSuccess(timeoutMillis = WAIT_UNTIL_TIMEOUT_LONG) {
                    onTangemPayMainScreen { awaitingDepositBannerTitle(plusFee).assertIsDisplayed() }
                }
            }
            val cancelsBefore = getTangemPayCancelPlanOrderRequestCount()
            step("Open 'Current plan' screen") { openTangemPayCurrentPlan() }
            step("Click on 'Cancel $plusPlan, move to $basicPlan' button") {
                awaitSuccess { onTangemPayCurrentPlanScreen { notificationButton.assertIsDisplayed() } }
                onTangemPayCurrentPlanScreen { notificationButton.performClick() }
            }
            step("Drain the plan order list in WireMock") { drainTangemPayPlanOrders() }
            assertTangemPayTariffRequestSentOnce(
                name = "order cancellation",
                countBefore = cancelsBefore,
                count = ::getTangemPayCancelPlanOrderRequestCount,
            )
            step("Assert the plan notification is not displayed") {
                awaitSuccess(timeoutMillis = WAIT_UNTIL_TIMEOUT_LONG) {
                    onTangemPayCurrentPlanScreen { notification.assertDoesNotExist() }
                }
            }
            step("Press back to the payment account") { pressBackFromCurrentPlan() }
            assertTangemPayAwaitingDepositBannerHidden(plusFee)
            step("Open 'Current plan' screen") { openTangemPayCurrentPlan() }
            assertTangemPayCurrentPlanName(basicPlan)
        }
    }

    @AllureId("10379")
    @DisplayName("Tangem Pay: downgrading to Basic schedules a pending transition")
    @Test
    fun downgradeToBasicSchedulesPendingTransitionTest() {
        setupHooks(
            additionalBeforeSection = {
                setTangemPayTariffPlanScenarios(
                    plan = planPlusState,
                    transitions = transitionsFromPlusState,
                )
            }
        ).run {
            step("Open Tangem Pay payment account") { openTangemPay() }
            step("Open 'Current plan' screen") { openTangemPayCurrentPlan() }
            assertTangemPayCurrentPlanName(plusPlan)
            step("Assert the plan rows are displayed") {
                onTangemPayCurrentPlanScreen {
                    planRow(label = visaProgrammeAttribute, value = plusProgramValue).assertIsDisplayed()
                    planRow(label = planFeeAttribute, value = plusFee).assertIsDisplayed()
                    planRow(label = maxCardsIssuedAttribute, value = plusMaxCards).assertIsDisplayed()
                }
            }
            step("Open plan selection") { openTangemPayChangePlan() }
            step("Assert '$basicPlan' plan is displayed") {
                onTangemPaySelectPlanScreen { title.assertTextEquals(basicPlan) }
            }
            step("Click on 'Select' button") { selectTangemPayPlanForConfirmation() }
            step("Assert the downgrade confirmation is displayed") {
                onTangemPaySelectPlanScreen {
                    point(getResourceString(CoreResR.string.tangempay_select_plan_confirm_point_no_fee))
                        .assertIsDisplayed()
                }
            }
            val requestsBefore = getTangemPayPendingTransitionRequestCount()
            step("Schedule the pending downgrade in WireMock") {
                stageTangemPayPlanState(planDowngradePendingState)
            }
            step("Click on 'Continue' button") {
                onTangemPaySelectPlanScreen { confirmButton.clickWithAssertion() }
            }
            step("Assert the 'Stay on $plusPlan' offer is displayed") {
                awaitSuccess(timeoutMillis = WAIT_UNTIL_TIMEOUT_LONG) {
                    onTangemPayCurrentPlanScreen {
                        notificationButtonWithText(
                            getResourceString(CoreResR.string.tangempay_current_plan_stay_button, plusPlan),
                        ).assertIsDisplayed()
                    }
                }
            }
            assertTangemPayTariffRequestSentOnce(
                name = "pending transition",
                countBefore = requestsBefore,
                count = ::getTangemPayPendingTransitionRequestCount,
            )
        }
    }

    @AllureId("10381")
    @DisplayName("Tangem Pay: staying on Plus cancels the pending downgrade")
    @Test
    fun stayOnPlusCancelsPendingDowngradeTest() {
        setupHooks(
            additionalBeforeSection = {
                setTangemPayTariffPlanScenarios(
                    plan = planDowngradePendingState,
                    transitions = transitionsFromPlusState,
                )
            }
        ).run {
            step("Open Tangem Pay payment account") { openTangemPay() }
            step("Open 'Current plan' screen") { openTangemPayCurrentPlan() }
            val stayOnPlanButtonText =
                getResourceString(CoreResR.string.tangempay_current_plan_stay_button, plusPlan)
            step("Assert the pending downgrade notification is displayed") {
                awaitSuccess {
                    onTangemPayCurrentPlanScreen {
                        notificationButtonWithText(stayOnPlanButtonText).assertIsDisplayed()
                    }
                }
            }
            val requestsBefore = getTangemPayCancelPendingTransitionRequestCount()
            step("Click on '$stayOnPlanButtonText' button") {
                onTangemPayCurrentPlanScreen { notificationButton.clickWithAssertion() }
            }
            step("Assert the 'Stay on $plusPlan' sheet is displayed") {
                awaitSuccess {
                    onTangemPayTariffMessageSheet { stayOnPlanTitle(plusPlan).assertIsDisplayed() }
                }
                onTangemPayTariffMessageSheet { stayOnPlanDescription(basicPlan).assertIsDisplayed() }
            }
            step("Keep the customer on $plusPlan in WireMock") { stageTangemPayPlanState(planPlusState) }
            step("Click on '$stayOnPlanButtonText' button on the sheet") {
                onTangemPayTariffMessageSheet { primaryButton.clickWithAssertion() }
            }
            assertTangemPayTariffRequestSentOnce(
                name = "pending transition cancellation",
                countBefore = requestsBefore,
                count = ::getTangemPayCancelPendingTransitionRequestCount,
            )
            assertTangemPayCurrentPlanName(plusPlan)
            step("Assert the 'Stay on $plusPlan' offer is not displayed") {
                awaitSuccess(timeoutMillis = WAIT_UNTIL_TIMEOUT_LONG) {
                    onTangemPayCurrentPlanScreen {
                        notificationButtonWithText(stayOnPlanButtonText).assertDoesNotExist()
                    }
                }
            }
        }
    }

    @AllureId("10409")
    @DisplayName("Tangem Pay: plan details stay reachable while a transition is in progress")
    @Test
    fun planDetailsDuringActiveTransitionTest() {
        setupHooks(
            additionalBeforeSection = {
                setTangemPayTariffPlanScenarios(
                    plan = planTransitioningState,
                    transitions = transitionsFromBasicState,
                )
            }
        ).run {
            step("Open Tangem Pay payment account") { openTangemPay() }
            step("Open 'Current plan' screen") { openTangemPayCurrentPlan() }
            assertTangemPayCurrentPlanName(basicPlan)
            step("Assert 'Change plan' button is not displayed") {
                onTangemPayCurrentPlanScreen { changePlanButton.assertDoesNotExist() }
            }
        }
    }

    @AllureId("10385")
    @DisplayName("Tangem Pay: the third Basic card raises the plan cards-limit popup")
    @Test
    fun cardsLimitOnBasicPlanTest() {
        setupHooks(
            additionalBeforeSection = {
                setTangemPayTariffPlanScenarios(
                    plan = planBasicThreeCardsState,
                    transitions = transitionsFromBasicState,
                    offers = offersNoOffersState,
                )
            }
        ).run {
            step("Open Tangem Pay payment account") { openTangemPay() }
            step("Click on 'Add card' button") { openTangemPayCardsLimitSheet() }
            step("Assert 'Cards limit reached for this plan' sheet is displayed") {
                awaitSuccess { onTangemPayTariffMessageSheet { cardsLimitForPlanTitle.assertIsDisplayed() } }
                onTangemPayTariffMessageSheet { cardsLimitForPlanDescription.assertIsDisplayed() }
            }
        }
    }

    @AllureId("10386")
    @DisplayName("Tangem Pay: the fifth Plus card raises the maximum cards popup")
    @Test
    fun cardsLimitOnPlusPlanTest() {
        setupHooks(
            additionalBeforeSection = {
                setTangemPayTariffPlanScenarios(
                    plan = planPlusFiveCardsState,
                    transitions = transitionsNoneState,
                    offers = offersNoOffersState,
                )
            }
        ).run {
            step("Open Tangem Pay payment account") { openTangemPay() }
            step("Click on 'Add card' button") { openTangemPayCardsLimitSheet() }
            step("Assert 'Maximum Cards Issued' sheet is displayed") {
                awaitSuccess { onTangemPayTariffMessageSheet { maximumCardsTitle.assertIsDisplayed() } }
                onTangemPayTariffMessageSheet { maximumCardsDescription.assertIsDisplayed() }
            }
        }
    }

    @AllureId("10387")
    @DisplayName("Tangem Pay: the plan cards-limit popup leads to the Plus upgrade")
    @Test
    fun upgradeAfterThreeBasicCardsTest() {
        setupHooks(
            additionalBeforeSection = {
                setTangemPayTariffPlanScenarios(
                    plan = planBasicThreeCardsState,
                    transitions = transitionsFromBasicState,
                    offers = offersNoOffersState,
                )
            }
        ).run {
            step("Open Tangem Pay payment account") { openTangemPay() }
            step("Click on 'Add card' button") { openTangemPayCardsLimitSheet() }
            step("Assert 'Cards limit reached for this plan' sheet is displayed") {
                awaitSuccess { onTangemPayTariffMessageSheet { cardsLimitForPlanTitle.assertIsDisplayed() } }
            }
            step("Click on 'Upgrade plan' button") {
                onTangemPayTariffMessageSheet { primaryButton.clickWithAssertion() }
            }
            step("Assert 'Current plan' screen is displayed") {
                awaitSuccess { onTangemPayCurrentPlanScreen { screen.assertIsDisplayed() } }
            }
            assertTangemPayCurrentPlanName(basicPlan)
            step("Open plan selection") { openTangemPayChangePlan() }
            step("Assert '$plusPlan' plan is displayed") {
                onTangemPaySelectPlanScreen { title.assertTextEquals(plusPlan) }
            }
        }
    }
}