package com.tangem.domain.pay.usecase

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.domain.models.account.TangemPayCustomerTariffPlan
import com.tangem.domain.models.account.TangemPayTariffPlan
import com.tangem.domain.models.account.TangemPayTariffPlanState
import com.tangem.domain.models.account.TangemPayTariffPlanTransition
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.model.Order
import com.tangem.domain.pay.model.OrderStatus
import com.tangem.domain.pay.model.OrderStep
import com.tangem.domain.pay.model.OrderType
import com.tangem.domain.pay.repository.CustomerOrderRepository
import com.tangem.domain.visa.error.VisaApiError
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

internal class GetTangemPayTariffPlanStateUseCaseTest {

    private val customerOrderRepository: CustomerOrderRepository = mockk()
    private val getTariffPlanTransitions: GetTangemPayTariffPlanTransitionsUseCase = mockk()

    private val useCase = GetTangemPayTariffPlanStateUseCase(
        customerOrderRepository = customerOrderRepository,
        getTariffPlanTransitions = getTariffPlanTransitions,
    )

    @Test
    fun `GIVEN no active transition order WHEN invoke THEN state has no order`() = runTest {
        // GIVEN
        coEvery { customerOrderRepository.findOrders(USER_WALLET_ID, TRANSITION_TYPES, ACTIVE) } returns
            emptyList<Order>().right()

        // WHEN
        val result = useCase(USER_WALLET_ID, CUSTOMER_TARIFF)

        // THEN
        assertThat(result).isEqualTo(TangemPayTariffPlanState(tariff = CUSTOMER_TARIFF, order = null))
    }

    @Test
    fun `GIVEN findOrders fails WHEN invoke THEN state has no order`() = runTest {
        // GIVEN
        coEvery { customerOrderRepository.findOrders(USER_WALLET_ID, TRANSITION_TYPES, ACTIVE) } returns
            VisaApiError.Unspecified.left()

        // WHEN
        val result = useCase(USER_WALLET_ID, CUSTOMER_TARIFF)

        // THEN
        assertThat(result).isEqualTo(TangemPayTariffPlanState(tariff = CUSTOMER_TARIFF, order = null))
    }

    @Test
    fun `GIVEN active order with non-awaiting step WHEN invoke THEN order step is Unknown`() = runTest {
        // GIVEN
        val order = order(step = OrderStep.UNKNOWN)
        coEvery { customerOrderRepository.findOrders(USER_WALLET_ID, TRANSITION_TYPES, ACTIVE) } returns
            listOf(order).right()

        // WHEN
        val result = useCase(USER_WALLET_ID, CUSTOMER_TARIFF)

        // THEN
        val expected = TangemPayTariffPlanState(
            tariff = CUSTOMER_TARIFF,
            order = TangemPayTariffPlanState.Order(
                orderId = ORDER_ID,
                step = TangemPayTariffPlanState.OrderStep.Unknown,
            ),
        )
        assertThat(result).isEqualTo(expected)
    }

    @Test
    fun `GIVEN awaiting-deposit order but no matching transition WHEN invoke THEN order step is Unknown`() = runTest {
        // GIVEN
        val order = order(step = OrderStep.AWAITING_DEPOSIT)
        coEvery { customerOrderRepository.findOrders(USER_WALLET_ID, TRANSITION_TYPES, ACTIVE) } returns
            listOf(order).right()
        coEvery { getTariffPlanTransitions(USER_WALLET_ID) } returns emptyList<TangemPayTariffPlanTransition>().right()

        // WHEN
        val result = useCase(USER_WALLET_ID, CUSTOMER_TARIFF)

        // THEN
        val expected = TangemPayTariffPlanState(
            tariff = CUSTOMER_TARIFF,
            order = TangemPayTariffPlanState.Order(
                orderId = ORDER_ID,
                step = TangemPayTariffPlanState.OrderStep.Unknown,
            ),
        )
        assertThat(result).isEqualTo(expected)
    }

    @Test
    fun `GIVEN awaiting-deposit order with matching transition WHEN invoke THEN order step is AwaitingDeposit`() =
        runTest {
            // GIVEN
            val order = order(step = OrderStep.AWAITING_DEPOSIT)
            val transition = TangemPayTariffPlanTransition(
                type = TangemPayTariffPlanTransition.Type.UPGRADE,
                plan = TARGET_PLAN,
            )
            coEvery { customerOrderRepository.findOrders(USER_WALLET_ID, TRANSITION_TYPES, ACTIVE) } returns
                listOf(order).right()
            coEvery { getTariffPlanTransitions(USER_WALLET_ID) } returns listOf(transition).right()

            // WHEN
            val result = useCase(USER_WALLET_ID, CUSTOMER_TARIFF)

            // THEN
            val expected = TangemPayTariffPlanState(
                tariff = CUSTOMER_TARIFF,
                order = TangemPayTariffPlanState.Order(
                    orderId = ORDER_ID,
                    step = TangemPayTariffPlanState.OrderStep.AwaitingDeposit(
                        fromPlan = CURRENT_PLAN,
                        toPlan = TARGET_PLAN,
                    ),
                ),
            )
            assertThat(result).isEqualTo(expected)
        }

    private fun order(step: OrderStep): Order = Order(
        id = ORDER_ID,
        customerId = "customer",
        type = OrderType.TARIFF_PLAN_TRANSITION,
        status = OrderStatus.PROCESSING,
        step = step,
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
        const val ORDER_ID = "order-test-1"
        const val TARGET_PLAN_ID = "plan-plus"
        val TRANSITION_TYPES = setOf(OrderType.TARIFF_PLAN_TRANSITION)
        val ACTIVE = OrderStatus.activeStatuses

        val CURRENT_PLAN = TangemPayTariffPlan(
            id = "plan-basic",
            tierId = "BASIC",
            isBasicTier = true,
            name = "Basic",
            programName = "program-basic",
            descriptionItems = emptyList(),
        )
        val TARGET_PLAN = TangemPayTariffPlan(
            id = TARGET_PLAN_ID,
            tierId = "PLUS",
            isBasicTier = false,
            name = "Plus",
            programName = "program-plus",
            descriptionItems = emptyList(),
        )
        val CUSTOMER_TARIFF = TangemPayCustomerTariffPlan(
            status = TangemPayCustomerTariffPlan.Status.ACTIVE,
            source = TangemPayCustomerTariffPlan.Source.CUSTOMER,
            plan = CURRENT_PLAN,
            nextBillingAt = null,
            pendingPlan = null,
            pendingTransitionAt = null,
        )
    }
}