package com.tangem.data.pay.repository

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.core.remote.response.ApiResponse
import com.tangem.core.remote.response.ApiResponseError
import com.tangem.spend.datasource.pay.TangemPayApi
import com.tangem.spend.datasource.pay.models.response.CustomerOffersResponse
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.model.Offer
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
                    orderType = "CARD_ISSUE_VIRTUAL_RAIN_KYC",
                ),
            ),
        ),
    )

    private val reissueOffersResponse = CustomerOffersResponse(
        result = listOf(
            CustomerOffersResponse.Offer(
                type = "CARD_REISSUE_PLASTIC_RAIN",
                fee = CustomerOffersResponse.Fee(amount = BigDecimal("10.00"), currency = "USD"),
                data = CustomerOffersResponse.Data(
                    orderType = "CARD_REISSUE_PLASTIC_RAIN",
                    deliveryEtaMinDays = 3,
                    deliveryEtaMaxDays = 20,
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
        coEvery {
            tangemPayApi.getProductInstanceOffers(any(), any())
        } returns ApiResponse.Success(reissueOffersResponse)
    }

    @Test
    fun `GIVEN offers response WHEN getOffers called twice THEN backend is hit each time`() = runTest {
        // Arrange
        val repository = createRepository()

        // Act
        val first = repository.getOffers(userWalletId)
        val second = repository.getOffers(userWalletId)

        // Assert
        assertThat(first.isRight()).isTrue()
        assertThat(second).isEqualTo(first)
        coVerify(exactly = 2) { tangemPayApi.getCustomerOffers(any()) }
    }

    @Test
    fun `GIVEN backend error WHEN getOffers called again THEN backend is hit again`() = runTest {
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

    @Test
    fun `GIVEN a plastic offer without fee and data WHEN getOffers THEN only the virtual offer is returned`() =
        runTest {
            // Arrange
            val response = CustomerOffersResponse(
                result = offersResponse.result + CustomerOffersResponse.Offer(
                    type = "CARD_ISSUE_PLASTIC_RAIN",
                    fee = null,
                    data = null,
                ),
            )
            coEvery { tangemPayApi.getCustomerOffers(any(), any()) } returns ApiResponse.Success(response)
            val repository = createRepository()

            // Act
            val actual = repository.getOffers(userWalletId)

            // Assert
            assertThat(actual.getOrNull()?.single()?.type).isEqualTo(Offer.Type.CARD_ISSUE_VIRTUAL_RAIN)
        }

    @Test
    fun `GIVEN a product instance WHEN getProductInstanceOffers THEN the instance offers are returned`() = runTest {
        // Arrange
        val repository = createRepository()

        // Act
        val actual = repository.getProductInstanceOffers(userWalletId, PRODUCT_INSTANCE_ID)

        // Assert
        assertThat(actual.getOrNull()?.single()?.type).isEqualTo(Offer.Type.CARD_REISSUE_PLASTIC_RAIN)
        coVerify(exactly = 1) { tangemPayApi.getProductInstanceOffers(AUTH_HEADER, PRODUCT_INSTANCE_ID) }
    }

    @Test
    fun `GIVEN backend error WHEN getProductInstanceOffers THEN the error is returned`() = runTest {
        // Arrange
        coEvery { tangemPayApi.getProductInstanceOffers(any(), any()) } returns
            ApiResponse.Error(ApiResponseError.NetworkException()) as ApiResponse<CustomerOffersResponse>
        val repository = createRepository()

        // Act
        val actual = repository.getProductInstanceOffers(userWalletId, PRODUCT_INSTANCE_ID)

        // Assert
        assertThat(actual.leftOrNull()).isEqualTo(VisaApiError.Unspecified)
    }

    private fun createRepository() = DefaultCustomerOffersRepository(
        tangemPayApi = tangemPayApi,
        requestHelper = requestHelper,
    )

    private companion object {
        const val AUTH_HEADER = "auth-header"
        const val PRODUCT_INSTANCE_ID = "pi_source_0001"
    }
}