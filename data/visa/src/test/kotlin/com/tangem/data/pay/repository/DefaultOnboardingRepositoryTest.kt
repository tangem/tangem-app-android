package com.tangem.data.pay.repository

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.core.remote.response.ApiResponse
import com.tangem.core.remote.response.ApiResponseError
import com.tangem.data.pay.store.CashbackSummaryStore
import com.tangem.data.pay.store.TangemPayStorage
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.model.AccountFee
import com.tangem.domain.pay.model.AccountFeeGroupType
import com.tangem.domain.pay.model.CustomerOrderRequest
import com.tangem.domain.pay.model.OrderType
import com.tangem.domain.visa.error.VisaApiError
import com.tangem.spend.datasource.pay.TangemPayApi
import com.tangem.spend.datasource.pay.models.request.OrderRequest
import com.tangem.spend.datasource.pay.models.response.FeesResponse
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
import java.math.BigDecimal

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
        coEvery { tangemPayApi.createOrder(any(), any(), any()) } returns
            ApiResponse.Success(orderResponse(NEW_ORDER_ID))
    }

    @Nested
    inner class CreateOrderBody {

        @Test
        fun `GIVEN no reusable order WHEN createOrder THEN the caller's idempotency key is passed to the header`() =
            runTest {
                // Act
                repository.createOrder(USER_WALLET_ID, REGULAR_REQUEST, IDEMPOTENCY_KEY)

                // Assert
                coVerify(exactly = 1) { tangemPayApi.createOrder(AUTH_HEADER, IDEMPOTENCY_KEY, any()) }
            }

        @Test
        fun `GIVEN campaign params WHEN createOrder THEN body carries deeplink value, specification and type`() =
            runTest {
                // Arrange
                val body = slot<OrderRequest>()
                coEvery { tangemPayApi.createOrder(any(), any(), capture(body)) } returns
                    ApiResponse.Success(orderResponse(NEW_ORDER_ID))

                // Act
                val actual = repository.createOrder(USER_WALLET_ID, CAMPAIGN_REQUEST, IDEMPOTENCY_KEY)

                // Assert
                assertThat(actual).isEqualTo(NEW_ORDER_ID.right())
                assertThat(body.captured.data).isEqualTo(
                    OrderRequest.VirtualCardIssueKycV2(
                        customerWalletAddress = WALLET_ADDRESS,
                        specificationName = CAMPAIGN_SPECIFICATION,
                        deeplinkValue = CAMPAIGN_KEY,
                    ),
                )
                coVerify(exactly = 1) { tangemPayStorage.storeOrderId(WALLET_ADDRESS, NEW_ORDER_ID, true) }
            }

        @Test
        fun `GIVEN request without deeplink WHEN createOrder THEN body has no deeplink AND order stored as regular`() =
            runTest {
                // Arrange
                val body = slot<OrderRequest>()
                coEvery { tangemPayApi.createOrder(any(), any(), capture(body)) } returns
                    ApiResponse.Success(orderResponse(NEW_ORDER_ID))

                // Act
                val actual = repository.createOrder(USER_WALLET_ID, REGULAR_REQUEST, IDEMPOTENCY_KEY)

                // Assert
                assertThat(actual).isEqualTo(NEW_ORDER_ID.right())
                assertThat(body.captured.data).isEqualTo(
                    OrderRequest.VirtualCardIssueKyc(
                        customerWalletAddress = WALLET_ADDRESS,
                        specificationName = REGULAR_SPECIFICATION,
                        deeplinkValue = null,
                    ),
                )
                coVerify(exactly = 1) { tangemPayStorage.storeOrderId(WALLET_ADDRESS, NEW_ORDER_ID, false) }
            }

        @Test
        fun `GIVEN backend error WHEN createOrder THEN error returned AND nothing stored`() = runTest {
            // Arrange
            coEvery { tangemPayApi.createOrder(any(), any(), any()) } returns
                ApiResponse.Error(ApiResponseError.NetworkException()) as ApiResponse<OrderResponse>

            // Act
            val actual = repository.createOrder(USER_WALLET_ID, CAMPAIGN_REQUEST, IDEMPOTENCY_KEY)

            // Assert
            assertThat(actual).isEqualTo(VisaApiError.Unspecified.left())
            coVerify(exactly = 0) { tangemPayStorage.storeOrderId(any(), any(), any()) }
        }

        @Test
        fun `GIVEN response without result WHEN createOrder THEN error returned AND nothing stored`() = runTest {
            // Arrange
            coEvery { tangemPayApi.createOrder(any(), any(), any()) } returns
                ApiResponse.Success(OrderResponse(result = null))

            // Act
            val actual = repository.createOrder(USER_WALLET_ID, CAMPAIGN_REQUEST, IDEMPOTENCY_KEY)

            // Assert
            assertThat(actual).isEqualTo(VisaApiError.UnknownWithoutCode.left())
            coVerify(exactly = 0) { tangemPayStorage.storeOrderId(any(), any(), any()) }
        }
    }

    @Nested
    inner class CreateVirtualAccountOrder {

        @Test
        fun `GIVEN created order WHEN createVirtualAccountOrder THEN the order id is returned`() = runTest {
            // Act
            val actual = createVirtualAccountOrder()

            // Assert
            assertThat(actual).isEqualTo(NEW_ORDER_ID.right())
        }

        @Test
        fun `GIVEN an idempotency key WHEN createVirtualAccountOrder THEN it is passed to the idempotency header`() =
            runTest {
                // Act
                createVirtualAccountOrder()

                // Assert
                coVerify(exactly = 1) { tangemPayApi.createOrder(AUTH_HEADER, IDEMPOTENCY_KEY, any()) }
            }

        @Test
        fun `GIVEN response without result WHEN createVirtualAccountOrder THEN error returned instead of a crash`() =
            runTest {
                // Arrange
                coEvery { tangemPayApi.createOrder(any(), any(), any()) } returns
                    ApiResponse.Success(OrderResponse(result = null))

                // Act
                val actual = createVirtualAccountOrder()

                // Assert
                assertThat(actual).isEqualTo(VisaApiError.UnknownWithoutCode.left())
            }

        private suspend fun createVirtualAccountOrder() = repository.createVirtualAccountOrder(
            userWalletId = USER_WALLET_ID,
            paymentAccountAddress = PAYMENT_ACCOUNT_ADDRESS,
            idempotencyKey = IDEMPOTENCY_KEY,
        )
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
            val actual = repository.createOrder(USER_WALLET_ID, CAMPAIGN_REQUEST, IDEMPOTENCY_KEY)

            // Assert
            assertThat(actual).isEqualTo(STORED_ORDER_ID.right())
            coVerify(exactly = 0) { tangemPayApi.createOrder(any(), any(), any()) }
        }

        @Test
        fun `GIVEN stored active regular order WHEN campaign createOrder THEN campaign order created instead`() =
            runTest {
                // Arrange
                coEvery { tangemPayStorage.getOrderId(WALLET_ADDRESS) } returns STORED_ORDER_ID
                coEvery { tangemPayStorage.isCampaignOrder(WALLET_ADDRESS) } returns false

                // Act
                val actual = repository.createOrder(USER_WALLET_ID, CAMPAIGN_REQUEST, IDEMPOTENCY_KEY)

                // Assert
                assertThat(actual).isEqualTo(NEW_ORDER_ID.right())
                coVerify(exactly = 0) { tangemPayApi.getOrder(any(), any()) }
                coVerify(exactly = 1) { tangemPayApi.createOrder(any(), any(), any()) }
                coVerify(exactly = 1) { tangemPayStorage.storeOrderId(WALLET_ADDRESS, NEW_ORDER_ID, true) }
            }

        @Test
        fun `GIVEN stored active regular order WHEN regular createOrder THEN stored order returned`() = runTest {
            // Arrange
            coEvery { tangemPayStorage.getOrderId(WALLET_ADDRESS) } returns STORED_ORDER_ID
            coEvery { tangemPayApi.getOrder(AUTH_HEADER, STORED_ORDER_ID) } returns
                ApiResponse.Success(orderResponse(STORED_ORDER_ID, OrderResponse.Result.Status.NEW))

            // Act
            val actual = repository.createOrder(USER_WALLET_ID, REGULAR_REQUEST, IDEMPOTENCY_KEY)

            // Assert
            assertThat(actual).isEqualTo(STORED_ORDER_ID.right())
            coVerify(exactly = 0) { tangemPayApi.createOrder(any(), any(), any()) }
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
                val actual = repository.createOrder(USER_WALLET_ID, CAMPAIGN_REQUEST, IDEMPOTENCY_KEY)

                // Assert
                assertThat(actual).isEqualTo(NEW_ORDER_ID.right())
                coVerify(exactly = 1) { tangemPayApi.createOrder(any(), any(), any()) }
            }
    }

    @Nested
    inner class DisableTangemPay {

        @Test
        fun `GIVEN backend accepts WHEN disableTangemPay THEN cached cashback summary removed`() = runTest {
            // Arrange
            coEvery { tangemPayApi.setTangemPayEnabledStatus(any(), any()) } returns ApiResponse.Success(Unit)

            // Act
            repository.disableTangemPay(USER_WALLET_ID)

            // Assert
            coVerify(exactly = 1) { cashbackSummaryStore.remove(listOf(USER_WALLET_ID)) }
        }

        @Test
        fun `GIVEN backend error WHEN disableTangemPay THEN cached cashback summary kept`() = runTest {
            // Arrange
            coEvery { tangemPayApi.setTangemPayEnabledStatus(any(), any()) } returns
                ApiResponse.Error(ApiResponseError.NetworkException()) as ApiResponse<Unit>

            // Act
            repository.disableTangemPay(USER_WALLET_ID)

            // Assert
            coVerify(exactly = 0) { cashbackSummaryStore.remove(any()) }
        }
    }

    @Nested
    inner class GetFees {

        @Test
        fun `GIVEN both groups WHEN getFees THEN converted and requested once for both`() = runTest {
            // Arrange
            val expectedGroups = listOf("ONRAMP", "OFFRAMP")
            val response = FeesResponse(
                result = listOf(
                    fee(type = "ACH_ONRAMP", group = "ONRAMP", paymentMethodId = "ACH", amount = BigDecimal("1.00")),
                    fee(type = "ACH_OFFRAMP", group = "OFFRAMP", paymentMethodId = "ACH", amount = BigDecimal("1.50")),
                ),
            )
            coEvery { tangemPayApi.getFees(authHeader = AUTH_HEADER, groups = expectedGroups) } returns
                ApiResponse.Success(response)

            // Act
            val actual = repository.getFees(
                userWalletId = USER_WALLET_ID,
                types = setOf(AccountFeeGroupType.ONRAMP, AccountFeeGroupType.OFFRAMP),
            )

            // Assert
            val expected = listOf(
                AccountFee(
                    type = "ACH_ONRAMP",
                    name = "ACH",
                    paymentMethodId = "ACH",
                    group = AccountFeeGroupType.ONRAMP,
                    amount = BigDecimal("1.00"),
                    currency = "USD",
                ),
                AccountFee(
                    type = "ACH_OFFRAMP",
                    name = "ACH",
                    paymentMethodId = "ACH",
                    group = AccountFeeGroupType.OFFRAMP,
                    amount = BigDecimal("1.50"),
                    currency = "USD",
                ),
            )
            assertThat(actual).isEqualTo(expected.right())
            coVerify(exactly = 1) { tangemPayApi.getFees(authHeader = AUTH_HEADER, groups = expectedGroups) }
        }

        @Test
        fun `GIVEN incomplete entries WHEN getFees THEN they are dropped`() = runTest {
            // Arrange
            val response = FeesResponse(
                result = listOf(
                    fee(type = "ACH_ONRAMP", group = "ONRAMP", paymentMethodId = "ACH", amount = null),
                    fee(type = "ACH_ONRAMP", group = "ONRAMP", paymentMethodId = "ACH", currency = " "),
                ),
            )
            coEvery { tangemPayApi.getFees(authHeader = AUTH_HEADER, groups = any()) } returns
                ApiResponse.Success(response)

            // Act
            val actual = repository.getFees(USER_WALLET_ID, setOf(AccountFeeGroupType.ONRAMP))

            // Assert
            assertThat(actual).isEqualTo(emptyList<AccountFee>().right())
        }

        @Test
        fun `GIVEN lowercase group and no payment method WHEN getFees THEN group recognised and entry kept`() =
            runTest {
                // Arrange
                val response = FeesResponse(
                    result = listOf(fee(type = "ACH_ONRAMP", group = "onramp", paymentMethodId = null)),
                )
                coEvery { tangemPayApi.getFees(authHeader = AUTH_HEADER, groups = any()) } returns
                    ApiResponse.Success(response)

                // Act
                val actual = repository.getFees(USER_WALLET_ID, setOf(AccountFeeGroupType.ONRAMP))

                // Assert
                val expected = AccountFee(
                    type = "ACH_ONRAMP",
                    name = "ACH",
                    paymentMethodId = null,
                    group = AccountFeeGroupType.ONRAMP,
                    amount = BigDecimal.ONE,
                    currency = "USD",
                )
                assertThat(actual).isEqualTo(listOf(expected).right())
            }

        @Test
        fun `GIVEN response without result WHEN getFees THEN error`() = runTest {
            // Arrange
            coEvery { tangemPayApi.getFees(authHeader = AUTH_HEADER, groups = any()) } returns
                ApiResponse.Success(FeesResponse(result = null))

            // Act
            val actual = repository.getFees(USER_WALLET_ID, setOf(AccountFeeGroupType.ONRAMP))

            // Assert
            assertThat(actual).isEqualTo(VisaApiError.UnknownWithoutCode.left())
        }

        @Test
        fun `GIVEN failing request WHEN getFees THEN api error`() = runTest {
            // Arrange
            coEvery { tangemPayApi.getFees(authHeader = AUTH_HEADER, groups = any()) } returns
                ApiResponse.Error(ApiResponseError.NetworkException()) as ApiResponse<FeesResponse>

            // Act
            val actual = repository.getFees(USER_WALLET_ID, setOf(AccountFeeGroupType.ONRAMP))

            // Assert
            assertThat(actual).isEqualTo(VisaApiError.Unspecified.left())
        }

        private fun fee(
            type: String,
            group: String?,
            paymentMethodId: String?,
            amount: BigDecimal? = BigDecimal.ONE,
            currency: String? = "USD",
        ) = FeesResponse.Fee(
            type = type,
            group = group,
            name = "ACH",
            paymentMethodId = paymentMethodId,
            calculationType = "FIXED",
            amount = amount,
            currency = currency,
            description = "fee",
        )
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
        const val REGULAR_SPECIFICATION = "SP_000004"
        val CAMPAIGN_REQUEST = CustomerOrderRequest.CardIssue(
            type = OrderType.CARD_ISSUE_VIRTUAL_RAIN_KYC_V2,
            specificationName = CAMPAIGN_SPECIFICATION,
            deeplinkValue = CAMPAIGN_KEY,
        )
        val REGULAR_REQUEST = CustomerOrderRequest.CardIssue(
            type = OrderType.CARD_ISSUE_VIRTUAL_RAIN_KYC,
            specificationName = REGULAR_SPECIFICATION,
        )
        const val STORED_ORDER_ID = "order_stored"
        const val NEW_ORDER_ID = "order_new"
        const val PAYMENT_ACCOUNT_ADDRESS = "0xpayment"
        const val IDEMPOTENCY_KEY = "6f1c9e2a-0b3d-4c5e-8a7b-9d0e1f2a3b4c"
    }
}