package com.tangem.data.pay.repository

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.datasource.api.common.response.ApiResponse
import com.tangem.datasource.api.common.response.ApiResponseError
import com.tangem.datasource.api.pay.TangemPayApi
import com.tangem.datasource.api.pay.models.response.CustomerOffersResponse
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.visa.error.VisaApiError
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal

internal class DefaultCustomerOffersRepositoryTest {

    private val tangemPayApi: TangemPayApi = mockk()
    private val requestHelper: TangemPayRequestPerformer = mockk()

    private val userWalletId = UserWalletId("011")

    private val offersResponse = CustomerOffersResponse(
        result = listOf(
            CustomerOffersResponse.Offer(
                type = "CARD_ISSUE_VIRTUAL_RAIN",
                fee = CustomerOffersResponse.Fee(amount = BigDecimal("1.00"), currency = "USD"),
                data = CustomerOffersResponse.Data(
                    specificationName = "SP_000004",
                    orderType = "CARD_ISSUE_ADDITIONAL",
                ),
            ),
        ),
    )

    @BeforeEach
    fun setUp() {
        clearMocks(tangemPayApi, requestHelper)

        // performRequest is a transparent pass-through: it invokes the request block and maps the
        // ApiResponse to Either, so each test drives behaviour via the TangemPayApi mock.
        coEvery {
            requestHelper.performRequest<Any>(userWalletId = any(), requestBlock = any())
        } coAnswers {
            val block = secondArg<suspend (String) -> ApiResponse<Any>>()
            when (val response = block(AUTH_HEADER)) {
                is ApiResponse.Success -> response.data.right()
                is ApiResponse.Error -> VisaApiError.Unspecified.left()
            }
        }
        coEvery { tangemPayApi.getCustomerOffers(any()) } returns ApiResponse.Success(offersResponse)
    }

    @Test
    fun `GIVEN offers fetched once WHEN getOffers called twice THEN backend is hit only once`() = runTest {
        // Arrange
        val repository = createRepository()

        // Act
        val first = repository.getOffers(userWalletId)
        val second = repository.getOffers(userWalletId)

        // Assert
        assertThat(first.isRight()).isTrue()
        assertThat(second).isEqualTo(first)
        coVerify(exactly = 1) { tangemPayApi.getCustomerOffers(any()) }
    }

    @Test
    fun `GIVEN backend error WHEN getOffers called again THEN error is not cached and backend is hit again`() = runTest {
        // Arrange
        coEvery { tangemPayApi.getCustomerOffers(any()) } returnsMany listOf(
            ApiResponse.Error(ApiResponseError.NetworkException()) as ApiResponse<CustomerOffersResponse>,
            ApiResponse.Success(offersResponse),
        )
        val repository = createRepository()

        // Act
        val first = repository.getOffers(userWalletId)
        val second = repository.getOffers(userWalletId)

        // Assert
        assertThat(first.isLeft()).isTrue()
        assertThat(second.isRight()).isTrue()
        coVerify(exactly = 2) { tangemPayApi.getCustomerOffers(any()) }
    }

    private fun createRepository() = DefaultCustomerOffersRepository(
        tangemPayApi = tangemPayApi,
        requestHelper = requestHelper,
    )

    private companion object {
        const val AUTH_HEADER = "auth-header"
    }
}