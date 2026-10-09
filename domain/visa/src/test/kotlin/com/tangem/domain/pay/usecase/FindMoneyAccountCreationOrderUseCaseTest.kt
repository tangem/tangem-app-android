package com.tangem.domain.pay.usecase

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.model.Order
import com.tangem.domain.pay.model.OrderStatus
import com.tangem.domain.pay.model.OrderStep
import com.tangem.domain.pay.model.OrderType
import com.tangem.domain.visa.error.VisaApiError
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class FindMoneyAccountCreationOrderUseCaseTest {

    private val findOrdersUseCase: FindOrdersUseCase = mockk()
    private val useCase = FindMoneyAccountCreationOrderUseCase(findOrdersUseCase = findOrdersUseCase)

    @BeforeEach
    fun resetMocks() {
        clearMocks(findOrdersUseCase)
    }

    @Test
    fun `GIVEN invoke WHEN orders requested THEN filtered by type and non-cancelled statuses`() = runTest {
        // Arrange
        stubOrders()

        // Act
        useCase(USER_WALLET_ID)

        // Assert
        coVerify(exactly = 1) {
            findOrdersUseCase(
                userWalletId = USER_WALLET_ID,
                types = setOf(OrderType.ACCOUNT_ISSUE_BANKING_RAIN),
                statuses = setOf(OrderStatus.NEW, OrderStatus.PROCESSING, OrderStatus.COMPLETED),
            )
        }
    }

    @Test
    fun `GIVEN several money account orders WHEN invoke THEN the most recently updated returned`() = runTest {
        // Arrange
        val older = createOrder(id = "older", updatedAt = "2026-09-01T00:00:00Z")
        val newer = createOrder(id = "newer", updatedAt = "2026-09-02T00:00:00Z")
        stubOrders(older, newer)

        // Act
        val actual = useCase(USER_WALLET_ID)

        // Assert
        assertThat(actual).isEqualTo(newer.right())
    }

    @Test
    fun `GIVEN no orders WHEN invoke THEN null returned`() = runTest {
        // Arrange
        stubOrders()

        // Act
        val actual = useCase(USER_WALLET_ID)

        // Assert
        assertThat(actual).isEqualTo(null.right())
    }

    @Test
    fun `GIVEN completed order WHEN invoke THEN it is returned`() = runTest {
        // Arrange
        val completed = createOrder(id = "completed", status = OrderStatus.COMPLETED)
        stubOrders(completed)

        // Act
        val actual = useCase(USER_WALLET_ID)

        // Assert
        assertThat(actual).isEqualTo(completed.right())
    }

    @Test
    fun `GIVEN find orders fails WHEN invoke THEN error passed through`() = runTest {
        // Arrange
        coEvery { findOrdersUseCase(any(), any(), any()) } returns VisaApiError.Unspecified.left()

        // Act
        val actual = useCase(USER_WALLET_ID)

        // Assert
        assertThat(actual).isEqualTo(VisaApiError.Unspecified.left())
    }

    private fun stubOrders(vararg orders: Order) {
        coEvery { findOrdersUseCase(any(), any(), any()) } returns orders.toList().right()
    }

    private fun createOrder(
        id: String,
        type: OrderType = OrderType.ACCOUNT_ISSUE_BANKING_RAIN,
        status: OrderStatus = OrderStatus.PROCESSING,
        updatedAt: String? = null,
    ) = Order(
        id = id,
        customerId = null,
        type = type,
        status = status,
        step = OrderStep.STARTING,
        stepChangeCode = null,
        productInstanceId = null,
        paymentAccountId = null,
        cardId = null,
        toTariffPlanId = null,
        withdrawTxHash = null,
        createdAt = null,
        updatedAt = updatedAt,
    )

    private companion object {
        val USER_WALLET_ID = UserWalletId("011")
    }
}