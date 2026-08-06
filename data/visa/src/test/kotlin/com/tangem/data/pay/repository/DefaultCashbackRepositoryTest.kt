package com.tangem.data.pay.repository

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.data.pay.util.CashbackAccrualDocsConverter
import com.tangem.data.pay.util.CashbackPromotionsConverter
import com.tangem.data.visa.utils.PayTransactionCashbackConverter
import com.tangem.core.remote.response.ApiResponse
import com.tangem.core.remote.response.ApiResponseError
import com.tangem.spend.datasource.pay.TangemPayApi
import com.tangem.spend.datasource.pay.models.response.CashbackAccrualDocsResponse
import com.tangem.spend.datasource.pay.models.response.CashbackPromotionsResponse
import com.tangem.spend.datasource.pay.models.response.CashbackTransactionDetailsResponse
import com.tangem.spend.datasource.pay.models.response.TransactionCashbackResponse
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.visa.error.VisaApiError
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal

internal class DefaultCashbackRepositoryTest {

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
            when (val response = block(AUTH_HEADER)) {
                is ApiResponse.Success -> response.data.right()
                is ApiResponse.Error -> VisaApiError.Unspecified.left()
            }
        }
    }

    @Test
    fun `GIVEN promotions response WHEN getCashbackPromotions THEN converter result is returned`() = runTest {
        // Arrange
        val response = CashbackPromotionsResponse(cashbackOnCards = null, additionalCashback = null)
        coEvery { tangemPayApi.getCashbackPromotions(any()) } returns ApiResponse.Success(response)

        // Act
        val actual = createRepository().getCashbackPromotions(userWalletId)

        // Assert
        assertThat(actual).isEqualTo(CashbackPromotionsConverter.convert(response).right())
    }

    @Test
    fun `GIVEN backend error WHEN getCashbackPromotions THEN error is propagated`() = runTest {
        // Arrange
        coEvery { tangemPayApi.getCashbackPromotions(any()) } returns
            ApiResponse.Error(ApiResponseError.NetworkException()) as ApiResponse<CashbackPromotionsResponse>

        // Act
        val actual = createRepository().getCashbackPromotions(userWalletId)

        // Assert
        assertThat(actual).isEqualTo(VisaApiError.Unspecified.left())
    }

    @Test
    fun `GIVEN docs response WHEN getCashbackAccrualDocs THEN converter result is returned`() = runTest {
        // Arrange
        val response = CashbackAccrualDocsResponse(
            docs = listOf(CashbackAccrualDocsResponse.Doc(id = "1", title = "Terms", url = "https://a")),
        )
        coEvery { tangemPayApi.getCashbackAccrualDocs(any()) } returns ApiResponse.Success(response)

        // Act
        val actual = createRepository().getCashbackAccrualDocs(userWalletId)

        // Assert
        assertThat(actual).isEqualTo(CashbackAccrualDocsConverter.convert(response).right())
    }

    @Test
    fun `GIVEN backend error WHEN getCashbackAccrualDocs THEN error is propagated`() = runTest {
        // Arrange
        coEvery { tangemPayApi.getCashbackAccrualDocs(any()) } returns
            ApiResponse.Error(ApiResponseError.NetworkException()) as ApiResponse<CashbackAccrualDocsResponse>

        // Act
        val actual = createRepository().getCashbackAccrualDocs(userWalletId)

        // Assert
        assertThat(actual).isEqualTo(VisaApiError.Unspecified.left())
    }

    @Test
    fun `GIVEN details response WHEN getCashbackDetails THEN converter result is returned`() = runTest {
        // Arrange
        val response = CashbackTransactionDetailsResponse(
            cashback = TransactionCashbackResponse(
                status = "confirmed",
                amount = BigDecimal("0.63"),
                currency = "USD",
            ),
        )
        coEvery { tangemPayApi.getCashbackDetails(any(), any()) } returns ApiResponse.Success(response)

        // Act
        val actual = createRepository().getCashbackDetails(userWalletId, transactionId = "tx_1")

        // Assert
        assertThat(actual).isEqualTo(PayTransactionCashbackConverter.convert(response.cashback).right())
    }

    @Test
    fun `GIVEN backend error WHEN getCashbackDetails THEN error is propagated`() = runTest {
        // Arrange
        coEvery { tangemPayApi.getCashbackDetails(any(), any()) } returns
            ApiResponse.Error(ApiResponseError.NetworkException()) as ApiResponse<CashbackTransactionDetailsResponse>

        // Act
        val actual = createRepository().getCashbackDetails(userWalletId, transactionId = "tx_1")

        // Assert
        assertThat(actual).isEqualTo(VisaApiError.Unspecified.left())
    }

    private fun createRepository() = DefaultCashbackRepository(
        tangemPayApi = tangemPayApi,
        requestHelper = requestHelper,
        storage = mockk(),
    )

    private companion object {
        const val AUTH_HEADER = "auth-header"
    }
}