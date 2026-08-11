package com.tangem.domain.pay.usecase

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.model.Offer
import com.tangem.domain.pay.model.Order
import com.tangem.domain.pay.model.OrderStatus
import com.tangem.domain.pay.model.OrderStep
import com.tangem.domain.pay.model.OrderType
import com.tangem.domain.pay.model.PlasticCardOrder
import com.tangem.domain.pay.model.ShippingAddress
import com.tangem.domain.pay.model.TangemPayOrderInfo
import com.tangem.domain.pay.repository.CustomerOffersRepository
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
import java.math.BigDecimal
import java.util.Currency

internal class IssuePlasticCardUseCaseTest {

    private val offersRepository: CustomerOffersRepository = mockk()
    private val orderRepository: CustomerOrderRepository = mockk()
    private val startTangemPayOrderPollingUseCase: StartTangemPayOrderPollingUseCase = mockk(relaxed = true)
    private val useCase = IssuePlasticCardUseCase(
        customerOffersRepository = offersRepository,
        customerOrderRepository = orderRepository,
        startTangemPayOrderPollingUseCase = startTangemPayOrderPollingUseCase,
        appCoroutineScope = TestAppCoroutineScope(),
    )

    @BeforeEach
    fun resetMocks() {
        clearMocks(offersRepository, orderRepository, startTangemPayOrderPollingUseCase)
    }

    @Test
    fun `GIVEN no plastic offer WHEN invoked THEN returns CardIssueOfferNotAvailable and creates no order`() = runTest {
        // Arrange
        coEvery { offersRepository.getOffers(USER_WALLET_ID) } returns listOf(virtualOffer()).right()

        // Act
        val result = useCase(
            userWalletId = USER_WALLET_ID,
            plasticCardOrder = plasticCardOrder(),
            idempotencyKey = IDEMPOTENCY_KEY,
        )

        // Assert
        assertThat(result.leftOrNull()).isEqualTo(VisaApiError.CardIssueOfferNotAvailable)
        coVerify(exactly = 0) { orderRepository.findOrders(any(), any(), any()) }
        coVerify(exactly = 0) { orderRepository.createPlasticIssueOrder(any(), any(), any(), any()) }
    }

    @Test
    fun `GIVEN a plastic offer WHEN no active order THEN creates the order with the offer id and spec`() = runTest {
        // Arrange
        givenNoActiveOrders()
        val created = order(id = "created", status = OrderStatus.PROCESSING)
        coEvery {
            orderRepository.createPlasticIssueOrder(
                userWalletId = USER_WALLET_ID,
                specificationName = SPEC_NAME,
                order = plasticCardOrder(),
                idempotencyKey = IDEMPOTENCY_KEY,
            )
        } returns created.right()

        // Act
        val result = useCase(
            userWalletId = USER_WALLET_ID,
            plasticCardOrder = plasticCardOrder(),
            idempotencyKey = IDEMPOTENCY_KEY,
        )

        // Assert
        assertThat(result).isEqualTo(Unit.right())
        coVerify(exactly = 1) {
            startTangemPayOrderPollingUseCase(
                order = TangemPayOrderInfo.fromOrder(created),
                userWalletId = USER_WALLET_ID,
            )
        }
    }

    @Test
    fun `GIVEN an active plastic order WHEN invoked THEN raises CardIssueActiveOrderExists`() = runTest {
        // Arrange
        val existing = order(id = "existing", status = OrderStatus.PROCESSING)
        coEvery { offersRepository.getOffers(USER_WALLET_ID) } returns listOf(plasticOffer()).right()
        coEvery {
            orderRepository.findOrders(
                USER_WALLET_ID,
                types = setOf(OrderType.CARD_ISSUE_PLASTIC_RAIN),
                statuses = setOf(OrderStatus.NEW, OrderStatus.PROCESSING),
            )
        } returns listOf(existing).right()

        // Act
        val result = useCase(
            userWalletId = USER_WALLET_ID,
            plasticCardOrder = plasticCardOrder(),
            idempotencyKey = IDEMPOTENCY_KEY,
        )

        // Assert
        assertThat(result.leftOrNull()).isEqualTo(VisaApiError.CardIssueActiveOrderExists)
        coVerify(exactly = 0) { orderRepository.createPlasticIssueOrder(any(), any(), any(), any()) }
        coVerify(exactly = 0) { startTangemPayOrderPollingUseCase(any(), any(), any(), any()) }
    }

    @Test
    fun `GIVEN offers request fails WHEN invoked THEN the typed error is propagated`() = runTest {
        // Arrange
        coEvery { offersRepository.getOffers(USER_WALLET_ID) } returns VisaApiError.ServerUnavailable.left()

        // Act
        val result = useCase(
            userWalletId = USER_WALLET_ID,
            plasticCardOrder = plasticCardOrder(),
            idempotencyKey = IDEMPOTENCY_KEY,
        )

        // Assert
        assertThat(result.leftOrNull()).isEqualTo(VisaApiError.ServerUnavailable)
        coVerify(exactly = 0) { orderRepository.createPlasticIssueOrder(any(), any(), any(), any()) }
    }

    @Test
    fun `GIVEN findOrders fails WHEN invoked THEN the typed error is propagated`() = runTest {
        // Arrange
        coEvery { offersRepository.getOffers(USER_WALLET_ID) } returns listOf(plasticOffer()).right()
        coEvery { orderRepository.findOrders(USER_WALLET_ID, any(), any()) } returns
            VisaApiError.ServerUnavailable.left()

        // Act
        val result = useCase(
            userWalletId = USER_WALLET_ID,
            plasticCardOrder = plasticCardOrder(),
            idempotencyKey = IDEMPOTENCY_KEY,
        )

        // Assert
        assertThat(result.leftOrNull()).isEqualTo(VisaApiError.ServerUnavailable)
        coVerify(exactly = 0) { orderRepository.createPlasticIssueOrder(any(), any(), any(), any()) }
    }

    @Test
    fun `GIVEN a terminal plastic order WHEN invoked THEN creates a new order`() = runTest {
        // Arrange
        coEvery { offersRepository.getOffers(USER_WALLET_ID) } returns listOf(plasticOffer()).right()
        coEvery { orderRepository.findOrders(USER_WALLET_ID, any(), any()) } returns
            listOf(order(id = "done", status = OrderStatus.COMPLETED)).right()
        coEvery {
            orderRepository.createPlasticIssueOrder(any(), any(), any(), any())
        } returns order(id = "created", status = OrderStatus.NEW).right()

        // Act
        val result = useCase(
            userWalletId = USER_WALLET_ID,
            plasticCardOrder = plasticCardOrder(),
            idempotencyKey = IDEMPOTENCY_KEY,
        )

        // Assert
        assertThat(result).isEqualTo(Unit.right())
        coVerify(exactly = 1) { orderRepository.createPlasticIssueOrder(any(), any(), any(), any()) }
    }

    @Test
    fun `GIVEN the backend rejects the order WHEN invoked THEN the typed error is propagated`() = runTest {
        // Arrange
        givenNoActiveOrders()
        coEvery {
            orderRepository.createPlasticIssueOrder(any(), any(), any(), any())
        } returns VisaApiError.CardIssueInvalidShippingAddress.left()

        // Act
        val result = useCase(
            userWalletId = USER_WALLET_ID,
            plasticCardOrder = plasticCardOrder(),
            idempotencyKey = IDEMPOTENCY_KEY,
        )

        // Assert
        assertThat(result.leftOrNull()).isEqualTo(VisaApiError.CardIssueInvalidShippingAddress)
        coVerify(exactly = 0) { startTangemPayOrderPollingUseCase(any(), any(), any(), any()) }
    }

    @Test
    fun `GIVEN offers request throws WHEN invoked THEN collapses to Unspecified`() = runTest {
        // Arrange
        coEvery { offersRepository.getOffers(USER_WALLET_ID) } throws IllegalStateException("boom")

        // Act
        val result = useCase(
            userWalletId = USER_WALLET_ID,
            plasticCardOrder = plasticCardOrder(),
            idempotencyKey = IDEMPOTENCY_KEY,
        )

        // Assert
        assertThat(result.leftOrNull()).isEqualTo(VisaApiError.Unspecified)
    }

    @Test
    fun `GIVEN order creation throws WHEN invoked THEN collapses to Unspecified`() = runTest {
        // Arrange
        givenNoActiveOrders()
        coEvery {
            orderRepository.createPlasticIssueOrder(any(), any(), any(), any())
        } throws IllegalStateException("Can not find customer address")

        // Act
        val result = useCase(
            userWalletId = USER_WALLET_ID,
            plasticCardOrder = plasticCardOrder(),
            idempotencyKey = IDEMPOTENCY_KEY,
        )

        // Assert
        assertThat(result.leftOrNull()).isEqualTo(VisaApiError.Unspecified)
    }

    private fun givenNoActiveOrders() {
        coEvery { offersRepository.getOffers(USER_WALLET_ID) } returns listOf(plasticOffer()).right()
        coEvery { orderRepository.findOrders(USER_WALLET_ID, any(), any()) } returns emptyList<Order>().right()
    }

    private fun plasticOffer() = offer(type = Offer.Type.TANGEM_PAY_PLASTIC_VISA)

    private fun virtualOffer() = offer(type = Offer.Type.CARD_ISSUE_VIRTUAL_RAIN)

    private fun offer(type: Offer.Type) = Offer(
        type = type,
        fee = Offer.Fee(amount = BigDecimal("5.00"), currency = Currency.getInstance("USD")),
        data = Offer.Data(specificationName = SPEC_NAME, orderType = OrderType.CARD_ISSUE_PLASTIC_RAIN),
    )

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

    private fun order(id: String, status: OrderStatus) = Order(
        id = id,
        customerId = "customer",
        type = OrderType.CARD_ISSUE_PLASTIC_RAIN,
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

    private companion object {
        val USER_WALLET_ID = UserWalletId("1234567890ABCDEF")
        const val SPEC_NAME = "SP_000010"
        const val IDEMPOTENCY_KEY = "6f1c9e2a-0b3d-4c5e-8a7b-9d0e1f2a3b4c"
    }
}