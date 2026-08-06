package com.tangem.features.tangempay.tiers.current

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.decompose.model.MutableParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.core.ui.utils.DateTimeFormatters
import com.tangem.domain.models.account.AccountStatus
import com.tangem.domain.models.account.PaymentAccountStatusValue
import com.tangem.domain.models.account.TangemPayCustomerTariffPlan
import com.tangem.domain.models.account.TangemPayTariffPlanState
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.flow.PaymentAccountStatusSupplier
import com.tangem.domain.pay.usecase.CancelTariffPlanPendingTransitionUseCase
import com.tangem.domain.pay.usecase.CancelTariffTransitionUseCase
import com.tangem.domain.tangempay.TangemPayAnalyticsEvents
import com.tangem.domain.visa.error.VisaApiError
import com.tangem.features.tangempay.awaitingDepositOrder
import com.tangem.features.tangempay.customerTariffPlan
import com.tangem.features.tangempay.details.impl.R
import com.tangem.features.tangempay.tariffPlan
import com.tangem.features.tangempay.tariffPlanState
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import io.mockk.verify
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.emptyFlow
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class TangemPayCurrentPlanModelTest {

    private val router: Router = mockk(relaxed = true)
    private val cancelPendingTransition: CancelTariffPlanPendingTransitionUseCase = mockk()
    private val cancelTariffTransition: CancelTariffTransitionUseCase = mockk()
    private val paymentAccountStatusSupplier: PaymentAccountStatusSupplier = mockk()
    private val uiMessageSender: UiMessageSender = mockk(relaxed = true)
    private val analytics: AnalyticsEventHandler = mockk(relaxed = true)

    @BeforeEach
    fun setUp() {
        clearMocks(router, cancelPendingTransition, cancelTariffTransition, uiMessageSender, analytics)
        every { paymentAccountStatusSupplier(USER_WALLET_ID) } returns emptyFlow()
        // DateTimeFormatters.dateMMMd resolves the locale pattern through the Android framework,
        // which is unavailable on the JVM — pin it to a fixed joda pattern instead.
        mockkObject(DateTimeFormatters)
        every { DateTimeFormatters.dateMMMd } returns DateTimeFormat.forPattern("MMM d")
    }

    @AfterEach
    fun tearDown() {
        unmockkObject(DateTimeFormatters)
    }

    @Test
    fun `GIVEN awaiting deposit order WHEN model created THEN notification shows cancel transition button`() {
        // GIVEN + WHEN
        val model = createModel(planState = awaitingDepositState())

        // THEN
        val notification = model.state.value.notification
        assertThat(notification?.text).isEqualTo(
            resourceReference(
                R.string.tangempay_current_plan_awaiting_deposit_notification,
                wrappedList("PLUS"),
            ),
        )
        assertThat(notification?.button?.text).isEqualTo(
            resourceReference(
                R.string.tangempay_card_details_awaiting_deposit_cancel_button,
                wrappedList("PLUS", "BASIC"),
            ),
        )
        assertThat(notification?.button?.isProcessing).isFalse()
    }

    @Test
    fun `GIVEN awaiting deposit order WHEN model created THEN change plan button is hidden`() {
        // GIVEN + WHEN
        val model = createModel(planState = awaitingDepositState())

        // THEN
        assertThat(model.state.value.onChangePlanClick).isNull()
    }

    @Test
    fun `GIVEN transitioning status WHEN model created THEN change plan button is hidden`() {
        // GIVEN
        val planState = tariffPlanState(
            tariff = customerTariffPlan(status = TangemPayCustomerTariffPlan.Status.TRANSITIONING),
        )

        // WHEN
        val model = createModel(planState = planState)

        // THEN
        assertThat(model.state.value.onChangePlanClick).isNull()
    }

    @Test
    fun `GIVEN downgrade pending status WHEN model created THEN change plan button is hidden`() {
        // GIVEN
        val planState = tariffPlanState(
            tariff = customerTariffPlan(
                status = TangemPayCustomerTariffPlan.Status.DOWNGRADE_PENDING,
                pendingPlan = tariffPlan(tierId = "BASIC", isBasicTier = true),
                nextBillingAt = NEXT_BILLING_AT,
            ),
        )

        // WHEN
        val model = createModel(planState = planState)

        // THEN
        assertThat(model.state.value.onChangePlanClick).isNull()
    }

    @Test
    fun `GIVEN active plan without pending changes WHEN model created THEN change plan button is available`() {
        // GIVEN + WHEN
        val model = createModel(planState = tariffPlanState())

        // THEN
        assertThat(model.state.value.onChangePlanClick).isNotNull()
    }

    @Test
    fun `GIVEN awaiting deposit AND next billing date WHEN model created THEN awaiting deposit notification wins`() {
        // GIVEN
        val planState = tariffPlanState(
            tariff = customerTariffPlan(nextBillingAt = NEXT_BILLING_AT),
            order = awaitingDepositOrder(),
        )

        // WHEN
        val model = createModel(planState = planState)

        // THEN
        assertThat(model.state.value.notification?.text).isEqualTo(
            resourceReference(
                R.string.tangempay_current_plan_awaiting_deposit_notification,
                wrappedList("PLUS"),
            ),
        )
    }

    @Test
    fun `GIVEN awaiting deposit WHEN cancel clicked THEN transition cancelled by order id AND analytics sent`() {
        // GIVEN
        coEvery { cancelTariffTransition(USER_WALLET_ID, ORDER_ID) } returns Unit.right()
        val model = createModel(planState = awaitingDepositState())

        // WHEN
        model.state.value.notification?.button?.onClick?.invoke()

        // THEN
        coVerify(exactly = 1) { cancelTariffTransition(USER_WALLET_ID, ORDER_ID) }
        verify(exactly = 1) {
            analytics.send(ofType<TangemPayAnalyticsEvents.Tiers.CancelPlusMoveToBasicClicked>())
        }
        verify(exactly = 0) { uiMessageSender.send(any()) }
    }

    @Test
    fun `GIVEN cancel fails WHEN cancel clicked THEN error message sent AND processing reset`() {
        // GIVEN
        coEvery { cancelTariffTransition(USER_WALLET_ID, ORDER_ID) } returns VisaApiError.Unspecified.left()
        val model = createModel(planState = awaitingDepositState())

        // WHEN
        model.state.value.notification?.button?.onClick?.invoke()

        // THEN
        verify(exactly = 1) { uiMessageSender.send(any()) }
        assertThat(model.state.value.notification?.button?.isProcessing).isFalse()
    }

    @Test
    fun `GIVEN cancel in progress WHEN cancel clicked again THEN use case called once AND back is ignored`() {
        // GIVEN
        val pending = CompletableDeferred<Either<VisaApiError, Unit>>()
        coEvery { cancelTariffTransition(USER_WALLET_ID, ORDER_ID) } coAnswers { pending.await() }
        val model = createModel(planState = awaitingDepositState())
        model.state.value.notification?.button?.onClick?.invoke()

        // WHEN
        model.state.value.notification?.button?.onClick?.invoke()
        model.state.value.onBackClick()

        // THEN
        assertThat(model.state.value.notification?.button?.isProcessing).isTrue()
        coVerify(exactly = 1) { cancelTariffTransition(USER_WALLET_ID, ORDER_ID) }
        verify(exactly = 0) { router.pop() }
    }

    @Test
    fun `GIVEN supplier emits cleared order WHEN collected THEN banner hidden AND change plan available`() {
        // GIVEN
        val statuses = MutableSharedFlow<AccountStatus.Payment>(replay = 1)
        every { paymentAccountStatusSupplier(USER_WALLET_ID) } returns statuses
        val model = createModel(planState = awaitingDepositState())

        // WHEN
        statuses.tryEmit(paymentStatus(tariffPlanState()))

        // THEN
        assertThat(model.state.value.notification).isNull()
        assertThat(model.state.value.onChangePlanClick).isNotNull()
    }

    private fun createModel(planState: TangemPayTariffPlanState): TangemPayCurrentPlanModel {
        return TangemPayCurrentPlanModel(
            paramsContainer = MutableParamsContainer(
                TangemPayCurrentPlanComponent.Params(
                    userWalletId = USER_WALLET_ID,
                    tariffPlan = planState,
                ),
            ),
            dispatchers = TestingCoroutineDispatcherProvider(),
            router = router,
            cancelPendingTransition = cancelPendingTransition,
            cancelTariffTransition = cancelTariffTransition,
            paymentAccountStatusSupplier = paymentAccountStatusSupplier,
            uiMessageSender = uiMessageSender,
            analytics = analytics,
        )
    }

    private fun awaitingDepositState(): TangemPayTariffPlanState = tariffPlanState(
        tariff = customerTariffPlan(plan = tariffPlan(tierId = "BASIC", isBasicTier = true)),
        order = awaitingDepositOrder(orderId = ORDER_ID),
    )

    private fun paymentStatus(planState: TangemPayTariffPlanState): AccountStatus.Payment {
        val loaded: PaymentAccountStatusValue.Loaded = mockk(relaxed = true) {
            every { tariffPlan } returns planState
        }
        return mockk(relaxed = true) {
            every { value } returns loaded
        }
    }

    private companion object {
        val USER_WALLET_ID = UserWalletId("aabbcc112233")
        val NEXT_BILLING_AT: DateTime = DateTime.parse("2026-03-23T00:00:00Z")
        const val ORDER_ID = "order-42"
    }
}