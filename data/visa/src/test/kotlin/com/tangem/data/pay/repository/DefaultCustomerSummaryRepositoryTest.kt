package com.tangem.data.pay.repository

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.core.remote.response.ApiResponse
import com.tangem.core.remote.response.ApiResponseError
import com.tangem.data.pay.store.TangemPayCustomerSummaryStore
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.model.CustomerSummary
import com.tangem.domain.visa.error.VisaApiError
import com.tangem.spend.datasource.pay.TangemPayApi
import com.tangem.spend.datasource.pay.models.response.CustomerSummaryResponse
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class DefaultCustomerSummaryRepositoryTest {

    private val tangemPayApi: TangemPayApi = mockk()
    private val requestHelper: TangemPayRequestPerformer = mockk()
    private val customerSummaryStore = TangemPayCustomerSummaryStore()

    private val repository = DefaultCustomerSummaryRepository(
        tangemPayApi = tangemPayApi,
        requestHelper = requestHelper,
        customerSummaryStore = customerSummaryStore,
    )

    @BeforeEach
    fun setUp() {
        clearMocks(tangemPayApi, requestHelper)
        customerSummaryStore.clear()

        coEvery {
            requestHelper.performRequest<Any>(userWalletId = any(), requestBlock = any())
        } coAnswers {
            val block = secondArg<suspend (String) -> ApiResponse<Any>>()
            when (val response = block(AUTH_HEADER)) {
                is ApiResponse.Success -> response.data.right()
                is ApiResponse.Error -> VisaApiError.Unspecified.left()
            }
        }
    }


    @Test
    fun `GIVEN summary returned WHEN fetch THEN converted summary returned`() = runTest {
        // Arrange
        coEvery { tangemPayApi.getCustomerSummary(authHeader = AUTH_HEADER) } returns
            ApiResponse.Success(CustomerSummaryResponse(result = customerSummaryResult()))
        val expected = CustomerSummary(
            customerId = "customer",
            customerStatus = CustomerSummary.CustomerStatus.ACTIVE,
            isHidden = false,
            kycStatus = CustomerSummary.KycStatus.APPROVED,
            paymentAccount = CustomerSummary.PaymentAccount(
                id = "payment-account",
                activation = CustomerSummary.PaymentAccount.Activation(
                    status = CustomerSummary.PaymentAccount.Activation.Status.COMPLETED,
                    step = null,
                    failureReason = null,
                ),
                cards = CustomerSummary.Cards(active = 1, frozen = 0, pending = 0),
            ),
            moneyAccount = CustomerSummary.MoneyAccount(
                id = null,
                activation = CustomerSummary.MoneyAccount.Activation(
                    status = CustomerSummary.MoneyAccount.Activation.Status.PROCESSING,
                    step = CustomerSummary.MoneyAccount.Activation.Step.AWAITING_KYC,
                    failureReason = null,
                ),
            ),
        )

        // Act
        val actual = repository.fetch(USER_WALLET_ID)

        // Assert
        assertThat(actual).isEqualTo(expected.right())
        assertThat(repository.getFromCache(USER_WALLET_ID)).isEqualTo(expected)
    }

    @Test
    fun `GIVEN concurrent callers WHEN fetch THEN one request shared`() = runTest {
        // Arrange
        coEvery { tangemPayApi.getCustomerSummary(authHeader = AUTH_HEADER) } coAnswers {
            delay(1_000)
            ApiResponse.Success(CustomerSummaryResponse(result = customerSummaryResult()))
        }

        // Act
        val results = List(size = 2) { async { repository.fetch(USER_WALLET_ID) } }.awaitAll()

        // Assert
        assertThat(results[0].isRight()).isTrue()
        assertThat(results[1]).isEqualTo(results[0])
        coVerify(exactly = 1) { tangemPayApi.getCustomerSummary(authHeader = AUTH_HEADER) }
    }

    @Test
    fun `GIVEN sequential callers WHEN fetch THEN each fetches`() = runTest {
        // Arrange
        coEvery { tangemPayApi.getCustomerSummary(authHeader = AUTH_HEADER) } returns
            ApiResponse.Success(CustomerSummaryResponse(result = customerSummaryResult()))

        // Act
        repository.fetch(USER_WALLET_ID)
        repository.fetch(USER_WALLET_ID)

        // Assert
        coVerify(exactly = 2) { tangemPayApi.getCustomerSummary(authHeader = AUTH_HEADER) }
    }

    @Test
    fun `GIVEN response without result WHEN fetch THEN error`() = runTest {
        // Arrange
        coEvery { tangemPayApi.getCustomerSummary(authHeader = AUTH_HEADER) } returns
            ApiResponse.Success(CustomerSummaryResponse(result = null))

        // Act
        val actual = repository.fetch(USER_WALLET_ID)

        // Assert
        assertThat(actual).isEqualTo(VisaApiError.UnknownWithoutCode.left())
    }

    @Test
    fun `GIVEN failing request WHEN fetch THEN api error`() = runTest {
        // Arrange
        coEvery { tangemPayApi.getCustomerSummary(authHeader = AUTH_HEADER) } returns
            ApiResponse.Error(ApiResponseError.NetworkException()) as ApiResponse<CustomerSummaryResponse>

        // Act
        val actual = repository.fetch(USER_WALLET_ID)

        // Assert
        assertThat(actual).isEqualTo(VisaApiError.Unspecified.left())
        assertThat(repository.getFromCache(USER_WALLET_ID)).isNull()
    }

    private fun customerSummaryResult() = CustomerSummaryResponse.Result(
        customerId = "customer",
        customerStatus = "ACTIVE",
        isHidden = false,
        kycStatus = "APPROVED",
        paymentAccount = CustomerSummaryResponse.PaymentAccount(
            id = "payment-account",
            activation = CustomerSummaryResponse.Activation(status = "COMPLETED", step = null, failureReason = null),
            cards = CustomerSummaryResponse.Cards(active = 1, frozen = 0, pending = 0),
        ),
        bankingAccount = CustomerSummaryResponse.BankingAccount(
            id = null,
            activation = CustomerSummaryResponse.Activation(
                status = "PROCESSING",
                step = "AWAITING_KYC",
                failureReason = null,
            ),
        ),
    )

    private companion object {
        val USER_WALLET_ID = UserWalletId("011")
        const val AUTH_HEADER = "auth-header"
    }
}