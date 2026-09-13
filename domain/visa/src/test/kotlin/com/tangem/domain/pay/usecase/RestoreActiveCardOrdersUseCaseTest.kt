package com.tangem.domain.pay.usecase

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.model.Order
import com.tangem.domain.pay.model.OrderStatus
import com.tangem.domain.pay.model.OrderStep
import com.tangem.domain.pay.model.OrderType
import com.tangem.domain.pay.model.TangemPayOrderInfo
import com.tangem.domain.pay.repository.CustomerOrderRepository
import com.tangem.domain.pay.repository.TangemPayIssueCardRepository
import com.tangem.domain.visa.error.VisaApiError
import com.tangem.test.core.ProvideTestModels
import com.tangem.test.core.TestAppCoroutineScope
import io.mockk.CapturingSlot
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class RestoreActiveCardOrdersUseCaseTest {

    private val orderRepository: CustomerOrderRepository = mockk()
    private val issueCardRepository: TangemPayIssueCardRepository = mockk(relaxed = true)
    private val startTangemPayOrderPollingUseCase: StartTangemPayOrderPollingUseCase = mockk(relaxed = true)
    private val useCase = RestoreActiveCardOrdersUseCase(
        customerOrderRepository = orderRepository,
        issueCardRepository = issueCardRepository,
        startTangemPayOrderPollingUseCase = startTangemPayOrderPollingUseCase,
        appCoroutineScope = TestAppCoroutineScope(),
    )
    private val userWalletId = UserWalletId("1234567890ABCDEF")

    @BeforeEach
    fun resetMocks() {
        clearMocks(orderRepository, issueCardRepository, startTangemPayOrderPollingUseCase)
    }

    @Test
    fun `GIVEN active issue orders WHEN invoke THEN each order is stored and polled`() = runTest {
        // Arrange
        val first = order(id = "first", type = OrderType.CARD_ISSUE_VIRTUAL_RAIN, status = OrderStatus.NEW)
        val second = order(id = "second", type = OrderType.CARD_ISSUE_VIRTUAL_RAIN_KYC, status = OrderStatus.PROCESSING)
        coEvery {
            orderRepository.findOrders(
                userWalletId = userWalletId,
                types = RESTORED_ORDER_TYPES,
                statuses = ACTIVE_STATUSES,
            )
        } returns listOf(first, second).right()

        // Act
        val result = useCase(userWalletId)

        // Assert
        assertThat(result.isRight()).isTrue()
        coVerify(exactly = 1) { issueCardRepository.storeIssueOrderId(userWalletId, first.id) }
        coVerify(exactly = 1) { issueCardRepository.storeIssueOrderId(userWalletId, second.id) }
        coVerify(exactly = 1) {
            startTangemPayOrderPollingUseCase(orderInfo(first), userWalletId, any())
        }
        coVerify(exactly = 1) {
            startTangemPayOrderPollingUseCase(orderInfo(second), userWalletId, any())
        }
    }

    @Test
    fun `GIVEN no active orders WHEN invoke THEN nothing is stored or polled`() = runTest {
        // Arrange
        coEvery {
            orderRepository.findOrders(userWalletId, types = RESTORED_ORDER_TYPES, statuses = ACTIVE_STATUSES)
        } returns emptyList<Order>().right()

        // Act
        val result = useCase(userWalletId)

        // Assert
        assertThat(result.isRight()).isTrue()
        coVerify(exactly = 0) { issueCardRepository.storeIssueOrderId(any(), any()) }
        coVerify(exactly = 0) { startTangemPayOrderPollingUseCase(any(), any(), any()) }
    }

    @Test
    fun `GIVEN a terminal order leaks through WHEN invoke THEN it is filtered out`() = runTest {
        // Arrange
        val completed = order(id = "done", type = OrderType.CARD_ISSUE_VIRTUAL_RAIN, status = OrderStatus.COMPLETED)
        coEvery {
            orderRepository.findOrders(userWalletId, types = RESTORED_ORDER_TYPES, statuses = ACTIVE_STATUSES)
        } returns listOf(completed).right()

        // Act
        val result = useCase(userWalletId)

        // Assert
        assertThat(result.isRight()).isTrue()
        coVerify(exactly = 0) { issueCardRepository.storeIssueOrderId(any(), any()) }
        coVerify(exactly = 0) { startTangemPayOrderPollingUseCase(any(), any(), any()) }
    }

    @Test
    fun `GIVEN findOrders fails WHEN invoke THEN returns Unspecified and stores nothing`() = runTest {
        // Arrange
        coEvery {
            orderRepository.findOrders(userWalletId, types = RESTORED_ORDER_TYPES, statuses = ACTIVE_STATUSES)
        } returns VisaApiError.Unspecified.left()

        // Act
        val result = useCase(userWalletId)

        // Assert
        assertThat(result.leftOrNull()).isEqualTo(VisaApiError.Unspecified)
        coVerify(exactly = 0) { issueCardRepository.storeIssueOrderId(any(), any()) }
        coVerify(exactly = 0) { startTangemPayOrderPollingUseCase(any(), any(), any()) }
    }

    @Test
    fun `GIVEN a foreign type leaks through the backend filter WHEN invoke THEN it is dropped`() = runTest {
        // Arrange
        val foreign = order(id = "withdraw", type = OrderType.WITHDRAW, status = OrderStatus.PROCESSING)
        stubFindOrders(foreign)

        // Act
        val result = useCase(userWalletId)

        // Assert
        assertThat(result.isRight()).isTrue()
        coVerify(exactly = 0) { issueCardRepository.storeIssueOrderId(any(), any()) }
        coVerify(exactly = 0) { startTangemPayOrderPollingUseCase(any(), any(), any()) }
    }

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN an active order of a restored type WHEN invoke THEN it is polled and stored only when issuing`(
        model: RestoredTypeModel,
    ) = runTest {
        // Arrange
        val restored = order(id = "restored", type = model.type, status = OrderStatus.PROCESSING)
        stubFindOrders(restored)

        // Act
        val result = useCase(userWalletId)

        // Assert
        assertThat(result.isRight()).isTrue()
        coVerify(exactly = if (model.isStoredAsIssueOrder) 1 else 0) {
            issueCardRepository.storeIssueOrderId(userWalletId, restored.id)
        }
        coVerify(exactly = 1) {
            startTangemPayOrderPollingUseCase(orderInfo(restored), userWalletId, any())
        }
    }

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN a restored order WHEN it turns terminal THEN its id is forgotten only when issuing`(
        model: RestoredTypeModel,
    ) = runTest {
        // Arrange
        val restored = order(id = "restored", type = model.type, status = OrderStatus.PROCESSING)
        stubFindOrders(restored)
        val onOrderStateChange = captureOnOrderStateChange()

        // Act
        useCase(userWalletId)
        onOrderStateChange.captured.invoke(TangemPayOrderInfo(restored.id, OrderStatus.COMPLETED))

        // Assert
        coVerify(exactly = if (model.isStoredAsIssueOrder) 1 else 0) {
            issueCardRepository.removeIssueOrderId(userWalletId, restored.id)
        }
    }

    private fun orderInfo(order: Order) = TangemPayOrderInfo(
        orderId = order.id,
        orderStatus = order.status,
        orderStep = order.step,
        orderType = order.type,
    )

    private fun stubFindOrders(vararg orders: Order) {
        coEvery {
            orderRepository.findOrders(userWalletId, types = RESTORED_ORDER_TYPES, statuses = ACTIVE_STATUSES)
        } returns orders.toList().right()
    }

    private fun captureOnOrderStateChange(): CapturingSlot<suspend (TangemPayOrderInfo) -> Unit> {
        val slot = slot<suspend (TangemPayOrderInfo) -> Unit>()
        coEvery {
            startTangemPayOrderPollingUseCase(
                order = any(),
                userWalletId = userWalletId,
                onOrderStateChange = capture(slot),
                timeout = any(),
            )
        } returns true
        return slot
    }

    private fun order(id: String, type: OrderType, status: OrderStatus): Order = Order(
        id = id,
        customerId = "customer",
        type = type,
        status = status,
        step = OrderStep.UNKNOWN,
        stepChangeCode = null,
        productInstanceId = null,
        paymentAccountId = null,
        cardId = null,
        toTariffPlanId = null,
        withdrawTxHash = null,
        createdAt = null,
        updatedAt = null,
    )

    private fun provideTestModels() = listOf(
        RestoredTypeModel(type = OrderType.CARD_ISSUE_VIRTUAL_RAIN, isStoredAsIssueOrder = true),
        RestoredTypeModel(type = OrderType.CARD_ISSUE_VIRTUAL_RAIN_KYC, isStoredAsIssueOrder = true),
        RestoredTypeModel(type = OrderType.CARD_ISSUE_VIRTUAL_RAIN_KYC_V2, isStoredAsIssueOrder = true),
        RestoredTypeModel(type = OrderType.CARD_ISSUE_PLASTIC_RAIN, isStoredAsIssueOrder = true),
        RestoredTypeModel(type = OrderType.CARD_REISSUE_PLASTIC_RAIN, isStoredAsIssueOrder = false),
        RestoredTypeModel(type = OrderType.CARD_ACTIVATION_PLASTIC_RAIN, isStoredAsIssueOrder = false),
    )

    internal data class RestoredTypeModel(val type: OrderType, val isStoredAsIssueOrder: Boolean) {
        override fun toString(): String = "$type -> stored as issue order: $isStoredAsIssueOrder"
    }

    private companion object {
        val RESTORED_ORDER_TYPES = setOf(
            OrderType.CARD_ISSUE_VIRTUAL_RAIN,
            OrderType.CARD_ISSUE_VIRTUAL_RAIN_KYC,
            OrderType.CARD_ISSUE_VIRTUAL_RAIN_KYC_V2,
            OrderType.CARD_ISSUE_PLASTIC_RAIN,
            OrderType.CARD_REISSUE_PLASTIC_RAIN,
            OrderType.CARD_ACTIVATION_PLASTIC_RAIN,
        )
        val ACTIVE_STATUSES = OrderStatus.activeStatuses
    }
}