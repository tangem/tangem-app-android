package com.tangem.data.pay.repository

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.core.remote.response.ApiResponse
import com.tangem.core.remote.response.ApiResponseError
import com.tangem.data.pay.store.CashbackSummaryStore
import com.tangem.data.pay.store.TangemPayStorage
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.model.OrderType
import com.tangem.domain.pay.repository.OnboardingRepository
import com.tangem.domain.visa.error.VisaApiError
import com.tangem.spend.datasource.pay.TangemPayApi
import com.tangem.spend.datasource.pay.models.request.OrderRequest
import com.tangem.spend.datasource.pay.models.response.OrderResponse
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

internal class DefaultOnboardingRepositoryTest {

    private val tangemPayApi: TangemPayApi = mockk()
    private val requestHelper: TangemPayRequestPerformer = mockk()
    private val tangemPayStorage: TangemPayStorage = mockk(relaxed = true)
    private val cashbackSummaryStore: CashbackSummaryStore = mockk(relaxed = true)

    private val repository = DefaultOnboardingRepository(
        analytics = mockk(relaxed = true),
        dispatcherProvider = TestingCoroutineDispatcherProvider(),
        tangemPayApi = tangemPayApi,
        requestHelper = requestHelper,
        tangemPayStorage = tangemPayStorage,
        authDataSource = mockk(relaxed = true),
        cardFrozenStateStore = mockk(relaxed = true),
        userWalletsListRepository = mockk(relaxed = true),
        paymentAccountStatusStore = mockk(relaxed = true),
        txHistoryItemsStore = mockk(relaxed = true),
        cashbackSummaryStore = cashbackSummaryStore,
        customerInfoStore = mockk(relaxed = true),
    )

    @BeforeEach
    fun setUp() {
        clearMocks(tangemPayApi, requestHelper, tangemPayStorage, cashbackSummaryStore)

        coEvery {
            requestHelper.performRequest<Any>(userWalletId = any(), requestBlock = any())
        } coAnswers {
            val block = secondArg<suspend (String) -> ApiResponse<Any>>()
            when (val response = block(AUTH_HEADER)) {
                is ApiResponse.Success -> response.data.right()
                is ApiResponse.Error -> VisaApiError.Unspecified.left()
            }
        }
        coEvery { requestHelper.getCustomerWalletAddress(USER_WALLET_ID) } returns WALLET_ADDRESS
        coEvery { tangemPayStorage.getOrderId(WALLET_ADDRESS) } returns null
        coEvery { tangemPayStorage.isCampaignOrder(WALLET_ADDRESS) } returns false
        coEvery { tangemPayApi.createOrder(any(), any()) } returns ApiResponse.Success(orderResponse(NEW_ORDER_ID))
    }

    @Nested
    inner class CreateOrderBody {

        @Test
        fun `GIVEN campaign params WHEN createOrder THEN body carries deeplink value, specification and type`() =
            runTest {
                // Arrange
                val body = slot<OrderRequest>()
                coEvery { tangemPayApi.createOrder(any(), capture(body)) } returns
                    ApiResponse.Success(orderResponse(NEW_ORDER_ID))

                // Act
                val actual = repository.createOrder(
                    userWalletId = USER_WALLET_ID,
                    deeplinkValue = CAMPAIGN_KEY,
                    specificationName = CAMPAIGN_SPECIFICATION,
                    type = OrderType.CARD_ISSUE_VIRTUAL_RAIN_KYC_V2,
                )

                // Assert
                assertThat(actual).isEqualTo(NEW_ORDER_ID.right())
                assertThat(body.captured.data).isEqualTo(
                    OrderRequest.Data(
                        customerWalletAddress = WALLET_ADDRESS,
                        specificationName = CAMPAIGN_SPECIFICATION,
                        type = "CARD_ISSUE_VIRTUAL_RAIN_KYC_V2",
                        deeplinkValue = CAMPAIGN_KEY,
                    ),
                )
                coVerify(exactly = 1) { tangemPayStorage.storeOrderId(WALLET_ADDRESS, NEW_ORDER_ID, true) }
            }

        @Test
        fun `GIVEN default params WHEN createOrder THEN body keeps legacy specification and type without deeplink`() =
            runTest {
                // Arrange
                val body = slot<OrderRequest>()
                coEvery { tangemPayApi.createOrder(any(), capture(body)) } returns
                    ApiResponse.Success(orderResponse(NEW_ORDER_ID))

                // Act
                val actual = repository.createOrder(USER_WALLET_ID)

                // Assert
                assertThat(actual).isEqualTo(NEW_ORDER_ID.right())
                assertThat(body.captured.data).isEqualTo(
                    OrderRequest.Data(
                        customerWalletAddress = WALLET_ADDRESS,
                        specificationName = OnboardingRepository.DEFAULT_ISSUE_SPECIFICATION_NAME,
                        type = "CARD_ISSUE_VIRTUAL_RAIN_KYC",
                        deeplinkValue = null,
                    ),
                )
                coVerify(exactly = 1) { tangemPayStorage.storeOrderId(WALLET_ADDRESS, NEW_ORDER_ID, false) }
            }

        @Test
        fun `GIVEN backend error WHEN createOrder THEN error returned AND nothing stored`() = runTest {
            // Arrange
            coEvery { tangemPayApi.createOrder(any(), any()) } returns
                ApiResponse.Error(ApiResponseError.NetworkException()) as ApiResponse<OrderResponse>

            // Act
            val actual = repository.createOrder(USER_WALLET_ID, deeplinkValue = CAMPAIGN_KEY)

            // Assert
            assertThat(actual).isEqualTo(VisaApiError.Unspecified.left())
            coVerify(exactly = 0) { tangemPayStorage.storeOrderId(any(), any(), any()) }
        }
    }

    @Nested
    inner class ReusableOrder {

        @Test
        fun `GIVEN stored active campaign order WHEN campaign createOrder THEN stored order returned`() = runTest {
            // Arrange
            coEvery { tangemPayStorage.getOrderId(WALLET_ADDRESS) } returns STORED_ORDER_ID
            coEvery { tangemPayStorage.isCampaignOrder(WALLET_ADDRESS) } returns true
            coEvery { tangemPayApi.getOrder(AUTH_HEADER, STORED_ORDER_ID) } returns
                ApiResponse.Success(orderResponse(STORED_ORDER_ID, OrderResponse.Result.Status.PROCESSING))

            // Act
            val actual = repository.createOrder(USER_WALLET_ID, deeplinkValue = CAMPAIGN_KEY)

            // Assert
            assertThat(actual).isEqualTo(STORED_ORDER_ID.right())
            coVerify(exactly = 0) { tangemPayApi.createOrder(any(), any()) }
        }

        @Test
        fun `GIVEN stored active regular order WHEN campaign createOrder THEN campaign order created instead`() =
            runTest {
                // Arrange
                coEvery { tangemPayStorage.getOrderId(WALLET_ADDRESS) } returns STORED_ORDER_ID
                coEvery { tangemPayStorage.isCampaignOrder(WALLET_ADDRESS) } returns false

                // Act
                val actual = repository.createOrder(USER_WALLET_ID, deeplinkValue = CAMPAIGN_KEY)

                // Assert
                assertThat(actual).isEqualTo(NEW_ORDER_ID.right())
                coVerify(exactly = 0) { tangemPayApi.getOrder(any(), any()) }
                coVerify(exactly = 1) { tangemPayApi.createOrder(any(), any()) }
                coVerify(exactly = 1) { tangemPayStorage.storeOrderId(WALLET_ADDRESS, NEW_ORDER_ID, true) }
            }

        @Test
        fun `GIVEN stored active regular order WHEN regular createOrder THEN stored order returned`() = runTest {
            // Arrange
            coEvery { tangemPayStorage.getOrderId(WALLET_ADDRESS) } returns STORED_ORDER_ID
            coEvery { tangemPayApi.getOrder(AUTH_HEADER, STORED_ORDER_ID) } returns
                ApiResponse.Success(orderResponse(STORED_ORDER_ID, OrderResponse.Result.Status.NEW))

            // Act
            val actual = repository.createOrder(USER_WALLET_ID)

            // Assert
            assertThat(actual).isEqualTo(STORED_ORDER_ID.right())
            coVerify(exactly = 0) { tangemPayApi.createOrder(any(), any()) }
        }

        @Test
        fun `GIVEN stored completed campaign order WHEN campaign createOrder THEN new campaign order created`() =
            runTest {
                // Arrange
                coEvery { tangemPayStorage.getOrderId(WALLET_ADDRESS) } returns STORED_ORDER_ID
                coEvery { tangemPayStorage.isCampaignOrder(WALLET_ADDRESS) } returns true
                coEvery { tangemPayApi.getOrder(AUTH_HEADER, STORED_ORDER_ID) } returns
                    ApiResponse.Success(orderResponse(STORED_ORDER_ID, OrderResponse.Result.Status.COMPLETED))

                // Act
                val actual = repository.createOrder(USER_WALLET_ID, deeplinkValue = CAMPAIGN_KEY)

                // Assert
                assertThat(actual).isEqualTo(NEW_ORDER_ID.right())
                coVerify(exactly = 1) { tangemPayApi.createOrder(any(), any()) }
            }
    }

    @Nested
    inner class DisableTangemPay {

        @Test
        fun `GIVEN backend accepts WHEN disableTangemPay THEN cached cashback summary removed`() = runTest {
            // Arrange
            coEvery { tangemPayApi.setTangemPayEnabledStatus(any(), any()) } returns ApiResponse.Success<Any>(Unit)

            // Act
            repository.disableTangemPay(USER_WALLET_ID)

            // Assert
            coVerify(exactly = 1) { cashbackSummaryStore.remove(listOf(USER_WALLET_ID)) }
        }

        @Test
        fun `GIVEN backend error WHEN disableTangemPay THEN cached cashback summary kept`() = runTest {
            // Arrange
            coEvery { tangemPayApi.setTangemPayEnabledStatus(any(), any()) } returns
                ApiResponse.Error(ApiResponseError.NetworkException()) as ApiResponse<Any>

            // Act
            repository.disableTangemPay(USER_WALLET_ID)

            // Assert
            coVerify(exactly = 0) { cashbackSummaryStore.remove(any()) }
        }
    }

    private fun orderResponse(
        id: String,
        status: OrderResponse.Result.Status = OrderResponse.Result.Status.NEW,
    ) = OrderResponse(
        result = OrderResponse.Result(
            id = id,
            customerId = "customer",
            type = "CARD_ISSUE_VIRTUAL_RAIN_KYC_V2",
            status = status,
            step = null,
            data = OrderResponse.Result.Data(
                type = "CARD_ISSUE_VIRTUAL_RAIN_KYC_V2",
                specificationName = CAMPAIGN_SPECIFICATION,
                customerWalletAddress = WALLET_ADDRESS,
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
        const val AUTH_HEADER = "auth-header"
        const val WALLET_ADDRESS = "0xabc"
        const val CAMPAIGN_KEY = "tpay_for_oxford"
        const val CAMPAIGN_SPECIFICATION = "SP_000009"
        const val STORED_ORDER_ID = "order_stored"
        const val NEW_ORDER_ID = "order_new"
    }
}