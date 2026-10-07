package com.tangem.data.pay.repository

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.data.pay.store.CashbackSummaryStore
import com.tangem.data.pay.store.TangemPayStorage
import com.tangem.data.pay.util.CashbackAccrualDocsConverter
import com.tangem.data.pay.util.CashbackHistoryConverter
import com.tangem.data.pay.util.CashbackPromotionsConverter
import com.tangem.data.pay.util.CashbackSummaryConverter
import com.tangem.data.visa.utils.PayTransactionCashbackConverter
import com.tangem.core.remote.response.ApiResponse
import com.tangem.core.remote.response.ApiResponseError
import com.tangem.spend.datasource.pay.TangemPayApi
import com.tangem.spend.datasource.pay.models.response.CashbackAccrualDocsResponse
import com.tangem.spend.datasource.pay.models.response.CashbackHistoryResponse
import com.tangem.spend.datasource.pay.models.response.CashbackPromotionsResponse
import com.tangem.spend.datasource.pay.models.response.CashbackSummaryResponse
import com.tangem.spend.datasource.pay.models.response.CashbackTransactionDetailsResponse
import com.tangem.spend.datasource.pay.models.response.TransactionCashbackResponse
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.model.CashbackSummary
import com.tangem.domain.visa.error.VisaApiError
import io.mockk.Runs
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal

internal class DefaultCashbackRepositoryTest {

    private val tangemPayApi: TangemPayApi = mockk()
    private val requestHelper: TangemPayRequestPerformer = mockk()
    private val summaryStore: CashbackSummaryStore = mockk()
    private val storage: TangemPayStorage = mockk()

    private val userWalletId = UserWalletId("011")

    @BeforeEach
    fun setUp() {
        clearMocks(tangemPayApi, requestHelper, summaryStore, storage)
        coEvery {
            requestHelper.performRequest<Any>(userWalletId = any(), requestBlock = any())
        } coAnswers {
            val block = secondArg<suspend (String) -> ApiResponse<Any>>()
            when (val response = block(AUTH_HEADER)) {
                is ApiResponse.Success -> response.data.right()
                is ApiResponse.Error -> VisaApiError.Unspecified.left()
            }
        }
        coEvery { summaryStore.store(any(), any()) } just Runs
    }

    @Test
    fun `GIVEN promotions response WHEN getCashbackPromotions THEN converter result is returned`() = runTest {
        // Arrange
        val response = CashbackPromotionsResponse(
            result = CashbackPromotionsResponse.Result(cashbackOnCards = null, additionalCashback = null),
        )
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
            result = CashbackAccrualDocsResponse.Result(
                docs = listOf(CashbackAccrualDocsResponse.Doc(id = "1", title = "Terms", url = "https://a")),
            ),
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
    fun `GIVEN history response WHEN getCashbackHistory THEN converter result is returned`() = runTest {
        // Arrange
        val response = CashbackHistoryResponse(
            result = CashbackHistoryResponse.Result(
                items = listOf(
                    CashbackHistoryResponse.Item(
                        year = 2026,
                        month = 6,
                        confirmedAmount = BigDecimal("22.54"),
                        currency = "USD",
                    ),
                ),
            ),
        )
        coEvery { tangemPayApi.getCashbackHistory(any(), any()) } returns ApiResponse.Success(response)

        // Act
        val actual = createRepository().getCashbackHistory(userWalletId, monthsNumber = 5)

        // Assert
        assertThat(actual).isEqualTo(CashbackHistoryConverter.convert(response).right())
        coVerify(exactly = 1) { tangemPayApi.getCashbackHistory(AUTH_HEADER, 5) }
    }

    @Test
    fun `GIVEN details response WHEN getCashbackDetails THEN converter result is returned`() = runTest {
        // Arrange
        val response = CashbackTransactionDetailsResponse(
            result = CashbackTransactionDetailsResponse.Result(
                cashback = TransactionCashbackResponse(
                    status = "confirmed",
                    amount = BigDecimal("0.63"),
                    currency = "USD",
                ),
            ),
        )
        coEvery { tangemPayApi.getCashbackDetails(any(), any()) } returns ApiResponse.Success(response)

        // Act
        val actual = createRepository().getCashbackDetails(userWalletId, transactionId = "tx_1")

        // Assert
        assertThat(actual).isEqualTo(PayTransactionCashbackConverter.convert(response.result?.cashback).right())
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

    @Test
    fun `GIVEN summary response WHEN getCashbackSummary THEN summary returned and cached`() = runTest {
        // Arrange
        coEvery { tangemPayApi.getCashbackSummary(any()) } returns ApiResponse.Success(SUMMARY_RESPONSE)
        val expected = CashbackSummaryConverter.convert(SUMMARY_RESPONSE)

        // Act
        val actual = createRepository().getCashbackSummary(userWalletId)

        // Assert
        assertThat(actual).isEqualTo(expected.right())
        coVerify(exactly = 1) { summaryStore.store(userWalletId, expected) }
    }

    @Test
    fun `GIVEN backend error WHEN getCashbackSummary THEN error returned and cache untouched`() = runTest {
        // Arrange
        coEvery { tangemPayApi.getCashbackSummary(any()) } returns
            ApiResponse.Error(ApiResponseError.NetworkException()) as ApiResponse<CashbackSummaryResponse>

        // Act
        val actual = createRepository().getCashbackSummary(userWalletId)

        // Assert
        assertThat(actual).isEqualTo(VisaApiError.Unspecified.left())
        coVerify(exactly = 0) { summaryStore.store(any(), any()) }
    }

    @Test
    fun `GIVEN cached summary WHEN observeCashbackSummary THEN cached value emitted`() = runTest {
        // Arrange
        every { summaryStore.get(userWalletId) } returns flowOf(CashbackSummary.Deactivated)

        // Act
        val actual = createRepository().observeCashbackSummary(userWalletId).first()

        // Assert
        assertThat(actual).isEqualTo(CashbackSummary.Deactivated)
    }

    @Test
    fun `GIVEN customer address missing WHEN isDeactivationBannerDismissed THEN returns false`() = runTest {
        // Arrange
        coEvery { requestHelper.getCustomerWalletAddress(userWalletId) } throws IllegalStateException("no address")

        // Act
        val actual = createRepository().isDeactivationBannerDismissed(userWalletId)

        // Assert
        assertThat(actual).isFalse()
    }

    @Test
    fun `GIVEN customer address missing WHEN setDeactivationBannerDismissed THEN does not throw`() = runTest {
        // Arrange
        coEvery { requestHelper.getCustomerWalletAddress(userWalletId) } throws IllegalStateException("no address")

        // Act
        val exception = runCatching { createRepository().setDeactivationBannerDismissed(userWalletId) }
            .exceptionOrNull()

        // Assert
        assertThat(exception).isNull()
        coVerify(exactly = 0) { storage.storeCashbackDeactivationDismissed(any(), any()) }
    }

    private fun createRepository() = DefaultCashbackRepository(
        tangemPayApi = tangemPayApi,
        requestHelper = requestHelper,
        storage = storage,
        summaryStore = summaryStore,
    )

    private companion object {
        const val AUTH_HEADER = "auth-header"

        val SUMMARY_RESPONSE = CashbackSummaryResponse(
            result = CashbackSummaryResponse.Result(
                cashbackProgramStatus = "enabled",
                cashbackDisplayMode = "full",
                period = CashbackSummaryResponse.Period(
                    year = 2026,
                    month = 8,
                    payoutStartDate = null,
                    payoutEndDate = null,
                ),
                payoutCurrency = "USDC",
                confirmedAmount = BigDecimal("2.70"),
                totalEarnedAmount = null,
                previousPayoutEndDate = null,
                previousPayoutAmount = null,
                currency = "USD",
            ),
        )
    }
}