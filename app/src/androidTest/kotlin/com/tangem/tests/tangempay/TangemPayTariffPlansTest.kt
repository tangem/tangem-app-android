package com.tangem.tests.tangempay

import com.tangem.common.BaseTestCase
import com.tangem.common.constants.TestConstants.WAIT_UNTIL_TIMEOUT_LONG
import com.tangem.common.extensions.clickWithAssertion
import com.tangem.common.utils.assertUrlOpenedStartingWith
import com.tangem.common.utils.stubExternalUrlOpening
import com.tangem.core.res.R as CoreResR
import com.tangem.features.tangempay.TangemPayConstants
import com.tangem.scenarios.*
import com.tangem.screens.tangempay.*
import dagger.hilt.android.testing.HiltAndroidTest
import io.github.kakaocup.kakao.common.utilities.getResourceString
import io.qameta.allure.kotlin.AllureId
import io.qameta.allure.kotlin.junit4.DisplayName
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

    // Plans, fees, transactions and card digits come from the mocked backend, not from string resources.
    private val basicPlan = "Basic"
    private val plusPlan = "Plus"
    private val plusProgram = "Visa Signature"
    private val plusFee = "$29.99"
    private val planFeeTransaction = "Plus monthly plan fee"
    private val cardLastDigits = "4242"
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

    // Localized text asserted in the bodies below.
    private val cardRelatedSection = getResourceString(CoreResR.string.tangempay_current_plan_section_card)
    private val planRelatedSection = getResourceString(CoreResR.string.tangempay_current_plan_section_plan)
    private val noFeeAppliedPoint = getResourceString(CoreResR.string.tangempay_select_plan_confirm_point_no_fee)
    private val stayOnPlusButton =
        getResourceString(CoreResR.string.tangempay_current_plan_stay_button, plusPlan)
    private val awaitingPlanFeeNotification =
        getResourceString(CoreResR.string.tangempay_current_plan_awaiting_deposit_notification, plusPlan)
    private val upgradeConfirmTitle =
        getResourceString(CoreResR.string.tangempay_select_plan_confirm_upgrade_title, plusProgram)
    private val monthlyFeePoint =
        getResourceString(CoreResR.string.tangempay_select_plan_confirm_point_monthly_fee, plusFee)

    @AllureId("10382")
    @DisplayName("Tangem Pay: comparing plans during onboarding lists both plans")
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
            step("Assert '$basicPlan' row is displayed") {
                onTangemPayComparePlansSheet { planRow(basicPlan).assertIsDisplayed() }
            }
            step("Assert '$plusPlan' row is displayed") {
                onTangemPayComparePlansSheet { planRow(plusPlan).assertIsDisplayed() }
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
            step("Assert '$basicPlan' plan name is displayed") {
                onTangemPaySelectPlanScreen { title.assertTextEquals(basicPlan) }
            }
            step("Assert '$basicDailyLimitPoint' point is displayed") {
                onTangemPaySelectPlanScreen { point(basicDailyLimitPoint).assertIsDisplayed() }
            }
            step("Assert '$basicFeePoint' point is displayed") {
                onTangemPaySelectPlanScreen { point(basicFeePoint).assertIsDisplayed() }
            }
            val ordersBefore = getTangemPayPlanOrderRequestCount()
            step("Hold the '$basicPlan' card issuing in WireMock") { stageTangemPayBasicOrderPolling() }
            step("Click on 'Select' button") {
                onTangemPaySelectPlanScreen { selectButton.clickWithAssertion() }
            }
            step("Assert the card is being issued") { assertTangemPayIssuingCardBanner() }
            step("Assert 'Withdraw' button is disabled") {
                onTangemPayMainScreen { withdrawButtonControl.assertIsNotEnabled() }
            }
            step("Assert the plan order request was sent once") {
                assertTangemPayTariffRequestSentOnce("plan order", ordersBefore, ::getTangemPayPlanOrderRequestCount)
            }
            step("Activate the '$basicPlan' plan in WireMock") { stageTangemPayPlanState(planBasicState) }
            step("Complete the card issuing in WireMock") { completeTangemPayBasicOrderIssuing() }
            step("Assert the card issuing is over") { assertTangemPayIssuingCardBannerHidden() }
            step("Assert payment account balance is displayed") {
                awaitSuccess(timeoutMillis = WAIT_UNTIL_TIMEOUT_LONG) {
                    onTangemPayMainScreen { balance.assertIsDisplayed() }
                }
            }
            step("Open 'Current plan' screen") { openTangemPayCurrentPlan(assertMenuItems = true) }
            step("Assert current plan is '$basicPlan'") { assertTangemPayCurrentPlanName(basicPlan) }
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
                onTangemPaySelectPlanScreen { scrollToPlanAt(index = 1) }
            }
            step("Assert '$plusPlan' plan name is displayed") {
                awaitSuccess { onTangemPaySelectPlanScreen { title.assertTextEquals(plusPlan) } }
            }
            step("Assert '$plusFeePoint' point is displayed") {
                onTangemPaySelectPlanScreen { point(plusFeePoint).assertIsDisplayed() }
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
                        notificationText(awaitingPlanFeeNotification).assertIsDisplayed()
                    }
                }
            }
            step("Click on 'Cancel $plusPlan, move to $basicPlan' button") {
                onTangemPayCurrentPlanScreen { notificationButton.clickWithAssertion() }
            }
            // The cancel stub only flips the polled order; the list the notification reads is drained here.
            step("Drain the plan order list in WireMock") { drainTangemPayPlanOrders() }
            step("Assert the order cancellation request was sent once") {
                assertTangemPayTariffRequestSentOnce(
                    name = "order cancellation",
                    countBefore = cancelsBefore,
                    count = ::getTangemPayCancelPlanOrderRequestCount,
                )
            }
            step("Assert the plan notification is not displayed") {
                awaitSuccess(timeoutMillis = WAIT_UNTIL_TIMEOUT_LONG) {
                    onTangemPayCurrentPlanScreen { notification.assertDoesNotExist() }
                }
            }
            step("Press back to the payment account") { pressBackFromCurrentPlan() }
            step("Assert 'Top-up your account' banner is not displayed") {
                assertTangemPayAwaitingDepositBannerHidden(plusFee)
            }
            step("Open 'Current plan' screen") { openTangemPayCurrentPlan(assertMenuItems = true) }
            step("Assert current plan is '$basicPlan'") { assertTangemPayCurrentPlanName(basicPlan) }
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
                onTangemPaySelectPlanScreen { scrollToPlanAt(index = 1) }
            }
            step("Assert '$plusPlan' plan name is displayed") {
                awaitSuccess { onTangemPaySelectPlanScreen { title.assertTextEquals(plusPlan) } }
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
            step("Open 'Add funds' from the top-up banner") { checkTangemPayAddFundsFromTopUpBanner() }
            step("Move the plan fee order to 'processing' in WireMock") { stageTangemPayPlusOrderProcessing() }
            step("Assert 'Top-up your account' banner is not displayed") {
                assertTangemPayAwaitingDepositBannerHidden(plusFee)
            }
            step("Complete the plan order in WireMock") { completeTangemPayPlusOrderWithPlanFee(planPlusState) }
            step("Pull to refresh Tangem Pay") { pullToRefreshTangemPay() }
            step("Assert the plan fee transaction is displayed") {
                assertTangemPayTransactionRowDisplayed(planFeeTransaction)
            }
            step("Open 'Current plan' screen") { openTangemPayCurrentPlan(assertMenuItems = true) }
            step("Assert current plan is '$plusPlan'") { assertTangemPayCurrentPlanName(plusPlan) }
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
            step("Open 'Current plan' screen") { openTangemPayCurrentPlan(assertMenuItems = true) }
            step("Open plan selection") { openTangemPayChangePlan() }
            step("Open 'Compare plans' sheet") { openTangemPayComparePlans(basicPlan) }
            step("Scroll to the '$maxCardsIssuedAttribute' comparison section") {
                onTangemPayComparePlansSheet { scrollToSection(maxCardsIssuedAttribute) }
            }
            step("Assert the '$maxCardsIssuedAttribute' comparison section is displayed") {
                onTangemPayComparePlansSheet { sectionTitle(maxCardsIssuedAttribute).assertIsDisplayed() }
            }
            step("Assert '$basicPlan' row is displayed") {
                onTangemPayComparePlansSheet { planRow(basicPlan).assertIsDisplayed() }
            }
            step("Assert '$plusPlan' row is displayed") {
                onTangemPayComparePlansSheet { planRow(plusPlan).assertIsDisplayed() }
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
            step("Assert current plan is '$basicPlan'") { assertTangemPayCurrentPlanName(basicPlan) }
            step("Assert '$cardRelatedSection' section is displayed") {
                onTangemPayCurrentPlanScreen { sectionHeader(cardRelatedSection).assertIsDisplayed() }
            }
            step("Assert '$planRelatedSection' section is displayed") {
                onTangemPayCurrentPlanScreen { sectionHeader(planRelatedSection).assertIsDisplayed() }
            }
            step("Assert '$visaProgrammeAttribute' row is displayed") {
                onTangemPayCurrentPlanScreen {
                    planRow(label = visaProgrammeAttribute, value = basicProgramValue).assertIsDisplayed()
                }
            }
            step("Assert '$planFeeAttribute' row is displayed") {
                onTangemPayCurrentPlanScreen {
                    planRow(label = planFeeAttribute, value = basicFeeValue).assertIsDisplayed()
                }
            }
            step("Assert '$maxCardsIssuedAttribute' row is displayed") {
                onTangemPayCurrentPlanScreen {
                    planRow(label = maxCardsIssuedAttribute, value = basicMaxCards).assertIsDisplayed()
                }
            }
            step("Open plan selection") { openTangemPayChangePlan() }
            step("Assert '$plusPlan' plan is displayed") {
                onTangemPaySelectPlanScreen { title.assertTextEquals(plusPlan) }
            }
            step("Click on 'Select' button") { selectTangemPayPlanForConfirmation() }
            step("Assert the upgrade confirmation title is displayed") {
                onTangemPaySelectPlanScreen { title.assertTextEquals(upgradeConfirmTitle) }
            }
            step("Assert the monthly fee point is displayed") {
                onTangemPaySelectPlanScreen { point(monthlyFeePoint).assertIsDisplayed() }
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
            step("Move the plan fee order to 'processing' in WireMock") { stageTangemPayPlusOrderProcessing() }
            step("Assert 'Top-up your account' banner is not displayed") {
                assertTangemPayAwaitingDepositBannerHidden(plusFee)
            }
            step("Complete the plan order in WireMock") { stageTangemPayPlanOrderCompleted(planPlusState) }
            step("Pull to refresh Tangem Pay") { pullToRefreshTangemPay() }
            step("Open 'Current plan' screen") { openTangemPayCurrentPlan(assertMenuItems = true) }
            step("Assert current plan is '$plusPlan'") { assertTangemPayCurrentPlanName(plusPlan) }
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
            step("Open 'Current plan' screen") { openTangemPayCurrentPlan(assertMenuItems = true) }
            step("Open plan selection") { openTangemPayChangePlan() }
            step("Assert '$plusPlan' plan is displayed") {
                onTangemPaySelectPlanScreen { title.assertTextEquals(plusPlan) }
            }
            step("Click on 'Select' button") { selectTangemPayPlanForConfirmation() }
            val ordersBefore = getTangemPayPlanOrderRequestCount()
            step("Complete the plan order in WireMock") { completeTangemPayPlusOrderWithPlanFee(planPlusState) }
            step("Click on 'Upgrade plan' button") {
                onTangemPaySelectPlanScreen { confirmButton.clickWithAssertion() }
            }
            step("Assert payment account balance is displayed") {
                awaitSuccess(timeoutMillis = WAIT_UNTIL_TIMEOUT_LONG) {
                    onTangemPayMainScreen { balance.assertIsDisplayed() }
                }
            }
            step("Pull to refresh Tangem Pay") { pullToRefreshTangemPay() }
            step("Assert the plan fee transaction is displayed") {
                assertTangemPayTransactionRowDisplayed(planFeeTransaction)
            }
            step("Open 'Current plan' screen") { openTangemPayCurrentPlan() }
            step("Assert current plan is '$plusPlan'") { assertTangemPayCurrentPlanName(plusPlan) }
            step("Assert the plan order request was sent once") {
                assertTangemPayTariffRequestSentOnce("plan order", ordersBefore, ::getTangemPayPlanOrderRequestCount)
            }
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
            step("Assert the card is displayed") {
                onTangemPayMainScreen { cardTileWithDigits(cardLastDigits).assertIsDisplayed() }
            }
            val cancelsBefore = getTangemPayCancelPlanOrderRequestCount()
            step("Open 'Current plan' screen") { openTangemPayCurrentPlan() }
            step("Click on 'Cancel $plusPlan, move to $basicPlan' button") {
                awaitSuccess { onTangemPayCurrentPlanScreen { notificationButton.assertIsDisplayed() } }
                onTangemPayCurrentPlanScreen { notificationButton.performClick() }
            }
            step("Drain the plan order list in WireMock") { drainTangemPayPlanOrders() }
            step("Assert the order cancellation request was sent once") {
                assertTangemPayTariffRequestSentOnce(
                    name = "order cancellation",
                    countBefore = cancelsBefore,
                    count = ::getTangemPayCancelPlanOrderRequestCount,
                )
            }
            step("Assert the plan notification is not displayed") {
                awaitSuccess(timeoutMillis = WAIT_UNTIL_TIMEOUT_LONG) {
                    onTangemPayCurrentPlanScreen { notification.assertDoesNotExist() }
                }
            }
            step("Press back to the payment account") { pressBackFromCurrentPlan() }
            step("Assert 'Top-up your account' banner is not displayed") {
                assertTangemPayAwaitingDepositBannerHidden(plusFee)
            }
            step("Assert the card is still displayed") {
                onTangemPayMainScreen { cardTileWithDigits(cardLastDigits).assertIsDisplayed() }
            }
            step("Open 'Current plan' screen") { openTangemPayCurrentPlan(assertMenuItems = true) }
            step("Assert current plan is '$basicPlan'") { assertTangemPayCurrentPlanName(basicPlan) }
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
            step("Open 'Current plan' screen") { openTangemPayCurrentPlan(assertMenuItems = true) }
            step("Assert current plan is '$plusPlan'") { assertTangemPayCurrentPlanName(plusPlan) }
            step("Assert '$visaProgrammeAttribute' row is displayed") {
                onTangemPayCurrentPlanScreen {
                    planRow(label = visaProgrammeAttribute, value = plusProgramValue).assertIsDisplayed()
                }
            }
            step("Assert '$planFeeAttribute' row is displayed") {
                onTangemPayCurrentPlanScreen {
                    planRow(label = planFeeAttribute, value = plusFee).assertIsDisplayed()
                }
            }
            step("Assert '$maxCardsIssuedAttribute' row is displayed") {
                onTangemPayCurrentPlanScreen {
                    planRow(label = maxCardsIssuedAttribute, value = plusMaxCards).assertIsDisplayed()
                }
            }
            step("Open plan selection") { openTangemPayChangePlan() }
            step("Assert '$basicPlan' plan is displayed") {
                onTangemPaySelectPlanScreen { title.assertTextEquals(basicPlan) }
            }
            step("Click on 'Select' button") { selectTangemPayPlanForConfirmation() }
            step("Assert the downgrade confirmation is displayed") {
                onTangemPaySelectPlanScreen { point(noFeeAppliedPoint).assertIsDisplayed() }
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
                        notificationButtonWithText(stayOnPlusButton).assertIsDisplayed()
                    }
                }
            }
            step("Assert the pending transition request was sent once") {
                assertTangemPayTariffRequestSentOnce(
                    name = "pending transition",
                    countBefore = requestsBefore,
                    count = ::getTangemPayPendingTransitionRequestCount,
                )
            }
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
            step("Open 'Current plan' screen") { openTangemPayCurrentPlan(assertMenuItems = true) }
            step("Assert the pending downgrade notification is displayed") {
                awaitSuccess {
                    onTangemPayCurrentPlanScreen {
                        notificationButtonWithText(stayOnPlusButton).assertIsDisplayed()
                    }
                }
            }
            val requestsBefore = getTangemPayCancelPendingTransitionRequestCount()
            step("Click on '$stayOnPlusButton' button") {
                onTangemPayCurrentPlanScreen { notificationButton.clickWithAssertion() }
            }
            step("Assert the 'Stay on $plusPlan' sheet title is displayed") {
                awaitSuccess {
                    onTangemPayTariffMessageSheet { stayOnPlanTitle(plusPlan).assertIsDisplayed() }
                }
            }
            step("Assert the 'Stay on $plusPlan' sheet description is displayed") {
                onTangemPayTariffMessageSheet { stayOnPlanDescription(basicPlan).assertIsDisplayed() }
            }
            step("Keep the customer on $plusPlan in WireMock") { stageTangemPayPlanState(planPlusState) }
            step("Click on '$stayOnPlusButton' button on the sheet") {
                onTangemPayTariffMessageSheet { primaryButton.clickWithAssertion() }
            }
            step("Assert the pending transition cancellation request was sent once") {
                assertTangemPayTariffRequestSentOnce(
                    name = "pending transition cancellation",
                    countBefore = requestsBefore,
                    count = ::getTangemPayCancelPendingTransitionRequestCount,
                )
            }
            step("Assert current plan is '$plusPlan'") { assertTangemPayCurrentPlanName(plusPlan) }
            step("Assert the 'Stay on $plusPlan' offer is not displayed") {
                awaitSuccess(timeoutMillis = WAIT_UNTIL_TIMEOUT_LONG) {
                    onTangemPayCurrentPlanScreen {
                        notificationButtonWithText(stayOnPlusButton).assertDoesNotExist()
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
            step("Assert current plan is '$basicPlan'") { assertTangemPayCurrentPlanName(basicPlan) }
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
            step("Assert 'Cards limit reached for this plan' sheet title is displayed") {
                awaitSuccess { onTangemPayTariffMessageSheet { cardsLimitForPlanTitle.assertIsDisplayed() } }
            }
            step("Assert 'Cards limit reached for this plan' sheet description is displayed") {
                onTangemPayTariffMessageSheet { cardsLimitForPlanDescription.assertIsDisplayed() }
            }
            step("Assert 'Cancel' button is displayed") {
                onTangemPayTariffMessageSheet { cancelButton.assertIsDisplayed() }
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
            step("Assert 'Maximum Cards Issued' sheet title is displayed") {
                awaitSuccess { onTangemPayTariffMessageSheet { maximumCardsTitle.assertIsDisplayed() } }
            }
            step("Assert 'Maximum Cards Issued' sheet description is displayed") {
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
            step("Assert current plan is '$basicPlan'") { assertTangemPayCurrentPlanName(basicPlan) }
            step("Open plan selection") { openTangemPayChangePlan() }
            step("Assert '$plusPlan' plan is displayed") {
                onTangemPaySelectPlanScreen { title.assertTextEquals(plusPlan) }
            }
        }
    }

    @AllureId("9592")
    @DisplayName("Tangem Pay: 'Full terms' on the current plan opens the tariffs document")
    @Test
    fun fullTermsOpensTariffsDocumentFromCurrentPlanTest() {
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
            step("Stub the external browser so the tariffs document is not actually opened") {
                stubExternalUrlOpening()
            }
            step("Click on 'Full terms' button") {
                onTangemPayCurrentPlanScreen { fullTermsButton.clickWithAssertion() }
            }
            step("Assert the tariffs document is opened") {
                assertUrlOpenedStartingWith(TangemPayConstants.TERMS_AND_LIMITS_URL)
            }
        }
    }
}