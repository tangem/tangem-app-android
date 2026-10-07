package com.tangem.domain.pay.usecase

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.model.Order
import com.tangem.domain.pay.model.OrderStatus
import com.tangem.domain.pay.model.OrderStep
import com.tangem.domain.pay.model.OrderType
import com.tangem.domain.pay.repository.CustomerOrderRepository
import com.tangem.domain.visa.error.VisaApiError
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class FindActiveOrdersUseCaseTest {

    private val customerOrderRepository: CustomerOrderRepository = mockk()
    private val useCase = FindActiveOrdersUseCase(customerOrderRepository = customerOrderRepository)

    @BeforeEach
    fun resetMocks() {
        clearMocks(customerOrderRepository)
    }

    @Test
    fun `GIVEN types WHEN invoke THEN orders requested with these types and active statuses`() = runTest {
        // Arrange
        val types = setOf(OrderType.ACCOUNT_ISSUE_BANKING_RAIN, OrderType.CARD_ISSUE_PLASTIC_RAIN)
        coEvery { customerOrderRepository.findOrders(any(), any(), any()) } returns emptyList<Order>().right()

        // Act
        useCase(userWalletId = USER_WALLET_ID, types = types)

        // Assert
        coVerify(exactly = 1) {
            customerOrderRepository.findOrders(
                userWalletId = USER_WALLET_ID,
                types = types,
                statuses = OrderStatus.activeStatuses,
            )
        }
    }

    @Test
    fun `GIVEN no types WHEN invoke THEN orders requested without type filter`() = runTest {
        // Arrange
        coEvery { customerOrderRepository.findOrders(any(), any(), any()) } returns emptyList<Order>().right()

        // Act
        useCase(userWalletId = USER_WALLET_ID)

        // Assert
        coVerify(exactly = 1) {
            customerOrderRepository.findOrders(
                userWalletId = USER_WALLET_ID,
                types = emptySet(),
                statuses = OrderStatus.activeStatuses,
            )
        }
    }

    @Test
    fun `GIVEN repository returns orders WHEN invoke THEN orders returned as is`() = runTest {
        // Arrange
        val orders = listOf(
            createOrder(id = "order-1", type = OrderType.ACCOUNT_ISSUE_BANKING_RAIN),
            createOrder(id = "order-2", type = OrderType.CARD_ISSUE_PLASTIC_RAIN),
        )
        coEvery { customerOrderRepository.findOrders(any(), any(), any()) } returns orders.right()

        // Act
        val actual = useCase(userWalletId = USER_WALLET_ID)

        // Assert
        assertThat(actual).isEqualTo(orders.right())
    }

    @Test
    fun `GIVEN repository fails WHEN invoke THEN error passed through`() = runTest {
        // Arrange
        coEvery { customerOrderRepository.findOrders(any(), any(), any()) } returns VisaApiError.Unspecified.left()

        // Act
        val actual = useCase(userWalletId = USER_WALLET_ID)

        // Assert
        assertThat(actual).isEqualTo(VisaApiError.Unspecified.left())
    }

    private fun createOrder(id: String, type: OrderType) = Order(
        id = id,
        customerId = null,
        type = type,
        status = OrderStatus.PROCESSING,
        step = OrderStep.STARTING,
        stepChangeCode = null,
        productInstanceId = null,
        paymentAccountId = null,
        cardId = null,
        toTariffPlanId = null,
        withdrawTxHash = null,
        createdAt = null,
        updatedAt = null,
    )

    private companion object {
        val USER_WALLET_ID = UserWalletId("011")
    }
}