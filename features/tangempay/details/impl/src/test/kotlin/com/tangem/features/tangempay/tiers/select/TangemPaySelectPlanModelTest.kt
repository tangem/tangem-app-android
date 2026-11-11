package com.tangem.features.tangempay.tiers.select

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.common.routing.AppRoute
import com.tangem.core.decompose.model.MutableParamsContainer
import com.tangem.core.navigation.url.UrlOpener
import com.tangem.core.ui.extensions.stringReference
import com.tangem.domain.models.account.TangemPayCustomerTariffPlan
import com.tangem.domain.models.account.TangemPayTariffPlan
import com.tangem.domain.models.account.TangemPayTariffPlanTransition
import com.tangem.domain.models.tangempay.TangemPayProduct
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.moneyaccount.model.StartTangemPayOnboardingError
import com.tangem.domain.moneyaccount.usecase.StartTangemPayOnboardingUseCase
import com.tangem.domain.tangempay.usecase.GetCurrentTariffUseCase
import com.tangem.domain.tangempay.usecase.GetTangemPayTariffPlanTransitionsUseCase
import com.tangem.domain.tangempay.usecase.SubmitTariffTransitionUseCase
import com.tangem.domain.tangempay.TangemPayAnalyticsEvents
import com.tangem.domain.tangempay.error.TangemPayApiError
import com.tangem.features.tangempay.TangemPayModelTestBase
import com.tangem.features.tangempay.account.TangemPayAccountDetailsInnerRoute
import com.tangem.features.tangempay.components.TangemPaySelectPlanComponent
import com.tangem.test.core.ProvideTestModels
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CompletableDeferred
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class TangemPaySelectPlanModelTest : TangemPayModelTestBase() {

    private val getTransitions: GetTangemPayTariffPlanTransitionsUseCase = mockk()
    private val getCurrentTariffUseCase: GetCurrentTariffUseCase = mockk()
    private val submitTariffTransitionUseCase: SubmitTariffTransitionUseCase = mockk()
    private val startTangemPayOnboardingUseCase: StartTangemPayOnboardingUseCase = mockk()
    private val urlOpener: UrlOpener = mockk(relaxed = true)

    @BeforeEach
    fun setUp() {
        clearMocks(
            analytics,
            router,
            uiMessageSender,
            getTransitions,
            getCurrentTariffUseCase,
            submitTariffTransitionUseCase,
            startTangemPayOnboardingUseCase,
            urlOpener,
        )
    }

    private fun createModel(
        mode: TangemPaySelectPlanComponent.Mode = TangemPaySelectPlanComponent.Mode.ChangePlan,
        currentTariff: Either<TangemPayApiError, TangemPayCustomerTariffPlan?> = CUSTOMER_TARIFF.right(),
        transitions: List<TangemPayTariffPlanTransition> = DEFAULT_TRANSITIONS,
        transitionsResult: Either<TangemPayApiError, List<TangemPayTariffPlanTransition>> = transitions.right(),
    ): TangemPaySelectPlanModel {
        coEvery { getTransitions(USER_WALLET_ID) } returns transitionsResult
        coEvery { getCurrentTariffUseCase(USER_WALLET_ID) } returns currentTariff
        return createModelWithoutStubs(mode = mode)
    }

    private fun createModelWithoutStubs(
        mode: TangemPaySelectPlanComponent.Mode = TangemPaySelectPlanComponent.Mode.ChangePlan,
    ): TangemPaySelectPlanModel {
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
        ).destroyAfterTest()
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Loading {

        @Test
        fun `GIVEN mixed transitions WHEN model created THEN only allowed types are shown as plans`() {
            // Act
            val model = createModel()

            // Assert
            assertThat(model.state.value.plans).hasSize(3)
            verify(exactly = 1) {
                analytics.send(ofType<TangemPayAnalyticsEvents.Tiers.TierSelectionScreenShowed>())
            }
        }

        @Test
        fun `GIVEN getTransitions fails WHEN model created THEN error content shown`() {
            // Arrange
            val transitionsResult: Either<TangemPayApiError, List<TangemPayTariffPlanTransition>> =
                TangemPayApiError.Unspecified.left()

            // Act
            val model = createModel(transitionsResult = transitionsResult)

            // Assert
            assertThat(model.state.value.plans).isEmpty()
            assertThat(model.state.value.content).isInstanceOf(TangemPaySelectPlanUM.Content.Error::class.java)
        }

        @Test
        fun `GIVEN getTransitions pending WHEN model created THEN loading content shown`() {
            // Arrange
            val pending = CompletableDeferred<Either<TangemPayApiError, List<TangemPayTariffPlanTransition>>>()
            coEvery { getTransitions(USER_WALLET_ID) } coAnswers { pending.await() }
            coEvery { getCurrentTariffUseCase(USER_WALLET_ID) } returns CUSTOMER_TARIFF.right()

            // Act
            val model = createModelWithoutStubs()
            val contentWhilePending = model.state.value.content
            pending.complete(DEFAULT_TRANSITIONS.right())

            // Assert
            assertThat(contentWhilePending).isEqualTo(TangemPaySelectPlanUM.Content.Loading)
            assertThat(model.state.value.content).isInstanceOf(TangemPaySelectPlanUM.Content.Select::class.java)
        }

        @Test
        fun `GIVEN error content WHEN onRetryClick AND getTransitions succeeds THEN select content with plans shown`() {
            // Arrange
            val model = createModel(transitionsResult = TangemPayApiError.Unspecified.left())
            val errorContent = model.state.value.content as TangemPaySelectPlanUM.Content.Error
            coEvery { getTransitions(USER_WALLET_ID) } returns DEFAULT_TRANSITIONS.right()

            // Act
            errorContent.onRetryClick()

            // Assert
            assertThat(model.state.value.content).isInstanceOf(TangemPaySelectPlanUM.Content.Select::class.java)
            assertThat(model.state.value.plans).hasSize(3)
            coVerify(exactly = 2) { getTransitions(USER_WALLET_ID) }
            coVerify(exactly = 2) { getCurrentTariffUseCase(USER_WALLET_ID) }
        }

        @Test
        fun `GIVEN current tariff loading fails WHEN model created THEN error content shown`() {
            // Act
            val model = createModel(currentTariff = TangemPayApiError.Unspecified.left())

            // Assert
            assertThat(model.state.value.content).isInstanceOf(TangemPaySelectPlanUM.Content.Error::class.java)
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class AnalyticsByMode {

        @ParameterizedTest
        @MethodSource("provideAnalyticsModels")
        fun `GIVEN mode WHEN model created THEN tier selection event sent with its products`(model: AnalyticsModel) {
            // Act
            createModel(mode = model.mode)

            // Assert
            verify(exactly = 1) {
                analytics.send(TangemPayAnalyticsEvents.Tiers.TierSelectionScreenShowed(products = model.expectedProducts))
            }
        }

        @ParameterizedTest
        @MethodSource("provideAnalyticsModels")
        fun `GIVEN mode WHEN plan selected THEN plan event sent with its products and context`(model: AnalyticsModel) {
            // Arrange
            coEvery { submitTariffTransitionUseCase(USER_WALLET_ID, any()) } returns Unit.right()
            coEvery { startTangemPayOnboardingUseCase(any(), any(), any()) } returns Unit.right()
            val selectPlanModel = createModel(mode = model.mode)

            // Act
            selectContent(selectPlanModel).onSelectClick()

            // Assert
            verify(exactly = 1) {
                analytics.send(
                    TangemPayAnalyticsEvents.Tiers.PlanSelectedClick(
                        tierId = UPGRADE_TRANSITION.plan.tierId,
                        products = model.expectedProducts,
                        context = model.expectedContext,
                    ),
                )
            }
        }

        private fun provideAnalyticsModels() = listOf(
            AnalyticsModel(
                mode = TangemPaySelectPlanComponent.Mode.ChangePlan,
                expectedProducts = null,
                expectedContext = TangemPayAnalyticsEvents.Tiers.PlanContext.CHANGE_PLAN,
            ),
            AnalyticsModel(
                mode = TangemPaySelectPlanComponent.Mode.TiersOnboarding,
                expectedProducts = null,
                expectedContext = TangemPayAnalyticsEvents.Tiers.PlanContext.ONBOARDING,
            ),
            AnalyticsModel(
                mode = TangemPaySelectPlanComponent.Mode.CreateOnboardingOrder(
                    availableProducts = setOf(TangemPayProduct.PAYMENT_ACCOUNT, TangemPayProduct.BANKING_ACCOUNT),
                ),
                expectedProducts = TangemPayAnalyticsEvents.ProductSet.CARD_AND_ACCOUNT,
                expectedContext = TangemPayAnalyticsEvents.Tiers.PlanContext.ONBOARDING,
            ),
            AnalyticsModel(
                mode = TangemPaySelectPlanComponent.Mode.CreateOnboardingOrder(
                    availableProducts = setOf(TangemPayProduct.PAYMENT_ACCOUNT),
                ),
                expectedProducts = TangemPayAnalyticsEvents.ProductSet.CARD,
                expectedContext = TangemPayAnalyticsEvents.Tiers.PlanContext.ONBOARDING,
            ),
        )
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class PlanSwipe {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN plan index WHEN onPlanSelected THEN selectedIndex follows and swipe analytics sent only for a new index`(
            testModel: PlanSwipeModel,
        ) {
            // Arrange
            val model = createModel()

            // Act
            model.state.value.onPlanSelected(testModel.index)

            // Assert
            assertThat(model.state.value.selectedIndex).isEqualTo(testModel.index)
            verify(exactly = testModel.expectedSwipeEvents) {
                analytics.send(ofType<TangemPayAnalyticsEvents.Tiers.TiersSwiped>())
            }
        }

        private fun provideTestModels() = listOf(
            PlanSwipeModel(index = 1, expectedSwipeEvents = 1),
            PlanSwipeModel(index = 0, expectedSwipeEvents = 0),
        )
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class SelectButtonIcon {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN mode WHEN model created THEN select button has icon only for onboarding order with money account`(
            testModel: SelectButtonIconModel,
        ) {
            // Act
            val model = createModel(mode = testModel.mode)

            // Assert
            assertThat(selectContent(model).selectButtonIconEnd != null).isEqualTo(testModel.hasIcon)
        }

        private fun provideTestModels() = listOf(
            SelectButtonIconModel(
                name = "onboarding order with money account",
                mode = CREATE_ONBOARDING_ORDER_MODE,
                hasIcon = true,
            ),
            SelectButtonIconModel(
                name = "onboarding order without money account",
                mode = TangemPaySelectPlanComponent.Mode.CreateOnboardingOrder(
                    availableProducts = setOf(TangemPayProduct.PAYMENT_ACCOUNT),
                ),
                hasIcon = false,
            ),
            SelectButtonIconModel(
                name = "change plan",
                mode = TangemPaySelectPlanComponent.Mode.ChangePlan,
                hasIcon = false,
            ),
        )
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class SelectClick {

        @Test
        fun `GIVEN change plan mode WHEN onSelectClick THEN switches to confirm content`() {
            // Arrange
            val model = createModel(mode = TangemPaySelectPlanComponent.Mode.ChangePlan)

            // Act
            selectContent(model).onSelectClick()

            // Assert
            assertThat(model.state.value.content).isInstanceOf(TangemPaySelectPlanUM.Content.Confirm::class.java)
            verify(exactly = 1) { analytics.send(ofType<TangemPayAnalyticsEvents.Tiers.PlanSelectedClick>()) }
            verify(exactly = 1) {
                analytics.send(ofType<TangemPayAnalyticsEvents.Tiers.PlanChangeConfirmationScreenShowed>())
            }
        }

        @Test
        fun `GIVEN onboarding mode AND submit succeeds WHEN onSelectClick THEN replaces to account details`() {
            // Arrange
            coEvery { submitTariffTransitionUseCase(USER_WALLET_ID, any()) } returns Unit.right()
            val model = createModel(mode = TangemPaySelectPlanComponent.Mode.TiersOnboarding)

            // Act
            selectContent(model).onSelectClick()

            // Assert
            coVerify(exactly = 1) { submitTariffTransitionUseCase(USER_WALLET_ID, UPGRADE_TRANSITION) }
            verify(exactly = 1) { router.replaceAll(TangemPayAccountDetailsInnerRoute.AccountDetails) }
        }

        @Test
        fun `GIVEN create onboarding order mode WHEN onSelectClick THEN starts onboarding with selected plan`() {
            // Arrange
            coEvery { startTangemPayOnboardingUseCase(any(), any(), any()) } returns Unit.right()
            val model = createModel(mode = CREATE_ONBOARDING_ORDER_MODE)

            // Act
            selectContent(model).onSelectClick()

            // Assert
            coVerify(exactly = 1) {
                startTangemPayOnboardingUseCase(
                    userWalletId = USER_WALLET_ID,
                    tariffPlanTransition = UPGRADE_TRANSITION,
                    availableProducts = AVAILABLE_PRODUCTS,
                )
            }
            coVerify(exactly = 0) { submitTariffTransitionUseCase(any(), any()) }
            verify(exactly = 0) { uiMessageSender.send(any()) }
            verify(exactly = 1) { router.replaceAll(AppRoute.Wallet, AppRoute.TangemPayOnboardingSuccess) }
        }

        @Test
        fun `GIVEN create onboarding order mode AND onboarding fails WHEN onSelectClick THEN processing reset and error shown`() {
            // Arrange
            coEvery {
                startTangemPayOnboardingUseCase(any(), any(), any())
            } returns StartTangemPayOnboardingError.Failed.left()
            val model = createModel(mode = CREATE_ONBOARDING_ORDER_MODE)

            // Act
            selectContent(model).onSelectClick()

            // Assert
            assertThat(selectContent(model).isProcessing).isFalse()
            verify(exactly = 1) { uiMessageSender.send(any()) }
            verify(exactly = 0) { router.pop() }
            verify(exactly = 0) { router.replaceAll(AppRoute.Wallet, AppRoute.TangemPayOnboardingSuccess) }
        }

        @Test
        fun `GIVEN create onboarding order mode AND user cancelled WHEN onSelectClick THEN processing reset without error`() {
            // Arrange
            coEvery {
                startTangemPayOnboardingUseCase(any(), any(), any())
            } returns StartTangemPayOnboardingError.UserCancelled.left()
            val model = createModel(mode = CREATE_ONBOARDING_ORDER_MODE)

            // Act
            selectContent(model).onSelectClick()

            // Assert
            assertThat(selectContent(model).isProcessing).isFalse()
            verify(exactly = 0) { uiMessageSender.send(any()) }
        }

        @Test
        fun `GIVEN no allowed transitions WHEN onSelectClick THEN nothing happens`() {
            // Arrange
            val model = createModel(transitions = listOf(SYSTEM_DOWNGRADE_TRANSITION))

            // Act
            selectContent(model).onSelectClick()

            // Assert
            verify(exactly = 0) { analytics.send(ofType<TangemPayAnalyticsEvents.Tiers.PlanSelectedClick>()) }
            coVerify(exactly = 0) { submitTariffTransitionUseCase(any(), any()) }
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class ComparePlans {

        @Test
        fun `GIVEN current tariff loaded WHEN onComparePlansClick THEN compare starts with current plan`() {
            // Arrange
            val model = createModel()

            // Act
            selectContent(model).onComparePlansClick()

            // Assert
            val compare = requireNotNull(model.state.value.compare)
            assertThat(compare.plans.map { it.name }).containsExactly(
                stringReference(CUSTOMER_TARIFF.plan.name),
                stringReference(UPGRADE_TRANSITION.plan.name),
                stringReference(DOWNGRADE_TRANSITION.plan.name),
            ).inOrder()
            coVerify(exactly = 1) { getCurrentTariffUseCase(USER_WALLET_ID) }
        }

        @Test
        fun `GIVEN no current tariff WHEN onComparePlansClick THEN compare has only transitions`() {
            // Arrange
            val model = createModel(currentTariff = null.right())

            // Act
            selectContent(model).onComparePlansClick()

            // Assert
            val compare = requireNotNull(model.state.value.compare)
            assertThat(compare.plans.map { it.name }).containsExactly(
                stringReference(UPGRADE_TRANSITION.plan.name),
                stringReference(DOWNGRADE_TRANSITION.plan.name),
            ).inOrder()
        }

        @Test
        fun `GIVEN allowed transitions WHEN onComparePlansClick THEN compare is shown with analytics`() {
            // Arrange
            val model = createModel()

            // Act
            selectContent(model).onComparePlansClick()

            // Assert
            assertThat(model.state.value.compare).isNotNull()
            verify(exactly = 1) { analytics.send(ofType<TangemPayAnalyticsEvents.Tiers.ComparePlansClicked>()) }
            verify(exactly = 1) { analytics.send(ofType<TangemPayAnalyticsEvents.Tiers.PlansComparisonPopupShowed>()) }
        }

        @Test
        fun `GIVEN no allowed transitions WHEN onComparePlansClick THEN compare stays hidden`() {
            // Arrange
            val model = createModel(transitions = listOf(SYSTEM_DOWNGRADE_TRANSITION))

            // Act
            selectContent(model).onComparePlansClick()

            // Assert
            assertThat(model.state.value.compare).isNull()
            verify(exactly = 0) { analytics.send(ofType<TangemPayAnalyticsEvents.Tiers.ComparePlansClicked>()) }
        }

        @Test
        fun `GIVEN compare shown WHEN onDismiss THEN compare hidden with analytics`() {
            // Arrange
            val model = createModel()
            selectContent(model).onComparePlansClick()

            // Act
            model.state.value.compare!!.onDismiss()

            // Assert
            assertThat(model.state.value.compare).isNull()
            verify(exactly = 1) { analytics.send(ofType<TangemPayAnalyticsEvents.Tiers.PlansComparisonPopupClosed>()) }
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class ConfirmClick {

        @Test
        fun `GIVEN confirm AND submit fails WHEN onConfirmClick THEN processing reset and error shown`() {
            // Arrange
            coEvery { submitTariffTransitionUseCase(USER_WALLET_ID, any()) } returns TangemPayApiError.Unspecified.left()
            val model = createModel(mode = TangemPaySelectPlanComponent.Mode.ChangePlan)
            selectContent(model).onSelectClick()

            // Act
            confirmContent(model).onConfirmClick()

            // Assert
            verify(exactly = 1) { uiMessageSender.send(any()) }
            assertThat(confirmContent(model).isProcessing).isFalse()
            verify(exactly = 0) { router.pop() }
        }

        @Test
        fun `GIVEN change plan non-upgrade AND submit succeeds WHEN onConfirmClick THEN router pops`() {
            // Arrange
            coEvery { submitTariffTransitionUseCase(USER_WALLET_ID, any()) } returns Unit.right()
            val model = createModel(mode = TangemPaySelectPlanComponent.Mode.ChangePlan)
            model.state.value.onPlanSelected(ACTIVATION_INDEX)
            selectContent(model).onSelectClick()

            // Act
            confirmContent(model).onConfirmClick()

            // Assert
            verify(exactly = 1) { router.pop() }
            verify(exactly = 0) { router.replaceAll(TangemPayAccountDetailsInnerRoute.AccountDetails) }
        }

        @Test
        fun `GIVEN change plan upgrade AND submit succeeds WHEN onConfirmClick THEN replaces to account details`() {
            // Arrange
            coEvery { submitTariffTransitionUseCase(USER_WALLET_ID, any()) } returns Unit.right()
            val model = createModel(mode = TangemPaySelectPlanComponent.Mode.ChangePlan)
            selectContent(model).onSelectClick()

            // Act
            confirmContent(model).onConfirmClick()

            // Assert
            coVerify(exactly = 1) { submitTariffTransitionUseCase(USER_WALLET_ID, UPGRADE_TRANSITION) }
            verify(exactly = 1) { router.replaceAll(TangemPayAccountDetailsInnerRoute.AccountDetails) }
            verify(exactly = 0) { router.pop() }
        }

        @Test
        fun `GIVEN upgrade transition WHEN applied THEN upgrade analytics sent`() {
            // Arrange
            coEvery { submitTariffTransitionUseCase(USER_WALLET_ID, any()) } returns Unit.right()
            val model = createModel(mode = TangemPaySelectPlanComponent.Mode.ChangePlan)
            selectContent(model).onSelectClick()

            // Act
            confirmContent(model).onConfirmClick()

            // Assert
            verify(exactly = 1) { analytics.send(ofType<TangemPayAnalyticsEvents.Tiers.PlanChangeUpgradeClicked>()) }
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Navigation {

        @Test
        fun `GIVEN markdown link WHEN onLinkClick THEN url is opened in app`() {
            // Arrange
            val model = createModel()

            // Act
            model.state.value.onLinkClick("https://tangem.com/visa-benefits")

            // Assert
            verify(exactly = 1) { urlOpener.openUrl("https://tangem.com/visa-benefits") }
        }

        @Test
        fun `GIVEN markdown link to app deep link WHEN onLinkClick THEN nothing is opened`() {
            // Arrange
            val model = createModel()

            // Act
            model.state.value.onLinkClick("tangem://wc?uri=wc:topic@2?relay-protocol=irn&symKey=secret")

            // Assert
            verify(exactly = 0) { urlOpener.openUrl(any()) }
        }

        @Test
        fun `GIVEN confirm shown WHEN onBackClick THEN returns to select content without pop`() {
            // Arrange
            val model = createModel(mode = TangemPaySelectPlanComponent.Mode.ChangePlan)
            selectContent(model).onSelectClick()

            // Act
            model.onBackClick()

            // Assert
            assertThat(model.state.value.content).isInstanceOf(TangemPaySelectPlanUM.Content.Select::class.java)
            verify(exactly = 1) { analytics.send(ofType<TangemPayAnalyticsEvents.Tiers.PlanChangeCancelClicked>()) }
            verify(exactly = 0) { router.pop() }
        }

        @Test
        fun `GIVEN select shown WHEN onBackClick THEN router pops`() {
            // Arrange
            val model = createModel()

            // Act
            model.onBackClick()

            // Assert
            verify(exactly = 1) { router.pop() }
        }

        @Test
        fun `GIVEN not processing WHEN onCloseClick THEN router pops`() {
            // Arrange
            val model = createModel()

            // Act
            model.state.value.onCloseClick()

            // Assert
            verify(exactly = 1) { router.pop() }
        }

        @Test
        fun `GIVEN processing in progress WHEN onCloseClick THEN pop is ignored`() {
            // Arrange
            val pending = CompletableDeferred<arrow.core.Either<TangemPayApiError, Unit>>()
            coEvery { submitTariffTransitionUseCase(USER_WALLET_ID, any()) } coAnswers { pending.await() }
            val model = createModel(mode = TangemPaySelectPlanComponent.Mode.TiersOnboarding)
            selectContent(model).onSelectClick()

            // Act
            model.state.value.onCloseClick()

            // Assert
            verify(exactly = 0) { router.pop() }
        }
    }

    private fun selectContent(model: TangemPaySelectPlanModel) =
        model.state.value.content as TangemPaySelectPlanUM.Content.Select

    private fun confirmContent(model: TangemPaySelectPlanModel) =
        model.state.value.content as TangemPaySelectPlanUM.Content.Confirm

    data class AnalyticsModel(
        val mode: TangemPaySelectPlanComponent.Mode,
        val expectedProducts: TangemPayAnalyticsEvents.ProductSet?,
        val expectedContext: TangemPayAnalyticsEvents.Tiers.PlanContext,
    )

    data class PlanSwipeModel(val index: Int, val expectedSwipeEvents: Int)

    data class SelectButtonIconModel(
        val name: String,
        val mode: TangemPaySelectPlanComponent.Mode,
        val hasIcon: Boolean,
    ) {
        override fun toString(): String = name
    }

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