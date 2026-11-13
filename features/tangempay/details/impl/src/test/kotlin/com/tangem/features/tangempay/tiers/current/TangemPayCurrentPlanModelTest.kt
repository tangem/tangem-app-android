package com.tangem.features.tangempay.tiers.current

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.common.routing.AppRoute
import com.tangem.core.decompose.model.MutableParamsContainer
import com.tangem.core.navigation.url.UrlOpener
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.core.ui.utils.DateTimeFormatters
import com.tangem.domain.models.account.AccountStatus
import com.tangem.domain.models.account.PaymentAccountStatusValue
import com.tangem.domain.models.account.TangemPayCustomerTariffPlan
import com.tangem.domain.models.account.TangemPayTariffPlanState
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.models.tangempay.TangemPayProduct
import com.tangem.domain.moneyaccount.flow.MoneyAccountStatusFetcher
import com.tangem.domain.moneyaccount.usecase.GetTangemPayProductsAvailabilityUseCase
import com.tangem.domain.tangempay.flow.PaymentAccountStatusFetcher
import com.tangem.domain.tangempay.flow.PaymentAccountStatusSupplier
import com.tangem.domain.tangempay.usecase.CancelTangemPayOrderUseCase
import com.tangem.domain.tangempay.usecase.CancelTariffPlanPendingTransitionUseCase
import com.tangem.domain.tangempay.TangemPayAnalyticsEvents
import com.tangem.domain.tangempay.error.TangemPayApiError
import com.tangem.features.tangempay.TangemPayConstants
import com.tangem.features.tangempay.TangemPayModelTestBase
import com.tangem.features.tangempay.awaitingDepositOrder
import com.tangem.features.tangempay.customerTariffPlan
import com.tangem.features.tangempay.details.impl.R
import com.tangem.features.tangempay.tariffPlan
import com.tangem.features.tangempay.tariffPlanState
import com.tangem.test.core.ProvideTestModels
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
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
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class TangemPayCurrentPlanModelTest : TangemPayModelTestBase() {

    private val cancelPendingTransition: CancelTariffPlanPendingTransitionUseCase = mockk()
    private val cancelTangemPayOrder: CancelTangemPayOrderUseCase = mockk()
    private val paymentAccountStatusSupplier: PaymentAccountStatusSupplier = mockk()
    private val paymentAccountStatusFetcher: PaymentAccountStatusFetcher = mockk()
    private val moneyAccountStatusFetcher: MoneyAccountStatusFetcher = mockk()
    private val getProductsAvailability: GetTangemPayProductsAvailabilityUseCase = mockk()
    private val urlOpener: UrlOpener = mockk(relaxed = true)

    @BeforeEach
    fun setUp() {
        clearMocks(
            router,
            cancelPendingTransition,
            cancelTangemPayOrder,
            paymentAccountStatusFetcher,
            moneyAccountStatusFetcher,
            getProductsAvailability,
            uiMessageSender,
            analytics,
            urlOpener,
            paymentAccountStatusSupplier,
        )
        every { paymentAccountStatusSupplier(USER_WALLET_ID) } returns emptyFlow()
        coEvery { paymentAccountStatusFetcher(USER_WALLET_ID) } returns Unit.right()
        coEvery { moneyAccountStatusFetcher(USER_WALLET_ID) } returns Unit.right()
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
        // Act
        val model = createModel(planState = awaitingDepositState())

        // Assert
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

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN plan state WHEN model created THEN change plan button available only without pending changes`(
        case: ChangePlanModel,
    ) {
        // Act
        val model = createModel(planState = case.planState)

        // Assert
        assertThat(model.state.value.onChangePlanClick != null).isEqualTo(case.isChangePlanAvailable)
    }

    @Test
    fun `GIVEN active plan WHEN change plan clicked THEN change plan event sent from the payment account`() {
        // Arrange
        val model = createModel(planState = tariffPlanState())

        // Act
        model.state.value.onChangePlanClick?.invoke()

        // Assert
        verify(exactly = 1) { analytics.send(TangemPayAnalyticsEvents.Tiers.ChangePlanClicked()) }
    }

    @Test
    fun `GIVEN markdown link WHEN onLinkClick THEN url is opened in app`() {
        // Arrange
        val model = createModel(planState = tariffPlanState())

        // Act
        model.state.value.onLinkClick("https://tangem.com/visa-benefits")

        // Assert
        verify(exactly = 1) { urlOpener.openUrl("https://tangem.com/visa-benefits") }
    }

    @Test
    fun `GIVEN markdown link to app deep link WHEN onLinkClick THEN nothing is opened`() {
        // Arrange
        val model = createModel(planState = tariffPlanState())

        // Act
        model.state.value.onLinkClick("tangem://wc?uri=wc:topic@2?relay-protocol=irn&symKey=secret")

        // Assert
        verify(exactly = 0) { urlOpener.openUrl(any()) }
    }

    @Test
    fun `GIVEN awaiting deposit AND next billing date WHEN model created THEN awaiting deposit notification wins`() {
        // Arrange
        val planState = tariffPlanState(
            tariff = customerTariffPlan(nextBillingAt = NEXT_BILLING_AT),
            order = awaitingDepositOrder(),
        )

        // Act
        val model = createModel(planState = planState)

        // Assert
        assertThat(model.state.value.notification?.text).isEqualTo(
            resourceReference(
                R.string.tangempay_current_plan_awaiting_deposit_notification,
                wrappedList("PLUS"),
            ),
        )
    }

    @Test
    fun `GIVEN awaiting deposit WHEN cancel clicked THEN order cancelled by id AND analytics sent`() {
        // Arrange
        coEvery { cancelTangemPayOrder(USER_WALLET_ID, ORDER_ID) } returns Unit.right()
        val model = createModel(planState = awaitingDepositState())

        // Act
        model.state.value.notification?.button?.onClick?.invoke()

        // Assert
        coVerify(exactly = 1) { cancelTangemPayOrder(USER_WALLET_ID, ORDER_ID) }
        verify(exactly = 1) {
            analytics.send(ofType<TangemPayAnalyticsEvents.Tiers.CancelPlusMoveToBasicClicked>())
        }
        verify(exactly = 0) { uiMessageSender.send(any()) }
    }

    @Test
    fun `GIVEN cancel in progress WHEN cancel clicked again THEN use case called once AND back is ignored`() {
        // Arrange
        val pending = CompletableDeferred<Either<TangemPayApiError, Unit>>()
        coEvery { cancelTangemPayOrder(USER_WALLET_ID, ORDER_ID) } coAnswers { pending.await() }
        val model = createModel(planState = awaitingDepositState())
        model.state.value.notification?.button?.onClick?.invoke()

        // Act
        model.state.value.notification?.button?.onClick?.invoke()
        model.state.value.onBackClick()

        // Assert
        assertThat(model.state.value.notification?.button?.isProcessing).isTrue()
        coVerify(exactly = 1) { cancelTangemPayOrder(USER_WALLET_ID, ORDER_ID) }
        verify(exactly = 0) { router.pop() }
    }

    @Test
    fun `GIVEN onboarding order AND products available WHEN cancel clicked THEN verified opened`() {
        // Arrange
        coEvery { cancelTangemPayOrder(USER_WALLET_ID, ORDER_ID) } returns Unit.right()
        coEvery { getProductsAvailability(USER_WALLET_ID) } returns setOf(TangemPayProduct.PAYMENT_ACCOUNT).right()
        val model = createModel(planState = awaitingDepositState(source = TangemPayCustomerTariffPlan.Source.DEFAULT))

        // Act
        model.state.value.notification?.button?.onClick?.invoke()

        // Assert
        coVerify(exactly = 1) { cancelTangemPayOrder(USER_WALLET_ID, ORDER_ID) }
        coVerifyOrder {
            paymentAccountStatusFetcher(USER_WALLET_ID)
            getProductsAvailability(USER_WALLET_ID)
        }
        coVerifyOrder {
            moneyAccountStatusFetcher(USER_WALLET_ID)
            getProductsAvailability(USER_WALLET_ID)
        }
        verify(exactly = 1) {
            router.replaceAll(
                AppRoute.Wallet,
                AppRoute.TangemPayVerified(userWalletId = USER_WALLET_ID),
                onComplete = any(),
            )
        }
        verify(exactly = 0) { uiMessageSender.send(any()) }
    }

    @ParameterizedTest
    @MethodSource("paymentAccountUnavailableResults")
    fun `GIVEN onboarding order AND payment account unavailable WHEN cancel clicked THEN wallet opened`(
        availability: Either<TangemPayApiError, Set<TangemPayProduct>>,
    ) {
        // Arrange
        coEvery { cancelTangemPayOrder(USER_WALLET_ID, ORDER_ID) } returns Unit.right()
        coEvery { getProductsAvailability(USER_WALLET_ID) } returns availability
        val model = createModel(planState = awaitingDepositState(source = TangemPayCustomerTariffPlan.Source.DEFAULT))

        // Act
        model.state.value.notification?.button?.onClick?.invoke()

        // Assert
        verify(exactly = 1) { router.replaceAll(AppRoute.Wallet, onComplete = any()) }
        verify(exactly = 0) { uiMessageSender.send(any()) }
    }

    private fun paymentAccountUnavailableResults(): List<Either<TangemPayApiError, Set<TangemPayProduct>>> = listOf(
        emptySet<TangemPayProduct>().right(),
        setOf(TangemPayProduct.BANKING_ACCOUNT).right(),
        TangemPayApiError.Unspecified.left(),
    )

    @Test
    fun `GIVEN upgrade order WHEN cancel clicked THEN order cancelled AND screen kept`() {
        // Arrange
        coEvery { cancelTangemPayOrder(USER_WALLET_ID, ORDER_ID) } returns Unit.right()
        val model = createModel(planState = awaitingDepositState())

        // Act
        model.state.value.notification?.button?.onClick?.invoke()

        // Assert
        coVerify(exactly = 1) { cancelTangemPayOrder(USER_WALLET_ID, ORDER_ID) }
        coVerify(exactly = 0) { paymentAccountStatusFetcher(any<UserWalletId>()) }
        coVerify(exactly = 0) { moneyAccountStatusFetcher(any<UserWalletId>()) }
        verify(exactly = 0) { router.replaceAll(routes = anyVararg(), onComplete = any()) }
        assertThat(model.state.value.notification?.button?.isProcessing).isFalse()
    }

    @Test
    fun `GIVEN cancel fails WHEN cancel clicked THEN error sent AND no navigation`() {
        // Arrange
        coEvery { cancelTangemPayOrder(USER_WALLET_ID, ORDER_ID) } returns TangemPayApiError.Unspecified.left()
        val model = createModel(planState = awaitingDepositState(source = TangemPayCustomerTariffPlan.Source.DEFAULT))

        // Act
        model.state.value.notification?.button?.onClick?.invoke()

        // Assert
        verify(exactly = 1) { uiMessageSender.send(any()) }
        verify(exactly = 0) { router.replaceAll(routes = anyVararg(), onComplete = any()) }
        assertThat(model.state.value.notification?.button?.isProcessing).isFalse()
    }

    @Test
    fun `GIVEN supplier emits cleared order WHEN collected THEN banner hidden AND change plan available`() {
        // Arrange
        val statuses = MutableSharedFlow<AccountStatus.Payment>(replay = 1)
        every { paymentAccountStatusSupplier(USER_WALLET_ID) } returns statuses
        val model = createModel(planState = awaitingDepositState())

        // Act
        statuses.tryEmit(paymentStatus(tariffPlanState()))

        // Assert
        assertThat(model.state.value.notification).isNull()
        assertThat(model.state.value.onChangePlanClick).isNotNull()
    }

    @Test
    fun `GIVEN any plan WHEN full terms clicked THEN card terms event sent AND terms link opened`() {
        // Arrange
        val model = createModel(planState = tariffPlanState())

        // Act
        model.state.value.onFullTermsClick()

        // Assert
        verify(exactly = 1) {
            analytics.send(
                TangemPayAnalyticsEvents.TermsAndLimitsClicked(product = TangemPayAnalyticsEvents.Product.CARD),
            )
        }
        verify(exactly = 1) { urlOpener.openUrl(match { it.startsWith(TangemPayConstants.TERMS_AND_LIMITS_URL) }) }
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
            cancelTangemPayOrderUseCase = cancelTangemPayOrder,
            paymentAccountStatusSupplier = paymentAccountStatusSupplier,
            paymentAccountStatusFetcher = paymentAccountStatusFetcher,
            moneyAccountStatusFetcher = moneyAccountStatusFetcher,
            getTangemPayProductsAvailabilityUseCase = getProductsAvailability,
            uiMessageSender = uiMessageSender,
            analytics = analytics,
            urlOpener = urlOpener,
        )
    }

    private fun awaitingDepositState(
        source: TangemPayCustomerTariffPlan.Source = TangemPayCustomerTariffPlan.Source.CUSTOMER,
    ): TangemPayTariffPlanState = tariffPlanState(
        tariff = customerTariffPlan(plan = tariffPlan(tierId = "BASIC", isBasicTier = true), source = source),
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

    private fun provideTestModels() = listOf(
        ChangePlanModel(
            name = "awaiting deposit order -> hidden",
            planState = awaitingDepositState(),
            isChangePlanAvailable = false,
        ),
        ChangePlanModel(
            name = "transitioning -> hidden",
            planState = tariffPlanState(
                tariff = customerTariffPlan(status = TangemPayCustomerTariffPlan.Status.TRANSITIONING),
            ),
            isChangePlanAvailable = false,
        ),
        ChangePlanModel(
            name = "downgrade pending -> hidden",
            planState = tariffPlanState(
                tariff = customerTariffPlan(
                    status = TangemPayCustomerTariffPlan.Status.DOWNGRADE_PENDING,
                    pendingPlan = tariffPlan(tierId = "BASIC", isBasicTier = true),
                    nextBillingAt = NEXT_BILLING_AT,
                ),
            ),
            isChangePlanAvailable = false,
        ),
        ChangePlanModel(
            name = "active plan without pending changes -> available",
            planState = tariffPlanState(),
            isChangePlanAvailable = true,
        ),
    )

    internal data class ChangePlanModel(
        val name: String,
        val planState: TangemPayTariffPlanState,
        val isChangePlanAvailable: Boolean,
    ) {
        override fun toString(): String = name
    }

    private companion object {
        val USER_WALLET_ID = UserWalletId("aabbcc112233")
        val NEXT_BILLING_AT: DateTime = DateTime.parse("2026-03-23T00:00:00Z")
        const val ORDER_ID = "order-42"
    }
}