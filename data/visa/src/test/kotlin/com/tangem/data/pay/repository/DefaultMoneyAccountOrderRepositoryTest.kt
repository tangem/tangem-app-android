package com.tangem.data.pay.repository

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.core.remote.response.ApiResponse
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.model.Order
import com.tangem.domain.pay.model.OrderStatus
import com.tangem.domain.pay.model.OrderStep
import com.tangem.domain.pay.model.OrderType
import com.tangem.domain.pay.model.TangemPayOrderInfo
import com.tangem.domain.pay.repository.CustomerOrderRepository
import com.tangem.domain.visa.error.VisaApiError
import com.tangem.spend.datasource.pay.TangemPayApi
import com.tangem.spend.datasource.pay.models.request.OrderRequest
import com.tangem.spend.datasource.pay.models.response.OrderResponse
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class DefaultMoneyAccountOrderRepositoryTest {

    private val tangemPayApi: TangemPayApi = mockk()
    private val requestHelper: TangemPayRequestPerformer = mockk()
    private val customerOrderRepository: CustomerOrderRepository = mockk()

    private val repository = DefaultMoneyAccountOrderRepository(
        tangemPayApi = tangemPayApi,
        requestHelper = requestHelper,
        customerOrderRepository = customerOrderRepository,
        dispatcherProvider = TestingCoroutineDispatcherProvider(),
    )

    @BeforeEach
    fun resetMocks() {
        clearMocks(tangemPayApi, requestHelper, customerOrderRepository)
        repository.clearOrder(USER_WALLET_ID)

        coEvery {
            requestHelper.performRequest<OrderResponse>(userWalletId = any(), requestBlock = any())
        } coAnswers {
            val block = secondArg<suspend (String) -> ApiResponse<OrderResponse>>()
            when (val response = block(AUTH_HEADER)) {
                is ApiResponse.Success -> response.data.right()
                is ApiResponse.Error -> VisaApiError.Unspecified.left()
            }
        }
        coEvery { tangemPayApi.createOrder(any(), any(), any()) } returns ApiResponse.Success(createResponse())
    }

    @Test
    fun `GIVEN deposit address and signature WHEN createOrder THEN money account order is requested`() = runTest {
        // Arrange
        val idempotencyKey = slot<String>()
        val body = slot<OrderRequest>()
        coEvery {
            tangemPayApi.createOrder(any(), capture(idempotencyKey), capture(body))
        } returns ApiResponse.Success(createResponse())

        // Act
        repository.createOrder(USER_WALLET_ID, DEPOSIT_ADDRESS, SIGNATURE)

        // Assert
        assertThat(body.captured.data).isEqualTo(
            OrderRequest.BankingAccountIssue(
                depositAddress = DEPOSIT_ADDRESS,
                signature = SIGNATURE,
                specificationName = "SP_000006",
            ),
        )
        assertThat(idempotencyKey.captured).isNotEmpty()
    }

    @Test
    fun `GIVEN a created order WHEN createOrder THEN the order is returned`() = runTest {
        // Act
        val actual = repository.createOrder(USER_WALLET_ID, DEPOSIT_ADDRESS, SIGNATURE)

        // Assert
        assertThat(actual.getOrNull()?.id).isEqualTo(ORDER_ID)
        assertThat(actual.getOrNull()?.type).isEqualTo(OrderType.ACCOUNT_ISSUE_BANKING_RAIN)
        assertThat(actual.getOrNull()?.step).isEqualTo(OrderStep.STARTING)
    }

    @Test
    fun `GIVEN an empty result WHEN createOrder THEN an error is returned`() = runTest {
        // Arrange
        coEvery {
            tangemPayApi.createOrder(any(), any(), any())
        } returns ApiResponse.Success(OrderResponse(result = null))

        // Act
        val actual = repository.createOrder(USER_WALLET_ID, DEPOSIT_ADDRESS, SIGNATURE)

        // Assert
        assertThat(actual).isEqualTo(VisaApiError.UnknownWithoutCode.left())
    }

    @Test
    fun `GIVEN orders of several types WHEN findActiveOrder THEN the latest money account order is returned`() =
        runTest {
            // Arrange
            val expected = createOrder(id = "latest", updatedAt = "2026-09-02T00:00:00Z")
            coEvery { customerOrderRepository.findOrders(USER_WALLET_ID, any(), any()) } returns listOf(
                createOrder(id = "card", type = OrderType.CARD_ISSUE_PLASTIC_RAIN, updatedAt = "2026-09-03T00:00:00Z"),
                createOrder(id = "older", updatedAt = "2026-09-01T00:00:00Z"),
                expected,
            ).right()

            // Act
            val actual = repository.findActiveOrder(USER_WALLET_ID)

            // Assert
            assertThat(actual.getOrNull()).isEqualTo(expected)
        }

    @Test
    fun `GIVEN no money account orders WHEN findActiveOrder THEN nothing is returned`() = runTest {
        // Arrange
        coEvery { customerOrderRepository.findOrders(USER_WALLET_ID, any(), any()) } returns listOf(
            createOrder(type = OrderType.CARD_ISSUE_PLASTIC_RAIN),
        ).right()

        // Act
        val actual = repository.findActiveOrder(USER_WALLET_ID)

        // Assert
        assertThat(actual.getOrNull()).isNull()
    }

    @Test
    fun `GIVEN a stored order WHEN clearOrder THEN it is no longer returned`() {
        // Arrange
        val order = TangemPayOrderInfo(orderId = ORDER_ID, orderStatus = OrderStatus.PROCESSING)
        repository.storeOrder(USER_WALLET_ID, order)

        // Act
        repository.clearOrder(USER_WALLET_ID)

        // Assert
        assertThat(repository.getOrder(USER_WALLET_ID)).isNull()
    }

    @Test
    fun `GIVEN an order stored for a wallet WHEN getOrder THEN other wallets are unaffected`() {
        // Arrange
        val order = TangemPayOrderInfo(orderId = ORDER_ID, orderStatus = OrderStatus.PROCESSING)

        // Act
        repository.storeOrder(USER_WALLET_ID, order)

        // Assert
        assertThat(repository.getOrder(USER_WALLET_ID)).isEqualTo(order)
        assertThat(repository.getOrder(OTHER_USER_WALLET_ID)).isNull()
    }

    private fun createOrder(
        id: String = ORDER_ID,
        type: OrderType = OrderType.ACCOUNT_ISSUE_BANKING_RAIN,
        updatedAt: String? = null,
    ) = Order(
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
        updatedAt = updatedAt,
    )

    private fun createResponse() = OrderResponse(
        result = OrderResponse.Result(
            id = ORDER_ID,
            customerId = null,
            type = "ACCOUNT_ISSUE_BANKING_RAIN",
            status = OrderResponse.Result.Status.NEW,
            step = "STARTING",
            data = OrderResponse.Result.Data(
                type = "ACCOUNT_ISSUE_BANKING_RAIN",
                specificationName = "SP_000006",
                customerWalletAddress = null,
                embossName = null,
                productInstanceId = null,
                paymentAccountId = null,
                targetTariffPlanId = null,
                transactionHash = null,
            ),
            stepChangeCode = null,
            createdAt = null,
            updatedAt = null,
        ),
    )

    private companion object {
        val USER_WALLET_ID = UserWalletId("011")
        val OTHER_USER_WALLET_ID = UserWalletId("012")
        const val AUTH_HEADER = "auth-header"
        const val ORDER_ID = "order-1"
        const val DEPOSIT_ADDRESS = "0xDEPOSIT"
        const val SIGNATURE = "0xSIG"
    }
}