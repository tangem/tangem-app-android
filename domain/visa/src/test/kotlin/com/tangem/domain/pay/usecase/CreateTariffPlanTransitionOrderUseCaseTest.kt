package com.tangem.domain.pay.usecase

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.domain.models.account.TangemPayTariffPlanTransition
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.model.Order
import com.tangem.domain.pay.model.OrderStatus
import com.tangem.domain.pay.model.OrderStep
import com.tangem.domain.pay.flow.PaymentAccountStatusFetcher
import com.tangem.domain.pay.model.OrderType
import com.tangem.domain.pay.repository.CustomerOrderRepository
import com.tangem.domain.pay.repository.TangemPayIssueCardRepository
import com.tangem.domain.visa.error.VisaApiError
import com.tangem.test.core.TestAppCoroutineScope
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

internal class CreateTariffPlanTransitionOrderUseCaseTest {

    private val customerOrderRepository: CustomerOrderRepository = mockk()
    private val issueCardRepository: TangemPayIssueCardRepository = mockk(relaxed = true)
    private val startTangemPayOrderPollingUseCase: StartTangemPayOrderPollingUseCase = mockk(relaxed = true)
    private val paymentAccountStatusFetcher: PaymentAccountStatusFetcher = mockk(relaxed = true)

    private val useCase = CreateTariffPlanTransitionOrderUseCase(
        customerOrderRepository = customerOrderRepository,
        issueCardRepository = issueCardRepository,
        startTangemPayOrderPollingUseCase = startTangemPayOrderPollingUseCase,
        paymentAccountStatusFetcher = paymentAccountStatusFetcher,
        appCoroutineScope = TestAppCoroutineScope(),
    )

    @Test
    fun `GIVEN findOrders fails WHEN invoke THEN returns Left and skips create and store`() = runTest {
        // GIVEN
        coEvery {
            customerOrderRepository.findOrders(USER_WALLET_ID, ACTIVE_TRANSITION_TYPES, OrderStatus.activeStatuses)
        } returns VisaApiError.Unspecified.left()

        // WHEN
        val result = useCase(USER_WALLET_ID, TARGET_PLAN_ID, TangemPayTariffPlanTransition.Type.UPGRADE)

        // THEN
        assertThat(result.leftOrNull()).isEqualTo(VisaApiError.Unspecified)
        coVerify(exactly = 0) {
            customerOrderRepository.createOrder(any(), any(), any(), any(), any(), any())
        }
        coVerify(exactly = 0) { issueCardRepository.storeIssueOrderId(any(), any()) }
        coVerify(exactly = 0) { paymentAccountStatusFetcher.invoke(any<UserWalletId>()) }
    }

    @Test
    fun `GIVEN active transition order exists WHEN invoke THEN reuses it without calling createOrder`() = runTest {
        // GIVEN
        val existing = order(id = "existing", status = OrderStatus.PROCESSING)
        coEvery {
            customerOrderRepository.findOrders(USER_WALLET_ID, ACTIVE_TRANSITION_TYPES, OrderStatus.activeStatuses)
        } returns listOf(existing).right()

        // WHEN
        val result = useCase(USER_WALLET_ID, TARGET_PLAN_ID, TangemPayTariffPlanTransition.Type.UPGRADE)

        // THEN
        assertThat(result.isRight()).isTrue()
        coVerify(exactly = 0) {
            customerOrderRepository.createOrder(any(), any(), any(), any(), any(), any())
        }
        coVerify(exactly = 1) { issueCardRepository.storeIssueOrderId(USER_WALLET_ID, existing.id) }
    }

    @Test
    fun `GIVEN no active order AND createOrder fails WHEN invoke THEN returns Left and skips store`() = runTest {
        // GIVEN
        coEvery {
            customerOrderRepository.findOrders(USER_WALLET_ID, ACTIVE_TRANSITION_TYPES, OrderStatus.activeStatuses)
        } returns emptyList<Order>().right()
        coEvery {
            customerOrderRepository.createOrder(
                userWalletId = USER_WALLET_ID,
                type = OrderType.TARIFF_PLAN_TRANSITION,
                specificationName = null,
                targetTariffPlanId = TARGET_PLAN_ID,
                transitionType = TangemPayTariffPlanTransition.Type.UPGRADE,
                idempotencyKey = any(),
            )
        } returns VisaApiError.Unspecified.left()

        // WHEN
        val result = useCase(USER_WALLET_ID, TARGET_PLAN_ID, TangemPayTariffPlanTransition.Type.UPGRADE)

        // THEN
        assertThat(result.leftOrNull()).isEqualTo(VisaApiError.Unspecified)
        coVerify(exactly = 0) { issueCardRepository.storeIssueOrderId(any(), any()) }
    }

    @Test
    fun `GIVEN no active order AND createOrder succeeds WHEN invoke THEN stores the new order id`() = runTest {
        // GIVEN
        val newOrder = order(id = "new", status = OrderStatus.NEW)
        coEvery {
            customerOrderRepository.findOrders(USER_WALLET_ID, ACTIVE_TRANSITION_TYPES, OrderStatus.activeStatuses)
        } returns emptyList<Order>().right()
        coEvery {
            customerOrderRepository.createOrder(
                userWalletId = USER_WALLET_ID,
                type = OrderType.TARIFF_PLAN_TRANSITION,
                specificationName = null,
                targetTariffPlanId = TARGET_PLAN_ID,
                transitionType = TangemPayTariffPlanTransition.Type.UPGRADE,
                idempotencyKey = any(),
            )
        } returns newOrder.right()

        // WHEN
        val result = useCase(USER_WALLET_ID, TARGET_PLAN_ID, TangemPayTariffPlanTransition.Type.UPGRADE)

        // THEN
        assertThat(result.isRight()).isTrue()
        coVerify(exactly = 1) { issueCardRepository.storeIssueOrderId(USER_WALLET_ID, newOrder.id) }
        coVerify(exactly = 1) { paymentAccountStatusFetcher.invoke(USER_WALLET_ID) }
    }

    private fun order(id: String, status: OrderStatus): Order = Order(
        id = id,
        customerId = "customer",
        type = OrderType.TARIFF_PLAN_TRANSITION,
        status = status,
        step = OrderStep.UNKNOWN,
        stepChangeCode = null,
        productInstanceId = null,
        paymentAccountId = null,
        cardId = null,
        toTariffPlanId = TARGET_PLAN_ID,
        withdrawTxHash = null,
        createdAt = null,
        updatedAt = null,
    )

    private companion object {
        val USER_WALLET_ID = UserWalletId("aabbcc112233")
        const val TARGET_PLAN_ID = "plan-plus"
        val ACTIVE_TRANSITION_TYPES = setOf(OrderType.TARIFF_PLAN_TRANSITION)
    }
}