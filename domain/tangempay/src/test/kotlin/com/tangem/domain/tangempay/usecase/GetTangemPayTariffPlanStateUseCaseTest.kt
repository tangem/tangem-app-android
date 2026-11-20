package com.tangem.domain.tangempay.usecase

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.domain.models.account.TangemPayCustomerTariffPlan
import com.tangem.domain.models.account.TangemPayTariffPlan
import com.tangem.domain.models.account.TangemPayTariffPlanState
import com.tangem.domain.models.account.TangemPayTariffPlanTransition
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.tangempay.model.Order
import com.tangem.domain.tangempay.model.OrderStatus
import com.tangem.domain.tangempay.model.OrderStep
import com.tangem.domain.tangempay.model.OrderType
import com.tangem.domain.tangempay.repository.CustomerOrderRepository
import com.tangem.domain.tangempay.error.TangemPayApiError
import com.tangem.test.core.ProvideTestModels
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class GetTangemPayTariffPlanStateUseCaseTest {

    private val customerOrderRepository: CustomerOrderRepository = mockk()
    private val getTariffPlanTransitions: GetTangemPayTariffPlanTransitionsUseCase = mockk()

    private val useCase = GetTangemPayTariffPlanStateUseCase(
        customerOrderRepository = customerOrderRepository,
        getTariffPlanTransitions = getTariffPlanTransitions,
    )

    @BeforeEach
    fun resetMocks() {
        clearMocks(customerOrderRepository, getTariffPlanTransitions)
    }

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN no active transition order or findOrders fails WHEN invoke THEN state has no order`(
        foundOrders: Either<TangemPayApiError, List<Order>>,
    ) = runTest {
        // Arrange
        coEvery { customerOrderRepository.findOrders(USER_WALLET_ID, TRANSITION_TYPES, ACTIVE) } returns foundOrders

        // Act
        val result = useCase(USER_WALLET_ID, CUSTOMER_TARIFF)

        // Assert
        assertThat(result).isEqualTo(TangemPayTariffPlanState(tariff = CUSTOMER_TARIFF, order = null))
    }

    @Test
    fun `GIVEN active order with non-awaiting step WHEN invoke THEN order step is Unknown`() = runTest {
        // Arrange
        val order = order(step = OrderStep.UNKNOWN)
        coEvery { customerOrderRepository.findOrders(USER_WALLET_ID, TRANSITION_TYPES, ACTIVE) } returns
            listOf(order).right()

        // Act
        val result = useCase(USER_WALLET_ID, CUSTOMER_TARIFF)

        // Assert
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
        // Arrange
        val order = order(step = OrderStep.AWAITING_DEPOSIT)
        coEvery { customerOrderRepository.findOrders(USER_WALLET_ID, TRANSITION_TYPES, ACTIVE) } returns
            listOf(order).right()
        coEvery { getTariffPlanTransitions(USER_WALLET_ID) } returns emptyList<TangemPayTariffPlanTransition>().right()

        // Act
        val result = useCase(USER_WALLET_ID, CUSTOMER_TARIFF)

        // Assert
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
            // Arrange
            val order = order(step = OrderStep.AWAITING_DEPOSIT)
            val transition = TangemPayTariffPlanTransition(
                type = TangemPayTariffPlanTransition.Type.UPGRADE,
                plan = TARGET_PLAN,
            )
            coEvery { customerOrderRepository.findOrders(USER_WALLET_ID, TRANSITION_TYPES, ACTIVE) } returns
                listOf(order).right()
            coEvery { getTariffPlanTransitions(USER_WALLET_ID) } returns listOf(transition).right()

            // Act
            val result = useCase(USER_WALLET_ID, CUSTOMER_TARIFF)

            // Assert
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

    private fun provideTestModels(): List<Either<TangemPayApiError, List<Order>>> = listOf(
        emptyList<Order>().right(),
        TangemPayApiError.Unspecified.left(),
    )

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