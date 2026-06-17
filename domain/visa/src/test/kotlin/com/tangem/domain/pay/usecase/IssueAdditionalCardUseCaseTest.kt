package com.tangem.domain.pay.usecase

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.model.Offer
import com.tangem.domain.pay.model.Order
import com.tangem.domain.pay.model.OrderStatus
import com.tangem.domain.pay.model.OrderType
import com.tangem.domain.pay.repository.CustomerOffersRepository
import com.tangem.domain.pay.repository.CustomerOrderRepository
import com.tangem.domain.pay.repository.TangemPayIssueCardRepository
import com.tangem.domain.visa.error.VisaApiError
import com.tangem.test.core.TestAppCoroutineScope
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.util.Currency

internal class IssueAdditionalCardUseCaseTest {

    private val offersRepository: CustomerOffersRepository = mockk()
    private val orderRepository: CustomerOrderRepository = mockk()
    private val issueCardRepository: TangemPayIssueCardRepository = mockk(relaxed = true)
    private val startTangemPayOrderPollingUseCase: StartTangemPayOrderPollingUseCase = mockk(relaxed = true)
    private val useCase = IssueAdditionalCardUseCase(
        customerOffersRepository = offersRepository,
        customerOrderRepository = orderRepository,
        issueCardRepository = issueCardRepository,
        startTangemPayOrderPollingUseCase = startTangemPayOrderPollingUseCase,
        appCoroutineScope = TestAppCoroutineScope(),
    )
    private val userWalletId = UserWalletId("1234567890ABCDEF")
    private val spec = "SP_000004"

    private val offer = Offer(
        type = Offer.Type.CARD_ISSUE_VIRTUAL_RAIN,
        fee = Offer.Fee(amount = BigDecimal("1.00"), currency = Currency.getInstance("USD")),
        data = Offer.Data(specificationName = spec, orderType = OrderType.CARD_ISSUE_ADDITIONAL),
    )

    @Test
    fun `WHEN no additional-card offer is available THEN returns Unspecified error`() = runTest {
        coEvery { offersRepository.getOffers(userWalletId) } returns emptyList<Offer>().right()

        val result = useCase(userWalletId)

        assertThat(result.leftOrNull()).isEqualTo(VisaApiError.Unspecified)
        coVerify(exactly = 0) { orderRepository.findOrders(any(), any(), any()) }
        coVerify(exactly = 0) { orderRepository.createOrder(any(), any(), any(), any()) }
    }

    @Test
    fun `WHEN active issue order exists THEN reuses it without calling createOrder`() = runTest {
        val existing = order(
            id = "existing",
            type = OrderType.CARD_ISSUE_ADDITIONAL,
            status = OrderStatus.PROCESSING,
        )
        coEvery { offersRepository.getOffers(userWalletId) } returns listOf(offer).right()
        coEvery {
            orderRepository.findOrders(
                userWalletId,
                types = setOf(OrderType.CARD_ISSUE_ADDITIONAL),
                statuses = emptySet(),
            )
        } returns listOf(existing).right()

        val result = useCase(userWalletId)

        assertThat(result.isRight()).isTrue()
        coVerify(exactly = 0) { orderRepository.createOrder(any(), any(), any(), any()) }
        coVerify(exactly = 1) { issueCardRepository.storeIssueOrderId(userWalletId, existing.id) }
    }

    @Test
    fun `WHEN backend returns insufficient balance THEN propagates CardIssueInsufficientBalance`() = runTest {
        coEvery { offersRepository.getOffers(userWalletId) } returns listOf(offer).right()
        coEvery {
            orderRepository.findOrders(
                userWalletId,
                types = setOf(OrderType.CARD_ISSUE_ADDITIONAL),
                statuses = emptySet(),
            )
        } returns emptyList<Order>().right()
        coEvery {
            orderRepository.createOrder(
                userWalletId = userWalletId,
                type = OrderType.CARD_ISSUE_ADDITIONAL,
                specificationName = spec,
                idempotencyKey = any(),
            )
        } returns VisaApiError.CardIssueInsufficientBalance.left()

        val result = useCase(userWalletId)

        assertThat(result.leftOrNull()).isEqualTo(VisaApiError.CardIssueInsufficientBalance)
    }

    @Test
    fun `WHEN no active order AND createOrder succeeds THEN returns the new order`() = runTest {
        coEvery { offersRepository.getOffers(userWalletId) } returns listOf(offer).right()
        coEvery {
            orderRepository.findOrders(
                userWalletId,
                types = setOf(OrderType.CARD_ISSUE_ADDITIONAL),
                statuses = emptySet(),
            )
        } returns emptyList<Order>().right()
        val newOrder = order(
            id = "new",
            type = OrderType.CARD_ISSUE_ADDITIONAL,
            status = OrderStatus.NEW,
        )
        coEvery {
            orderRepository.createOrder(
                userWalletId = userWalletId,
                type = OrderType.CARD_ISSUE_ADDITIONAL,
                specificationName = spec,
                idempotencyKey = any(),
            )
        } returns newOrder.right()

        val result = useCase(userWalletId)

        assertThat(result.isRight()).isTrue()
        coVerify(exactly = 1) { issueCardRepository.storeIssueOrderId(userWalletId, newOrder.id) }
    }

    private fun order(id: String, type: OrderType, status: OrderStatus): Order = Order(
        id = id,
        customerId = "customer",
        type = type,
        status = status,
        step = null,
        stepChangeCode = null,
        productInstanceId = null,
        paymentAccountId = null,
        cardId = null,
        withdrawTxHash = null,
        createdAt = null,
        updatedAt = null,
    )
}