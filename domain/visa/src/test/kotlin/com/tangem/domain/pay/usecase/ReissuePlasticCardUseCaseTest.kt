package com.tangem.domain.pay.usecase

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.model.Order
import com.tangem.domain.pay.model.OrderStatus
import com.tangem.domain.pay.model.OrderStep
import com.tangem.domain.pay.model.OrderType
import com.tangem.domain.pay.model.PlasticCardOrder
import com.tangem.domain.pay.model.ShippingAddress
import com.tangem.domain.pay.model.TangemPayOrderInfo
import com.tangem.domain.pay.repository.CustomerOrderRepository
import com.tangem.domain.visa.error.VisaApiError
import com.tangem.test.core.TestAppCoroutineScope
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class ReissuePlasticCardUseCaseTest {

    private val orderRepository: CustomerOrderRepository = mockk()
    private val startTangemPayOrderPollingUseCase: StartTangemPayOrderPollingUseCase = mockk(relaxed = true)
    private val useCase = ReissuePlasticCardUseCase(
        customerOrderRepository = orderRepository,
        startTangemPayOrderPollingUseCase = startTangemPayOrderPollingUseCase,
        appCoroutineScope = TestAppCoroutineScope(),
    )

    @BeforeEach
    fun resetMocks() {
        clearMocks(orderRepository, startTangemPayOrderPollingUseCase)
    }

    @Test
    fun `GIVEN no active reissue order WHEN invoked THEN creates the order with the source card and spec`() = runTest {
        // Arrange
        givenNoActiveOrders()
        val created = order(id = "created", status = OrderStatus.PROCESSING)
        coEvery {
            orderRepository.createPlasticReissueOrder(
                userWalletId = USER_WALLET_ID,
                sourceProductInstanceId = SOURCE_PRODUCT_INSTANCE_ID,
                order = plasticCardOrder(),
                idempotencyKey = IDEMPOTENCY_KEY,
            )
        } returns created.right()

        // Act
        val result = invokeUseCase()

        // Assert
        assertThat(result).isEqualTo(created.right())
        coVerify(exactly = 1) {
            startTangemPayOrderPollingUseCase(
                order = TangemPayOrderInfo.fromOrder(created),
                userWalletId = USER_WALLET_ID,
            )
        }
    }

    @Test
    fun `GIVEN a reissue order is created WHEN invoked THEN it is filtered by the plastic reissue type`() = runTest {
        // Arrange
        givenNoActiveOrders()
        coEvery {
            orderRepository.createPlasticReissueOrder(any(), any(), any(), any())
        } returns order(id = "created", status = OrderStatus.NEW).right()

        // Act
        invokeUseCase()

        // Assert
        coVerify(exactly = 1) {
            orderRepository.findOrders(
                userWalletId = USER_WALLET_ID,
                types = setOf(OrderType.CARD_REISSUE_PLASTIC_RAIN),
                statuses = setOf(OrderStatus.NEW, OrderStatus.PROCESSING),
            )
        }
    }

    @Test
    fun `GIVEN an active reissue order for the same card WHEN invoked THEN raises CardReissuePlasticActiveOrderExists`() =
        runTest {
            // Arrange
                coEvery { orderRepository.findOrders(USER_WALLET_ID, any(), any()) } returns
                listOf(
                    order(
                        id = "existing",
                        status = OrderStatus.PROCESSING,
                        productInstanceId = SOURCE_PRODUCT_INSTANCE_ID,
                    ),
                ).right()

            // Act
            val result = invokeUseCase()

            // Assert
            assertThat(result.leftOrNull()).isEqualTo(VisaApiError.CardReissuePlasticActiveOrderExists)
            coVerify(exactly = 0) { orderRepository.createPlasticReissueOrder(any(), any(), any(), any()) }
            coVerify(exactly = 0) { startTangemPayOrderPollingUseCase(any(), any(), any(), any()) }
        }

    @Test
    fun `GIVEN an active reissue order for another card WHEN invoked THEN the order is still created`() = runTest {
        // Arrange
        coEvery { orderRepository.findOrders(USER_WALLET_ID, any(), any()) } returns
            listOf(
                order(id = "other", status = OrderStatus.PROCESSING, productInstanceId = "pi_other_0002"),
            ).right()
        val created = order(id = "created", status = OrderStatus.NEW)
        coEvery {
            orderRepository.createPlasticReissueOrder(any(), any(), any(), any())
        } returns created.right()

        // Act
        val result = invokeUseCase()

        // Assert
        assertThat(result).isEqualTo(created.right())
        coVerify(exactly = 1) { orderRepository.createPlasticReissueOrder(any(), any(), any(), any()) }
    }

    @Test
    fun `GIVEN a terminal reissue order for the same card WHEN invoked THEN creates a new order`() = runTest {
        // Arrange
        coEvery { orderRepository.findOrders(USER_WALLET_ID, any(), any()) } returns
            listOf(
                order(
                    id = "done",
                    status = OrderStatus.COMPLETED,
                    productInstanceId = SOURCE_PRODUCT_INSTANCE_ID,
                ),
            ).right()
        val created = order(id = "created", status = OrderStatus.NEW)
        coEvery {
            orderRepository.createPlasticReissueOrder(any(), any(), any(), any())
        } returns created.right()

        // Act
        val result = invokeUseCase()

        // Assert
        assertThat(result).isEqualTo(created.right())
        coVerify(exactly = 1) { orderRepository.createPlasticReissueOrder(any(), any(), any(), any()) }
    }

    @Test
    fun `GIVEN findOrders fails WHEN invoked THEN the typed error is propagated`() = runTest {
        // Arrange
        coEvery { orderRepository.findOrders(USER_WALLET_ID, any(), any()) } returns
            VisaApiError.ServerUnavailable.left()

        // Act
        val result = invokeUseCase()

        // Assert
        assertThat(result.leftOrNull()).isEqualTo(VisaApiError.ServerUnavailable)
        coVerify(exactly = 0) { orderRepository.createPlasticReissueOrder(any(), any(), any(), any()) }
    }

    @Test
    fun `GIVEN the backend rejects the order WHEN invoked THEN no polling starts and the error is propagated`() =
        runTest {
            // Arrange
            givenNoActiveOrders()
            coEvery {
                orderRepository.createPlasticReissueOrder(any(), any(), any(), any())
            } returns VisaApiError.CardReissuePlasticInsufficientBalance.left()

            // Act
            val result = invokeUseCase()

            // Assert
            assertThat(result.leftOrNull()).isEqualTo(VisaApiError.CardReissuePlasticInsufficientBalance)
            coVerify(exactly = 0) { startTangemPayOrderPollingUseCase(any(), any(), any(), any()) }
        }

    @Test
    fun `GIVEN order creation throws WHEN invoked THEN collapses to Unspecified`() = runTest {
        // Arrange
        givenNoActiveOrders()
        coEvery {
            orderRepository.createPlasticReissueOrder(any(), any(), any(), any())
        } throws IllegalStateException("Can not find customer address")

        // Act
        val result = invokeUseCase()

        // Assert
        assertThat(result.leftOrNull()).isEqualTo(VisaApiError.Unspecified)
    }

    private suspend fun invokeUseCase() = useCase(
        userWalletId = USER_WALLET_ID,
        sourceProductInstanceId = SOURCE_PRODUCT_INSTANCE_ID,
        plasticCardOrder = plasticCardOrder(),
        idempotencyKey = IDEMPOTENCY_KEY,
    )

    private fun givenNoActiveOrders() {
        coEvery { orderRepository.findOrders(USER_WALLET_ID, any(), any()) } returns emptyList<Order>().right()
    }

    private fun plasticCardOrder() = PlasticCardOrder(
        embossName = "JOHNNY SILVERHAND",
        shippingAddress = ShippingAddress(
            firstName = "Johnny",
            lastName = "Silverhand",
            region = "California",
            city = "Night City",
            line1 = "Crescent st. 24",
            line2 = "Apt. 56",
            postalCode = "90210",
            phone = "+12345678901",
        ),
    )

    private fun order(id: String, status: OrderStatus, productInstanceId: String? = null) = Order(
        id = id,
        customerId = "customer",
        type = OrderType.CARD_REISSUE_PLASTIC_RAIN,
        status = status,
        step = OrderStep.UNKNOWN,
        stepChangeCode = null,
        productInstanceId = productInstanceId,
        paymentAccountId = null,
        cardId = null,
        toTariffPlanId = null,
        withdrawTxHash = null,
        createdAt = null,
        updatedAt = null,
    )

    private companion object {
        val USER_WALLET_ID = UserWalletId("1234567890ABCDEF")
        const val SOURCE_PRODUCT_INSTANCE_ID = "pi_source_0001"
        const val IDEMPOTENCY_KEY = "6f1c9e2a-0b3d-4c5e-8a7b-9d0e1f2a3b4d"
    }
}