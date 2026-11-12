package com.tangem.data.tangempay.repository

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.core.remote.response.ApiResponse
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.tangempay.error.TangemPayApiError
import com.tangem.domain.tangempay.model.CustomerOrderRequest
import com.tangem.domain.tangempay.model.OrderDetails
import com.tangem.domain.tangempay.model.OrderStep
import com.tangem.domain.tangempay.model.OrderType
import com.tangem.domain.tangempay.model.TangemPayOrder
import com.tangem.spend.datasource.tangempay.TangemPayOrderApi
import com.tangem.spend.datasource.tangempay.models.request.OrderRequest
import com.tangem.spend.datasource.tangempay.models.response.OrderDataResponse
import com.tangem.spend.datasource.tangempay.models.response.TangemPayOrderResponse
import com.tangem.spend.datasource.tangempay.models.response.TangemPayOrdersResponse
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

internal class DefaultTangemPayOrderRepositoryTest {

    private val orderApi: TangemPayOrderApi = mockk()
    private val requestHelper: TangemPayRequestPerformer = mockk()

    private val repository = DefaultTangemPayOrderRepository(orderApi = orderApi, requestHelper = requestHelper)

    @BeforeEach
    fun setUp() {
        clearMocks(orderApi, requestHelper)
        coEvery {
            requestHelper.performRequest<Any>(userWalletId = any(), requestBlock = any())
        } coAnswers {
            val block = secondArg<suspend (String) -> ApiResponse<Any>>()
            when (val response = block(AUTH_HEADER)) {
                is ApiResponse.Success -> response.data.right()
                is ApiResponse.Error -> TangemPayApiError.Unspecified.left()
            }
        }
        coEvery { requestHelper.getCustomerWalletAddress(USER_WALLET_ID) } returns WALLET_ADDRESS
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class GetOrder {

        @Test
        fun `GIVEN existing order WHEN getOrder THEN the typed order is returned`() = runTest {
            // Arrange
            coEvery { orderApi.getOrder(AUTH_HEADER, ORDER_ID) } returns ApiResponse.Success(orderResponse())

            // Act
            val result = repository.getOrder(USER_WALLET_ID, ORDER_ID)

            // Assert
            assertThat(result.getOrNull()).isEqualTo(expectedOrder())
        }

        @Test
        fun `GIVEN backend returns not found WHEN getOrder THEN OrderNotFound is returned`() = runTest {
            // Arrange
            coEvery {
                requestHelper.performRequest<Any>(userWalletId = any(), requestBlock = any())
            } returns TangemPayApiError.NotFound.left()

            // Act
            val result = repository.getOrder(USER_WALLET_ID, ORDER_ID)

            // Assert
            assertThat(result.leftOrNull()).isEqualTo(TangemPayApiError.OrderNotFound)
        }

        @Test
        fun `GIVEN backend error WHEN getOrder THEN the error is returned as is`() = runTest {
            // Arrange
            coEvery {
                requestHelper.performRequest<Any>(userWalletId = any(), requestBlock = any())
            } returns TangemPayApiError.Unspecified.left()

            // Act
            val result = repository.getOrder(USER_WALLET_ID, ORDER_ID)

            // Assert
            assertThat(result.leftOrNull()).isEqualTo(TangemPayApiError.Unspecified)
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class FindOrders {

        @Test
        fun `GIVEN orders on the backend WHEN findOrders THEN typed orders are returned`() = runTest {
            // Arrange
            coEvery { orderApi.findOrders(AUTH_HEADER, any(), any()) } returns
                ApiResponse.Success(TangemPayOrdersResponse(result = listOf(orderResponse().result)))

            // Act
            val result = repository.findOrders(USER_WALLET_ID, emptySet(), ACTIVE_STATUSES)

            // Assert
            assertThat(result.getOrNull()).containsExactly(expectedOrder())
        }

        @Test
        fun `GIVEN types and statuses WHEN findOrders THEN their wire values are sent and UNKNOWN is skipped`() = runTest {
            // Arrange
            coEvery { orderApi.findOrders(any(), any(), any()) } returns
                ApiResponse.Success(TangemPayOrdersResponse(result = emptyList()))

            // Act
            repository.findOrders(
                userWalletId = USER_WALLET_ID,
                types = setOf(TangemPayOrder.Type.CARD_FREEZE),
                statuses = ACTIVE_STATUSES + TangemPayOrder.Status.UNKNOWN,
            )

            // Assert
            coVerify(exactly = 1) {
                orderApi.findOrders(
                    authHeader = AUTH_HEADER,
                    orderTypes = listOf("CARD_FREEZE_RAIN"),
                    orderStatuses = listOf("NEW", "PROCESSING"),
                )
            }
        }

        @ParameterizedTest
        @MethodSource("provideTypeWireValues")
        fun `GIVEN order type WHEN findOrders THEN the Swagger wire value is sent`(model: TypeWireModel) = runTest {
            // Arrange
            coEvery { orderApi.findOrders(any(), any(), any()) } returns
                ApiResponse.Success(TangemPayOrdersResponse(result = emptyList()))

            // Act
            repository.findOrders(USER_WALLET_ID, setOf(model.type), emptySet())

            // Assert
            coVerify(exactly = 1) {
                orderApi.findOrders(authHeader = AUTH_HEADER, orderTypes = listOf(model.wire), orderStatuses = emptyList())
            }
        }

        private fun provideTypeWireValues() = listOf(
            TypeWireModel(TangemPayOrder.Type.CARD_ISSUE_VIRTUAL, "CARD_ISSUE_VIRTUAL_RAIN"),
            TypeWireModel(TangemPayOrder.Type.CARD_ISSUE_VIRTUAL_KYC, "CARD_ISSUE_VIRTUAL_RAIN_KYC"),
            TypeWireModel(TangemPayOrder.Type.CARD_ISSUE_VIRTUAL_KYC_V2, "CARD_ISSUE_VIRTUAL_RAIN_KYC_V2"),
            TypeWireModel(TangemPayOrder.Type.CARD_ISSUE_PLASTIC, "CARD_ISSUE_PLASTIC_RAIN"),
            TypeWireModel(TangemPayOrder.Type.CARD_ACTIVATION_PLASTIC, "CARD_ACTIVATION_PLASTIC_RAIN"),
            TypeWireModel(TangemPayOrder.Type.CARD_REISSUE_PLASTIC, "CARD_REISSUE_PLASTIC_RAIN"),
            TypeWireModel(TangemPayOrder.Type.CARD_REISSUE_VIRTUAL, "CARD_REISSUE_VIRTUAL_RAIN"),
            TypeWireModel(TangemPayOrder.Type.CARD_FREEZE, "CARD_FREEZE_RAIN"),
            TypeWireModel(TangemPayOrder.Type.CARD_UNFREEZE, "CARD_UNFREEZE_RAIN"),
            TypeWireModel(TangemPayOrder.Type.CARD_CLOSE, "CARD_CLOSE_RAIN"),
            TypeWireModel(TangemPayOrder.Type.ACCOUNT_ISSUE_VIRTUAL, "ACCOUNT_ISSUE_VIRTUAL_RAIN"),
            TypeWireModel(TangemPayOrder.Type.ACCOUNT_ISSUE_BANKING, "ACCOUNT_ISSUE_BANKING_RAIN"),
            TypeWireModel(TangemPayOrder.Type.WITHDRAWAL, "WITHDRAWAL_RAIN"),
            TypeWireModel(TangemPayOrder.Type.BANK_OFFRAMP, "BANK_OFFRAMP_RAIN"),
            TypeWireModel(TangemPayOrder.Type.SMART_CONTRACT_ISSUE, "SMART_CONTRACT_ISSUE_RAIN"),
            TypeWireModel(TangemPayOrder.Type.TARIFF_PLAN_TRANSITION, "TARIFF_PLAN_TRANSITION"),
            TypeWireModel(TangemPayOrder.Type.ONBOARDING, "ONBOARDING_RAIN"),
            TypeWireModel(TangemPayOrder.Type.UNDEFINED, "UNDEFINED"),
        )

        @Test
        fun `GIVEN backend error WHEN findOrders THEN the error is returned`() = runTest {
            // Arrange
            coEvery {
                requestHelper.performRequest<Any>(userWalletId = any(), requestBlock = any())
            } returns TangemPayApiError.Unspecified.left()

            // Act
            val result = repository.findOrders(USER_WALLET_ID, emptySet(), ACTIVE_STATUSES)

            // Assert
            assertThat(result.leftOrNull()).isEqualTo(TangemPayApiError.Unspecified)
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class CreateOrder {

        @Test
        fun `GIVEN created order WHEN createOrder THEN the typed order is returned`() = runTest {
            // Arrange
            coEvery { orderApi.createOrder(AUTH_HEADER, IDEMPOTENCY_KEY, any()) } returns
                ApiResponse.Success(orderResponse())

            // Act
            val result = repository.createOrder(USER_WALLET_ID, CARD_ISSUE_REQUEST, IDEMPOTENCY_KEY)

            // Assert
            assertThat(result.getOrNull()).isEqualTo(expectedOrder())
        }

        @Test
        fun `GIVEN card issue request WHEN createOrder THEN the body and idempotency key are sent`() = runTest {
            // Arrange
            coEvery { orderApi.createOrder(any(), any(), any()) } returns ApiResponse.Success(orderResponse())

            // Act
            repository.createOrder(USER_WALLET_ID, CARD_ISSUE_REQUEST, IDEMPOTENCY_KEY)

            // Assert
            coVerify(exactly = 1) {
                orderApi.createOrder(
                    authHeader = AUTH_HEADER,
                    idempotencyKey = IDEMPOTENCY_KEY,
                    body = OrderRequest(
                        data = OrderRequest.VirtualCardIssue(
                            customerWalletAddress = WALLET_ADDRESS,
                            specificationName = SPEC_NAME,
                        ),
                    ),
                )
            }
        }

        @Test
        fun `GIVEN wallet address is missing WHEN createOrder THEN an error is returned without a request`() =
            runTest {
                // Arrange
                coEvery {
                    requestHelper.getCustomerWalletAddress(USER_WALLET_ID)
                } throws IllegalStateException("Can not find customer address")

                // Act
                val result = repository.createOrder(USER_WALLET_ID, CARD_ISSUE_REQUEST, IDEMPOTENCY_KEY)

                // Assert
                assertThat(result.leftOrNull()).isEqualTo(TangemPayApiError.UnknownWithoutCode)
                coVerify(exactly = 0) { orderApi.createOrder(any(), any(), any()) }
            }

        @Test
        fun `GIVEN request type without create contract WHEN createOrder THEN an error is returned without a request`() =
            runTest {
                // Act
                val result = repository.createOrder(
                    userWalletId = USER_WALLET_ID,
                    request = CustomerOrderRequest.CardIssue(type = OrderType.CARD_FREEZE, specificationName = SPEC_NAME),
                    idempotencyKey = IDEMPOTENCY_KEY,
                )

                // Assert
                assertThat(result.leftOrNull()).isEqualTo(TangemPayApiError.Unspecified)
                coVerify(exactly = 0) { orderApi.createOrder(any(), any(), any()) }
            }
    }

    internal data class TypeWireModel(val type: TangemPayOrder.Type, val wire: String)

    private fun orderResponse() = TangemPayOrderResponse(
        result = TangemPayOrderResponse.Result(
            id = ORDER_ID,
            status = "PROCESSING",
            step = "STARTING",
            data = OrderDataResponse.CardFreeze(productInstanceId = "pi_1"),
        ),
    )

    private fun expectedOrder() = TangemPayOrder(
        id = ORDER_ID,
        status = TangemPayOrder.Status.PROCESSING,
        step = OrderStep.STARTING,
        details = OrderDetails.CardFreeze(productInstanceId = "pi_1"),
    )

    private companion object {
        val USER_WALLET_ID = UserWalletId("011")
        const val AUTH_HEADER = "auth-header"
        const val WALLET_ADDRESS = "0xWALLET"
        const val SPEC_NAME = "SP_000010"
        const val ORDER_ID = "order-1"
        const val IDEMPOTENCY_KEY = "6f1c9e2a-0b3d-4c5e-8a7b-9d0e1f2a3b4c"
        val ACTIVE_STATUSES = setOf(TangemPayOrder.Status.NEW, TangemPayOrder.Status.PROCESSING)
        val CARD_ISSUE_REQUEST = CustomerOrderRequest.CardIssue(
            type = OrderType.CARD_ISSUE_VIRTUAL_RAIN,
            specificationName = SPEC_NAME,
        )
    }
}