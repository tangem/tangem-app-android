package com.tangem.data.pay.repository

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.core.remote.response.ApiResponse
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.model.PlasticCardOrder
import com.tangem.domain.pay.model.ShippingAddress
import com.tangem.domain.visa.error.VisaApiError
import com.tangem.spend.datasource.pay.TangemPayApi
import com.tangem.spend.datasource.pay.models.response.OrderResponse
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class DefaultCustomerOrderRepositoryTest {

    private val tangemPayApi: TangemPayApi = mockk()
    private val requestHelper: TangemPayRequestPerformer = mockk()

    private val userWalletId = UserWalletId("011")

    @BeforeEach
    fun setUp() {
        clearMocks(tangemPayApi, requestHelper)

        coEvery {
            requestHelper.performRequest<Any>(userWalletId = any(), requestBlock = any())
        } coAnswers {
            val block = secondArg<suspend (String) -> ApiResponse<Any>>()
            runCatching { block(AUTH_HEADER) }.fold(
                onSuccess = { response ->
                    when (response) {
                        is ApiResponse.Success -> response.data.right()
                        is ApiResponse.Error -> VisaApiError.Unspecified.left()
                    }
                },
                onFailure = { VisaApiError.UnknownWithoutCode.left() },
            )
        }
        coEvery { requestHelper.getCustomerWalletAddress(userWalletId) } returns WALLET_ADDRESS
        coEvery { tangemPayApi.createOrder(any(), any()) } returns ApiResponse.Success(orderResponse())
    }

    @Test
    fun `GIVEN a created order WHEN createPlasticIssueOrder THEN the order is returned`() = runTest {
        // Arrange
        val repository = createRepository()

        // Act
        val result = repository.createPlasticIssueOrder(
            userWalletId = userWalletId,
            specificationName = SPEC_NAME,
            order = plasticCardOrder(),
            idempotencyKey = IDEMPOTENCY_KEY,
        )

        // Assert
        assertThat(result.getOrNull()?.id).isEqualTo(ORDER_ID)
    }

    @Test
    fun `GIVEN a response without result WHEN createPlasticIssueOrder THEN an error is returned`() = runTest {
        // Arrange
        coEvery { tangemPayApi.createOrder(any(), any()) } returns ApiResponse.Success(OrderResponse(result = null))
        val repository = createRepository()

        // Act
        val result = repository.createPlasticIssueOrder(
            userWalletId = userWalletId,
            specificationName = SPEC_NAME,
            order = plasticCardOrder(),
            idempotencyKey = IDEMPOTENCY_KEY,
        )

        // Assert
        assertThat(result.leftOrNull()).isEqualTo(VisaApiError.UnknownWithoutCode)
    }

    @Test
    fun `GIVEN the wallet address is missing WHEN createPlasticIssueOrder THEN an error is returned`() = runTest {
        // Arrange
        coEvery {
            requestHelper.getCustomerWalletAddress(userWalletId)
        } throws IllegalStateException("Can not find customer address")
        val repository = createRepository()

        // Act
        val result = repository.createPlasticIssueOrder(
            userWalletId = userWalletId,
            specificationName = SPEC_NAME,
            order = plasticCardOrder(),
            idempotencyKey = IDEMPOTENCY_KEY,
        )

        // Assert
        assertThat(result.leftOrNull()).isEqualTo(VisaApiError.UnknownWithoutCode)
        coVerify(exactly = 0) { tangemPayApi.createOrder(any(), any()) }
    }

    private fun createRepository() = DefaultCustomerOrderRepository(
        tangemPayApi = tangemPayApi,
        requestHelper = requestHelper,
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

    private fun orderResponse() = OrderResponse(
        result = OrderResponse.Result(
            id = ORDER_ID,
            customerId = "customer",
            type = "CARD_ISSUE_PLASTIC_RAIN",
            status = OrderResponse.Result.Status.NEW,
            step = null,
            data = OrderResponse.Result.Data(
                type = "CARD_ISSUE_PLASTIC_RAIN",
                specificationName = SPEC_NAME,
                customerWalletAddress = WALLET_ADDRESS,
                embossName = "JOHNNY SILVERHAND",
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
        const val AUTH_HEADER = "auth-header"
        const val WALLET_ADDRESS = "0xWALLET"
        const val SPEC_NAME = "SP_000010"
        const val ORDER_ID = "order-1"
        const val IDEMPOTENCY_KEY = "6f1c9e2a-0b3d-4c5e-8a7b-9d0e1f2a3b4c"
    }
}