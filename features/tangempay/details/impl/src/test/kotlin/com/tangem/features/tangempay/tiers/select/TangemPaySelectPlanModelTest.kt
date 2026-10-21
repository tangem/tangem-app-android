package com.tangem.features.tangempay.tiers.select

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.decompose.model.MutableParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.navigation.url.UrlOpener
import com.tangem.core.ui.extensions.stringReference
import com.tangem.domain.models.account.TangemPayCustomerTariffPlan
import com.tangem.domain.models.account.TangemPayTariffPlan
import com.tangem.domain.models.account.TangemPayTariffPlanTransition
import com.tangem.domain.models.pay.TangemPayProduct
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.moneyaccount.model.StartTangemPayOnboardingError
import com.tangem.domain.moneyaccount.usecase.StartTangemPayOnboardingUseCase
import com.tangem.domain.pay.usecase.GetCurrentTariffUseCase
import com.tangem.domain.pay.usecase.GetTangemPayTariffPlanTransitionsUseCase
import com.tangem.domain.pay.usecase.SubmitTariffTransitionUseCase
import com.tangem.domain.tangempay.TangemPayAnalyticsEvents
import com.tangem.domain.visa.error.VisaApiError
import com.tangem.features.tangempay.account.TangemPayAccountDetailsInnerRoute
import com.tangem.features.tangempay.components.TangemPaySelectPlanComponent
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CompletableDeferred
import org.junit.jupiter.api.Test

internal class TangemPaySelectPlanModelTest {

    private val router: Router = mockk(relaxed = true)
    private val getTransitions: GetTangemPayTariffPlanTransitionsUseCase = mockk()
    private val getCurrentTariffUseCase: GetCurrentTariffUseCase = mockk()
    private val submitTariffTransitionUseCase: SubmitTariffTransitionUseCase = mockk()
    private val startTangemPayOnboardingUseCase: StartTangemPayOnboardingUseCase = mockk()
    private val uiMessageSender: UiMessageSender = mockk(relaxed = true)
    private val analytics: AnalyticsEventHandler = mockk(relaxed = true)
    private val urlOpener: UrlOpener = mockk(relaxed = true)

    private fun createModel(
        mode: TangemPaySelectPlanComponent.Mode = TangemPaySelectPlanComponent.Mode.ChangePlan,
        currentTariff: Either<VisaApiError, TangemPayCustomerTariffPlan?> = CUSTOMER_TARIFF.right(),
        transitions: List<TangemPayTariffPlanTransition> = DEFAULT_TRANSITIONS,
    ): TangemPaySelectPlanModel {
        coEvery { getTransitions(USER_WALLET_ID) } returns transitions.right()
        coEvery { getCurrentTariffUseCase(USER_WALLET_ID) } returns currentTariff
        return TangemPaySelectPlanModel(
            paramsContainer = MutableParamsContainer(
                TangemPaySelectPlanComponent.Params(
                    userWalletId = USER_WALLET_ID,
                    mode = mode,
                ),
            ),
            dispatchers = TestingCoroutineDispatcherProvider(),
            router = router,
            getTransitions = getTransitions,
            getCurrentTariffUseCase = getCurrentTariffUseCase,
            submitTariffTransitionUseCase = submitTariffTransitionUseCase,
            startTangemPayOnboardingUseCase = startTangemPayOnboardingUseCase,
            uiMessageSender = uiMessageSender,
            analytics = analytics,
            urlOpener = urlOpener,
        )
    }

    @Test
    fun `GIVEN mixed transitions WHEN model created THEN only allowed types are shown as plans`() {
        // GIVEN + WHEN
        val model = createModel()

        // THEN
        assertThat(model.state.value.plans).hasSize(3)
        verify(exactly = 1) {
            analytics.send(ofType<TangemPayAnalyticsEvents.Tiers.TierSelectionScreenShowed>())
        }
    }

    @Test
    fun `GIVEN getTransitions fails WHEN model created THEN plans are empty`() {
        // GIVEN
        coEvery { getTransitions(USER_WALLET_ID) } returns VisaApiError.Unspecified.left()
        coEvery { getCurrentTariffUseCase(USER_WALLET_ID) } returns CUSTOMER_TARIFF.right()

        // WHEN
        val model = TangemPaySelectPlanModel(
            paramsContainer = MutableParamsContainer(
                TangemPaySelectPlanComponent.Params(
                    userWalletId = USER_WALLET_ID,
                    mode = TangemPaySelectPlanComponent.Mode.ChangePlan,
                ),
            ),
            dispatchers = TestingCoroutineDispatcherProvider(),
            router = router,
            getTransitions = getTransitions,
            getCurrentTariffUseCase = getCurrentTariffUseCase,
            submitTariffTransitionUseCase = submitTariffTransitionUseCase,
            startTangemPayOnboardingUseCase = startTangemPayOnboardingUseCase,
            uiMessageSender = uiMessageSender,
            analytics = analytics,
            urlOpener = urlOpener,
        )

        // THEN
        assertThat(model.state.value.plans).isEmpty()
    }

    @Test
    fun `GIVEN markdown link WHEN onLinkClick THEN url is opened in app`() {
        // GIVEN
        val model = createModel()

        // WHEN
        model.state.value.onLinkClick("https://tangem.com/visa-benefits")

        // THEN
        verify(exactly = 1) { urlOpener.openUrl("https://tangem.com/visa-benefits") }
    }

    @Test
    fun `GIVEN new index WHEN onPlanSelected THEN selectedIndex updates and swipe analytics sent`() {
        // GIVEN
        val model = createModel()

        // WHEN
        model.state.value.onPlanSelected(1)

        // THEN
        assertThat(model.state.value.selectedIndex).isEqualTo(1)
        verify(exactly = 1) { analytics.send(ofType<TangemPayAnalyticsEvents.Tiers.TiersSwiped>()) }
    }

    @Test
    fun `GIVEN same index WHEN onPlanSelected THEN nothing changes and no swipe analytics`() {
        // GIVEN
        val model = createModel()

        // WHEN
        model.state.value.onPlanSelected(0)

        // THEN
        assertThat(model.state.value.selectedIndex).isEqualTo(0)
        verify(exactly = 0) { analytics.send(ofType<TangemPayAnalyticsEvents.Tiers.TiersSwiped>()) }
    }

    @Test
    fun `GIVEN change plan mode WHEN onSelectClick THEN switches to confirm content`() {
        // GIVEN
        val model = createModel(mode = TangemPaySelectPlanComponent.Mode.ChangePlan)

        // WHEN
        selectContent(model).onSelectClick()

        // THEN
        assertThat(model.state.value.content).isInstanceOf(TangemPaySelectPlanUM.Content.Confirm::class.java)
        verify(exactly = 1) { analytics.send(ofType<TangemPayAnalyticsEvents.Tiers.PlanSelectedClick>()) }
        verify(exactly = 1) {
            analytics.send(ofType<TangemPayAnalyticsEvents.Tiers.PlanChangeConfirmationScreenShowed>())
        }
    }

    @Test
    fun `GIVEN onboarding mode AND submit succeeds WHEN onSelectClick THEN replaces to account details`() {
        // GIVEN
        coEvery { submitTariffTransitionUseCase(USER_WALLET_ID, any()) } returns Unit.right()
        val model = createModel(mode = TangemPaySelectPlanComponent.Mode.TiersOnboarding)

        // WHEN
        selectContent(model).onSelectClick()

        // THEN
        coVerify(exactly = 1) { submitTariffTransitionUseCase(USER_WALLET_ID, UPGRADE_TRANSITION) }
        verify(exactly = 1) { router.replaceAll(TangemPayAccountDetailsInnerRoute.AccountDetails) }
    }

    @Test
    fun `GIVEN create onboarding order mode WHEN onSelectClick THEN starts onboarding with selected plan`() {
        // GIVEN
        coEvery { startTangemPayOnboardingUseCase(any(), any(), any()) } returns Unit.right()
        val model = createModel(mode = CREATE_ONBOARDING_ORDER_MODE)

        // WHEN
        selectContent(model).onSelectClick()

        // THEN
        coVerify(exactly = 1) {
            startTangemPayOnboardingUseCase(
                userWalletId = USER_WALLET_ID,
                tariffPlanTransition = UPGRADE_TRANSITION,
                availableProducts = AVAILABLE_PRODUCTS,
            )
        }
        coVerify(exactly = 0) { submitTariffTransitionUseCase(any(), any()) }
        verify(exactly = 0) { uiMessageSender.send(any()) }
    }

    @Test
    fun `GIVEN create onboarding order mode AND onboarding fails WHEN onSelectClick THEN processing reset and error shown`() {
        // GIVEN
        coEvery {
            startTangemPayOnboardingUseCase(any(), any(), any())
        } returns StartTangemPayOnboardingError.Failed.left()
        val model = createModel(mode = CREATE_ONBOARDING_ORDER_MODE)

        // WHEN
        selectContent(model).onSelectClick()

        // THEN
        assertThat(selectContent(model).isProcessing).isFalse()
        verify(exactly = 1) { uiMessageSender.send(any()) }
        verify(exactly = 0) { router.pop() }
    }

    @Test
    fun `GIVEN create onboarding order mode AND user cancelled WHEN onSelectClick THEN processing reset without error`() {
        // GIVEN
        coEvery {
            startTangemPayOnboardingUseCase(any(), any(), any())
        } returns StartTangemPayOnboardingError.UserCancelled.left()
        val model = createModel(mode = CREATE_ONBOARDING_ORDER_MODE)

        // WHEN
        selectContent(model).onSelectClick()

        // THEN
        assertThat(selectContent(model).isProcessing).isFalse()
        verify(exactly = 0) { uiMessageSender.send(any()) }
    }

    @Test
    fun `GIVEN current tariff loaded WHEN onComparePlansClick THEN compare starts with current plan`() {
        // GIVEN
        val model = createModel()

        // WHEN
        selectContent(model).onComparePlansClick()

        // THEN
        val compare = requireNotNull(model.state.value.compare)
        assertThat(compare.plans.map { it.name }).containsExactly(
            stringReference(CUSTOMER_TARIFF.plan.name),
            stringReference(UPGRADE_TRANSITION.plan.name),
            stringReference(DOWNGRADE_TRANSITION.plan.name),
        ).inOrder()
        coVerify(exactly = 1) { getCurrentTariffUseCase(USER_WALLET_ID) }
    }

    @Test
    fun `GIVEN current tariff loading fails WHEN onComparePlansClick THEN compare has only transitions`() {
        // GIVEN
        val model = createModel(currentTariff = VisaApiError.Unspecified.left())

        // WHEN
        selectContent(model).onComparePlansClick()

        // THEN
        val compare = requireNotNull(model.state.value.compare)
        assertThat(compare.plans.map { it.name }).containsExactly(
            stringReference(UPGRADE_TRANSITION.plan.name),
            stringReference(DOWNGRADE_TRANSITION.plan.name),
        ).inOrder()
    }

    @Test
    fun `GIVEN no allowed transitions WHEN onSelectClick THEN nothing happens`() {
        // GIVEN
        val model = createModel(transitions = listOf(SYSTEM_DOWNGRADE_TRANSITION))

        // WHEN
        selectContent(model).onSelectClick()

        // THEN
        verify(exactly = 0) { analytics.send(ofType<TangemPayAnalyticsEvents.Tiers.PlanSelectedClick>()) }
        coVerify(exactly = 0) { submitTariffTransitionUseCase(any(), any()) }
    }

    @Test
    fun `GIVEN allowed transitions WHEN onComparePlansClick THEN compare is shown with analytics`() {
        // GIVEN
        val model = createModel()

        // WHEN
        selectContent(model).onComparePlansClick()

        // THEN
        assertThat(model.state.value.compare).isNotNull()
        verify(exactly = 1) { analytics.send(ofType<TangemPayAnalyticsEvents.Tiers.ComparePlansClicked>()) }
        verify(exactly = 1) { analytics.send(ofType<TangemPayAnalyticsEvents.Tiers.PlansComparisonPopupShowed>()) }
    }

    @Test
    fun `GIVEN no allowed transitions WHEN onComparePlansClick THEN compare stays hidden`() {
        // GIVEN
        val model = createModel(transitions = listOf(SYSTEM_DOWNGRADE_TRANSITION))

        // WHEN
        selectContent(model).onComparePlansClick()

        // THEN
        assertThat(model.state.value.compare).isNull()
        verify(exactly = 0) { analytics.send(ofType<TangemPayAnalyticsEvents.Tiers.ComparePlansClicked>()) }
    }

    @Test
    fun `GIVEN compare shown WHEN onDismiss THEN compare hidden with analytics`() {
        // GIVEN
        val model = createModel()
        selectContent(model).onComparePlansClick()

        // WHEN
        model.state.value.compare!!.onDismiss()

        // THEN
        assertThat(model.state.value.compare).isNull()
        verify(exactly = 1) { analytics.send(ofType<TangemPayAnalyticsEvents.Tiers.PlansComparisonPopupClosed>()) }
    }

    @Test
    fun `GIVEN confirm shown WHEN onBackClick THEN returns to select content without pop`() {
        // GIVEN
        val model = createModel(mode = TangemPaySelectPlanComponent.Mode.ChangePlan)
        selectContent(model).onSelectClick()

        // WHEN
        model.onBackClick()

        // THEN
        assertThat(model.state.value.content).isInstanceOf(TangemPaySelectPlanUM.Content.Select::class.java)
        verify(exactly = 1) { analytics.send(ofType<TangemPayAnalyticsEvents.Tiers.PlanChangeCancelClicked>()) }
        verify(exactly = 0) { router.pop() }
    }

    @Test
    fun `GIVEN select shown WHEN onBackClick THEN router pops`() {
        // GIVEN
        val model = createModel()

        // WHEN
        model.onBackClick()

        // THEN
        verify(exactly = 1) { router.pop() }
    }

    @Test
    fun `GIVEN not processing WHEN onCloseClick THEN router pops`() {
        // GIVEN
        val model = createModel()

        // WHEN
        model.state.value.onCloseClick()

        // THEN
        verify(exactly = 1) { router.pop() }
    }

    @Test
    fun `GIVEN confirm AND submit fails WHEN onConfirmClick THEN processing reset and error shown`() {
        // GIVEN
        coEvery { submitTariffTransitionUseCase(USER_WALLET_ID, any()) } returns VisaApiError.Unspecified.left()
        val model = createModel(mode = TangemPaySelectPlanComponent.Mode.ChangePlan)
        selectContent(model).onSelectClick()

        // WHEN
        confirmContent(model).onConfirmClick()

        // THEN
        verify(exactly = 1) { uiMessageSender.send(any()) }
        assertThat(confirmContent(model).isProcessing).isFalse()
        verify(exactly = 0) { router.pop() }
    }

    @Test
    fun `GIVEN change plan non-upgrade AND submit succeeds WHEN onConfirmClick THEN router pops`() {
        // GIVEN
        coEvery { submitTariffTransitionUseCase(USER_WALLET_ID, any()) } returns Unit.right()
        val model = createModel(mode = TangemPaySelectPlanComponent.Mode.ChangePlan)
        model.state.value.onPlanSelected(ACTIVATION_INDEX)
        selectContent(model).onSelectClick()

        // WHEN
        confirmContent(model).onConfirmClick()

        // THEN
        verify(exactly = 1) { router.pop() }
        verify(exactly = 0) { router.replaceAll(TangemPayAccountDetailsInnerRoute.AccountDetails) }
    }

    @Test
    fun `GIVEN change plan upgrade AND submit succeeds WHEN onConfirmClick THEN replaces to account details`() {
        // GIVEN
        coEvery { submitTariffTransitionUseCase(USER_WALLET_ID, any()) } returns Unit.right()
        val model = createModel(mode = TangemPaySelectPlanComponent.Mode.ChangePlan)
        selectContent(model).onSelectClick()

        // WHEN
        confirmContent(model).onConfirmClick()

        // THEN
        coVerify(exactly = 1) { submitTariffTransitionUseCase(USER_WALLET_ID, UPGRADE_TRANSITION) }
        verify(exactly = 1) { router.replaceAll(TangemPayAccountDetailsInnerRoute.AccountDetails) }
        verify(exactly = 0) { router.pop() }
    }

    @Test
    fun `GIVEN upgrade transition WHEN applied THEN upgrade analytics sent`() {
        // GIVEN
        coEvery { submitTariffTransitionUseCase(USER_WALLET_ID, any()) } returns Unit.right()
        val model = createModel(mode = TangemPaySelectPlanComponent.Mode.ChangePlan)
        selectContent(model).onSelectClick()

        // WHEN
        confirmContent(model).onConfirmClick()

        // THEN
        verify(exactly = 1) { analytics.send(ofType<TangemPayAnalyticsEvents.Tiers.PlanChangeUpgradeClicked>()) }
    }

    @Test
    fun `GIVEN processing in progress WHEN onCloseClick THEN pop is ignored`() {
        // GIVEN
        val pending = CompletableDeferred<arrow.core.Either<VisaApiError, Unit>>()
        coEvery { submitTariffTransitionUseCase(USER_WALLET_ID, any()) } coAnswers { pending.await() }
        val model = createModel(mode = TangemPaySelectPlanComponent.Mode.TiersOnboarding)

        // WHEN
        selectContent(model).onSelectClick()
        model.state.value.onCloseClick()

        // THEN
        verify(exactly = 0) { router.pop() }
    }

    private fun selectContent(model: TangemPaySelectPlanModel) =
        model.state.value.content as TangemPaySelectPlanUM.Content.Select

    private fun confirmContent(model: TangemPaySelectPlanModel) =
        model.state.value.content as TangemPaySelectPlanUM.Content.Confirm

    private companion object {
        val USER_WALLET_ID = UserWalletId("aabbcc112233")

        val AVAILABLE_PRODUCTS = setOf(TangemPayProduct.BANKING_ACCOUNT, TangemPayProduct.PAYMENT_ACCOUNT)
        val CREATE_ONBOARDING_ORDER_MODE = TangemPaySelectPlanComponent.Mode.CreateOnboardingOrder(
            availableProducts = AVAILABLE_PRODUCTS,
        )

        const val ACTIVATION_INDEX = 2
        val UPGRADE_TRANSITION = transition(TangemPayTariffPlanTransition.Type.UPGRADE, "PLUS", isBasic = false)
        val DOWNGRADE_TRANSITION = transition(TangemPayTariffPlanTransition.Type.DOWNGRADE, "BASIC", isBasic = true)
        val ACTIVATION_TRANSITION = transition(TangemPayTariffPlanTransition.Type.ACTIVATION, "PLUS", isBasic = false)
        val SYSTEM_DOWNGRADE_TRANSITION =
            transition(TangemPayTariffPlanTransition.Type.SYSTEM_DOWNGRADE, "BASIC", isBasic = true)
        val UNKNOWN_TRANSITION = transition(TangemPayTariffPlanTransition.Type.UNKNOWN, "BASIC", isBasic = true)

        val DEFAULT_TRANSITIONS = listOf(
            UPGRADE_TRANSITION,
            DOWNGRADE_TRANSITION,
            ACTIVATION_TRANSITION,
            SYSTEM_DOWNGRADE_TRANSITION,
            UNKNOWN_TRANSITION,
        )

        val CUSTOMER_TARIFF = TangemPayCustomerTariffPlan(
            status = TangemPayCustomerTariffPlan.Status.ACTIVE,
            source = TangemPayCustomerTariffPlan.Source.CUSTOMER,
            plan = plan("PLUS", isBasic = false),
            nextBillingAt = null,
            pendingPlan = null,
            pendingTransitionAt = null,
        )

        private fun transition(
            type: TangemPayTariffPlanTransition.Type,
            tierId: String,
            isBasic: Boolean,
        ) = TangemPayTariffPlanTransition(type = type, plan = plan(tierId, isBasic))

        private fun plan(tierId: String, isBasic: Boolean) = TangemPayTariffPlan(
            id = "plan-${tierId.lowercase()}",
            tierId = tierId,
            isBasicTier = isBasic,
            name = tierId,
            programName = "program-$tierId",
            descriptionItems = emptyList(),
        )
    }
}