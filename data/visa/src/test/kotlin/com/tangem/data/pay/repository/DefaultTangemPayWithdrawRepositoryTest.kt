package com.tangem.data.pay.repository

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.common.extensions.toHexString
import com.tangem.test.core.TestAppCoroutineScope
import com.tangem.data.common.quote.QuotesFetcher
import com.tangem.core.remote.response.ApiResponse
import com.tangem.core.remote.response.ApiResponseError
import com.tangem.spend.datasource.pay.TangemPayApi
import com.tangem.spend.datasource.pay.models.request.WithdrawDataRequest
import com.tangem.spend.datasource.pay.models.request.WithdrawRequest
import com.tangem.spend.datasource.pay.models.response.WithdrawDataResponse
import com.tangem.spend.datasource.pay.models.response.WithdrawResponse
import com.tangem.datasource.api.tangemTech.models.QuotesResponse
import com.tangem.data.pay.store.TangemPayStorage
import com.tangem.domain.models.account.PaymentNetworkStatus
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.TangemPayWithdrawExchangeState
import com.tangem.domain.pay.TangemPayWithdrawState
import com.tangem.domain.pay.WithdrawalResult
import com.tangem.domain.pay.WithdrawalSignatureResult
import com.tangem.domain.pay.datasource.TangemPayAuthDataSource
import com.tangem.domain.pay.flow.PaymentAccountStatusSupplier
import com.tangem.domain.pay.model.OrderData
import com.tangem.domain.pay.model.OrderStatus
import com.tangem.domain.pay.repository.CustomerOrderRepository
import com.tangem.domain.visa.error.VisaApiError
import com.tangem.feature.swap.domain.api.SwapRepository
import com.tangem.test.core.ProvideTestModels
import com.tangem.test.mock.MockAccounts
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.web3j.crypto.StructuredDataEncoder
import java.math.BigDecimal
import kotlin.time.Duration.Companion.minutes

@OptIn(ExperimentalCoroutinesApi::class)
internal class DefaultTangemPayWithdrawRepositoryTest {

    private val tangemPayApi: TangemPayApi = mockk()
    private val requestHelper: TangemPayRequestPerformer = mockk()
    private val authDataSource: TangemPayAuthDataSource = mockk()
    private val quotesFetcher: QuotesFetcher = mockk()
    private val tangemPayStorage: TangemPayStorage = mockk(relaxUnitFun = true)
    private val swapRepository: SwapRepository = mockk()
    private val orderRepository: CustomerOrderRepository = mockk()

    private val userWalletId = UserWalletId("011")
    private val userWallet: UserWallet = mockk {
        every { walletId } returns userWalletId
    }

    private val paymentAccountStatusSupplier: PaymentAccountStatusSupplier = mockk()
    private val sourceCurrency = MockAccounts.createPaymentAccountToken(
        rawId = CURRENCY_ID,
        contractAddress = CONTRACT_ADDRESS,
    )
    private val exchangeData = TangemPayWithdrawExchangeState(
        txId = "txId",
        fromNetwork = "ETH",
        fromAddress = "0xFrom",
        payInAddress = "0xPayIn",
        payInExtraId = null,
    )

    private val orderWithoutHash = OrderData(
        customerId = "customer",
        status = OrderStatus.PROCESSING,
        withdrawTxHash = null,
    )
    private val orderWithHash = orderWithoutHash.copy(withdrawTxHash = TX_HASH)

    @BeforeEach
    fun setUp() {
        // Valid fiat rate so amountInCents resolves to a non-empty value.
        coEvery {
            quotesFetcher.fetch(fiatCurrencyId = any(), currencyId = any(), field = any())
        } returns QuotesResponse(
            quotes = mapOf(CURRENCY_ID to QuotesResponse.Quote.EMPTY.copy(price = BigDecimal.ONE)),
        ).right()

        // performRequest is treated as a transparent pass-through: it invokes the request block and
        // maps the ApiResponse to Either, so each test can drive behaviour via the TangemPayApi mock.
        coEvery {
            requestHelper.performRequest<Any>(userWalletId = any(), requestBlock = any())
        } coAnswers {
            val block = secondArg<suspend (String) -> ApiResponse<Any>>()
            when (val response = block(AUTH_HEADER)) {
                is ApiResponse.Success -> response.data.right()
                is ApiResponse.Error -> VisaApiError.WithdrawError.left()
            }
        }

        coEvery { tangemPayApi.getWithdrawData(any(), any()) } returns ApiResponse.Success(
            WithdrawDataResponse(result = withdrawData()),
        )
        coEvery { tangemPayApi.withdraw(any(), any()) } returns ApiResponse.Success(
            WithdrawResponse(
                result = WithdrawResponse.Result(orderId = ORDER_ID, status = "NEW", type = "withdraw"),
            ),
        )
        coEvery {
            authDataSource.getWithdrawalSignature(any(), any())
        } returns WithdrawalSignatureResult.Success(SIGNATURE).right()
        coEvery { swapRepository.exchangeSent(any(), any(), any(), any(), any(), any(), any()) } returns Unit.right()

        every { paymentAccountStatusSupplier.invoke(userWalletId) } returns flowOf(
            MockAccounts.createPaymentAccountStatus(
                userWalletId = userWalletId,
                cryptoCurrency = sourceCurrency,
                networks = listOf(
                    PaymentNetworkStatus.Available(
                        network = sourceCurrency.network,
                        depositAddress = "0xDeposit",
                        chainId = CHAIN_ID,
                        cryptoCurrencyStatuses = emptyList(),
                    ),
                ),
            ),
        )
    }

    // region withdrawWithSwap

    @Test
    fun `GIVEN amountInCents is null WHEN withdrawWithSwap THEN return WithdrawalDataError`() = runTest {
        coEvery {
            quotesFetcher.fetch(fiatCurrencyId = any(), currencyId = any(), field = any())
        } returns QuotesFetcher.Error.CacheOperationError.left()

        val result = createRepository().withdrawWithSwap()

        Assertions.assertEquals(VisaApiError.WithdrawalDataError.left(), result)
        coVerify(exactly = 0) { tangemPayApi.getWithdrawData(any(), any()) }
    }

    @Test
    fun `GIVEN getWithdrawData result is null WHEN withdrawWithSwap THEN return WithdrawalDataError`() = runTest {
        coEvery { tangemPayApi.getWithdrawData(any(), any()) } returns ApiResponse.Success(
            WithdrawDataResponse(result = null),
        )

        val result = createRepository().withdrawWithSwap()

        Assertions.assertEquals(VisaApiError.WithdrawalDataError.left(), result)
        coVerify(exactly = 0) { authDataSource.getWithdrawalSignature(any(), any()) }
    }

    @Test
    fun `GIVEN withdrawal signature is null WHEN withdrawWithSwap THEN return SignWithdrawError`() = runTest {
        coEvery { authDataSource.getWithdrawalSignature(any(), any()) } returns RuntimeException("error").left()

        val result = createRepository().withdrawWithSwap()

        Assertions.assertEquals(VisaApiError.SignWithdrawError.left(), result)
        coVerify(exactly = 0) { tangemPayApi.withdraw(any(), any()) }
    }

    @Test
    fun `GIVEN withdrawal signature is Cancelled WHEN withdrawWithSwap THEN return Cancelled`() = runTest {
        coEvery { authDataSource.getWithdrawalSignature(any(), any()) } returns WithdrawalSignatureResult.Cancelled.right()

        val result = createRepository().withdrawWithSwap()

        Assertions.assertEquals(WithdrawalResult.Cancelled.right(), result)
        coVerify(exactly = 0) { tangemPayApi.withdraw(any(), any()) }
    }

    @Test
    fun `GIVEN withdraw returns error WHEN withdrawWithSwap THEN return WithdrawError`() = runTest {
        coEvery { tangemPayApi.withdraw(any(), any()) } returns
            ApiResponse.Error(ApiResponseError.NetworkException()) as ApiResponse<WithdrawResponse>

        val result = createRepository().withdrawWithSwap()

        Assertions.assertEquals(VisaApiError.WithdrawError.left(), result)
        coVerify(exactly = 0) { orderRepository.getOrderData(any(), any()) }
    }

    @Test
    fun `GIVEN no txHash on initial check WHEN withdrawWithSwap THEN order is stored with its creation time`() =
        runTest {
            // Arrange
            coEvery { orderRepository.getOrderData(any(), any()) } returns orderWithoutHash.right()

            // Act
            createRepository().withdrawWithSwap()

            // Assert
            coVerify { tangemPayStorage.storeWithdrawOrder(userWalletId, storedOrder(txHash = null)) }
        }

    @Test
    fun `GIVEN no txHash WHEN withdrawWithSwap THEN order is polled every 3s for 30s and every 5s after that`() =
        runTest {
            // Arrange
            val requestTimes = mutableListOf<Long>()
            coEvery { orderRepository.getOrderData(any(), any()) } answers {
                requestTimes += testScheduler.currentTime
                orderWithoutHash.right()
            }

            // Act
            createRepository().withdrawWithSwap()
            advanceUntilIdle()

            // Assert
            val fastPhaseTimes = (0L..30_000L step 3_000L).toList()
            val slowPhaseTimes = (35_000L..POLLING_WINDOW_MILLIS step 5_000L).toList()
            assertThat(requestTimes).containsExactlyElementsIn(fastPhaseTimes + slowPhaseTimes).inOrder()
        }

    @Test
    fun `GIVEN no txHash within five minutes WHEN withdrawWithSwap THEN order is deleted at five minutes`() = runTest {
        // Arrange
        var deletedAt: Long? = null
        coEvery { orderRepository.getOrderData(any(), any()) } returns orderWithoutHash.right()
        coEvery { tangemPayStorage.deleteWithdrawOrder(userWalletId, ORDER_ID) } answers {
            deletedAt = testScheduler.currentTime
        }

        // Act
        val result = createRepository().withdrawWithSwap()
        advanceUntilIdle()

        // Assert
        assertThat(result).isEqualTo(WithdrawalResult.Success.right())
        assertThat(deletedAt).isEqualTo(POLLING_WINDOW_MILLIS)
        coVerify(exactly = 0) { swapRepository.exchangeSent(any(), any(), any(), any(), any(), any(), any()) }
    }

    @Test
    fun `GIVEN order requests fail WHEN withdrawWithSwap THEN order is kept until txHash arrives`() = runTest {
        // Arrange
        val failedRequest: Either<VisaApiError, OrderData> = VisaApiError.WithdrawError.left()
        coEvery { orderRepository.getOrderData(any(), any()) } returnsMany
            List(size = 20) { failedRequest } + listOf(orderWithHash.right())

        // Act
        createRepository().withdrawWithSwap()
        advanceUntilIdle()

        // Assert
        assertExchangeSent()
        coVerify(exactly = 1) { tangemPayStorage.deleteWithdrawOrder(userWalletId, ORDER_ID) }
    }

    @Test
    fun `GIVEN polling throws WHEN withdrawWithSwap THEN order is kept for the next attempt`() = runTest {
        // Arrange
        coEvery {
            orderRepository.getOrderData(any(), any())
        } returns orderWithoutHash.right() andThenThrows RuntimeException("boom")

        // Act
        val result = createRepository().withdrawWithSwap()
        advanceUntilIdle()

        // Assert
        assertThat(result).isEqualTo(WithdrawalResult.Success.right())
        coVerify(exactly = 2) { orderRepository.getOrderData(userWalletId, ORDER_ID) }
        coVerify(exactly = 0) { tangemPayStorage.deleteWithdrawOrder(any(), any()) }
        coVerify(exactly = 0) { swapRepository.exchangeSent(any(), any(), any(), any(), any(), any(), any()) }
    }

    @Test
    fun `GIVEN polling threw WHEN poll again THEN polling restarts for the kept order`() = runTest {
        // Arrange
        val repository = createRepository()
        coEvery { tangemPayStorage.getWithdrawOrders(userWalletId) } returns listOf(storedOrder(txHash = null))
        coEvery {
            orderRepository.getOrderData(any(), any())
        } returns orderWithoutHash.right() andThenThrows RuntimeException("boom") andThen orderWithHash.right()
        repository.withdrawWithSwap()
        advanceUntilIdle()

        // Act
        repository.pollWithdrawOrdersIfNeeds(userWallet)
        advanceUntilIdle()

        // Assert
        assertExchangeSent()
        coVerify(exactly = 1) { tangemPayStorage.deleteWithdrawOrder(userWalletId, ORDER_ID) }
    }

    @Test
    fun `GIVEN txHash appears at five minutes WHEN withdrawWithSwap THEN polling finalizes the withdrawal`() = runTest {
        // Arrange
        coEvery { orderRepository.getOrderData(any(), any()) } returnsMany
            List(size = POLLS_IN_WINDOW) { orderWithoutHash.right() } + listOf(orderWithHash.right())

        // Act
        val result = createRepository().withdrawWithSwap()
        advanceUntilIdle()

        // Assert
        assertThat(result).isEqualTo(WithdrawalResult.Success.right())
        coVerify(exactly = POLLS_IN_WINDOW + 1) { orderRepository.getOrderData(userWalletId, ORDER_ID) }
        assertExchangeSent()
        coVerify(exactly = 1) { tangemPayStorage.deleteWithdrawOrder(userWalletId, ORDER_ID) }
    }

    // endregion

    // region withdraw

    @Test
    fun `GIVEN withdrawal signature is Cancelled WHEN withdraw THEN return Cancelled`() = runTest {
        coEvery { authDataSource.getWithdrawalSignature(any(), any()) } returns WithdrawalSignatureResult.Cancelled.right()

        val result = createRepository().withdraw()

        Assertions.assertEquals(WithdrawalResult.Cancelled.right(), result)
        coVerify(exactly = 0) { tangemPayApi.withdraw(any(), any()) }
    }

    @Test
    fun `GIVEN withdraw succeeds WHEN withdraw THEN return Success`() = runTest {
        val result = createRepository().withdraw()

        Assertions.assertEquals(WithdrawalResult.Success.right(), result)
        coVerify { tangemPayApi.withdraw(any(), any()) }
    }

    @Test
    fun `GIVEN withdraw data describing the request WHEN withdraw THEN its hash is signed`() = runTest {
        // Act
        createRepository().withdraw()

        // Assert
        coVerify(exactly = 1) { authDataSource.getWithdrawalSignature(userWallet, withdrawData().hash) }
    }

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN withdraw data not describing the request WHEN withdraw THEN WithdrawalDataError and nothing signed`(
        data: WithdrawDataResponse.Result,
    ) = runTest {
        // Arrange
        coEvery { tangemPayApi.getWithdrawData(any(), any()) } returns ApiResponse.Success(
            WithdrawDataResponse(result = data),
        )

        // Act
        val result = createRepository().withdraw()

        // Assert
        Assertions.assertEquals(VisaApiError.WithdrawalDataError.left(), result)
        coVerify(exactly = 0) { authDataSource.getWithdrawalSignature(any(), any()) }
        coVerify(exactly = 0) { tangemPayApi.withdraw(any(), any()) }
    }

    @Test
    fun `GIVEN withdraw data to another recipient WHEN withdrawWithSwap THEN WithdrawalDataError and nothing signed`() =
        runTest {
            // Arrange
            coEvery { tangemPayApi.getWithdrawData(any(), any()) } returns ApiResponse.Success(
                WithdrawDataResponse(result = withdrawData(recipient = OTHER_ADDRESS)),
            )

            // Act
            val result = createRepository().withdrawWithSwap()

            // Assert
            Assertions.assertEquals(VisaApiError.WithdrawalDataError.left(), result)
            coVerify(exactly = 0) { authDataSource.getWithdrawalSignature(any(), any()) }
            coVerify(exactly = 0) { tangemPayApi.withdraw(any(), any()) }
        }

    // endregion

    // region hasWithdrawOrder

    @Test
    fun `GIVEN no active order id WHEN hasWithdrawOrder THEN return false`() = runTest {
        coEvery { tangemPayStorage.getActiveWithdrawOrderId(userWalletId) } returns null

        val result = createRepository().hasWithdrawOrder(userWalletId)

        Assertions.assertFalse(result)
        coVerify(exactly = 0) { orderRepository.getOrderData(any(), any()) }
    }

    @Test
    fun `GIVEN order is not active WHEN hasWithdrawOrder THEN delete active order and return false`() = runTest {
        coEvery { tangemPayStorage.getActiveWithdrawOrderId(userWalletId) } returns ORDER_ID
        coEvery {
            orderRepository.getOrderData(userWalletId, ORDER_ID)
        } returns orderWithoutHash.copy(status = OrderStatus.COMPLETED).right()

        val result = createRepository().hasWithdrawOrder(userWalletId)

        Assertions.assertFalse(result)
        coVerify { tangemPayStorage.deleteActiveWithdrawOrder(userWalletId) }
    }

    @Test
    fun `GIVEN order is active WHEN hasWithdrawOrder THEN return true and keep active order`() = runTest {
        coEvery { tangemPayStorage.getActiveWithdrawOrderId(userWalletId) } returns ORDER_ID
        coEvery {
            orderRepository.getOrderData(userWalletId, ORDER_ID)
        } returns orderWithoutHash.copy(status = OrderStatus.NEW).right()

        val result = createRepository().hasWithdrawOrder(userWalletId)

        Assertions.assertTrue(result)
        coVerify(exactly = 0) { tangemPayStorage.deleteActiveWithdrawOrder(userWalletId) }
    }

    // endregion

    // region pollWithdrawOrdersIfNeeds

    @Test
    fun `GIVEN order created two minutes ago WHEN poll THEN polling resumes every 5s until five minutes`() = runTest {
        // Arrange
        val requestTimes = mutableListOf<Long>()
        coEvery { tangemPayStorage.getWithdrawOrders(userWalletId) } returns listOf(
            storedOrder(txHash = null, createdAtMillis = START_MILLIS - 2.minutes.inWholeMilliseconds),
        )
        coEvery { orderRepository.getOrderData(any(), any()) } answers {
            requestTimes += testScheduler.currentTime
            orderWithoutHash.right()
        }

        // Act
        createRepository().pollWithdrawOrdersIfNeeds(userWallet)
        advanceUntilIdle()

        // Assert
        assertThat(requestTimes).containsExactlyElementsIn((5_000L..180_000L step 5_000L).toList()).inOrder()
        coVerify { tangemPayStorage.deleteWithdrawOrder(userWalletId, ORDER_ID) }
    }

    @Test
    fun `GIVEN expired order with txHash WHEN poll THEN a single request finalizes the withdrawal`() = runTest {
        // Arrange
        coEvery { tangemPayStorage.getWithdrawOrders(userWalletId) } returns listOf(expiredStoredOrder())
        coEvery { orderRepository.getOrderData(userWalletId, ORDER_ID) } returns orderWithHash.right()

        // Act
        createRepository().pollWithdrawOrdersIfNeeds(userWallet)
        advanceUntilIdle()

        // Assert
        coVerify(exactly = 1) { orderRepository.getOrderData(userWalletId, ORDER_ID) }
        assertExchangeSent()
        coVerify { tangemPayStorage.deleteWithdrawOrder(userWalletId, ORDER_ID) }
    }

    @ParameterizedTest
    @MethodSource("provideFinalResponsesWithoutHash")
    fun `GIVEN expired order without txHash WHEN poll THEN order is deleted after a single request`(
        response: Either<VisaApiError, OrderData>,
    ) = runTest {
        // Arrange
        coEvery { tangemPayStorage.getWithdrawOrders(userWalletId) } returns listOf(expiredStoredOrder())
        coEvery { orderRepository.getOrderData(userWalletId, ORDER_ID) } returns response

        // Act
        createRepository().pollWithdrawOrdersIfNeeds(userWallet)
        advanceUntilIdle()

        // Assert
        coVerify(exactly = 1) { orderRepository.getOrderData(userWalletId, ORDER_ID) }
        coVerify { tangemPayStorage.deleteWithdrawOrder(userWalletId, ORDER_ID) }
        coVerify(exactly = 0) { swapRepository.exchangeSent(any(), any(), any(), any(), any(), any(), any()) }
    }

    @Test
    fun `GIVEN order stored without creation time WHEN poll THEN it is treated as expired`() = runTest {
        // Arrange
        coEvery { tangemPayStorage.getWithdrawOrders(userWalletId) } returns
            listOf(storedOrder(txHash = null, createdAtMillis = 0L))
        coEvery { orderRepository.getOrderData(userWalletId, ORDER_ID) } returns orderWithoutHash.right()

        // Act
        createRepository().pollWithdrawOrdersIfNeeds(userWallet)
        advanceUntilIdle()

        // Assert
        coVerify(exactly = 1) { orderRepository.getOrderData(userWalletId, ORDER_ID) }
        coVerify { tangemPayStorage.deleteWithdrawOrder(userWalletId, ORDER_ID) }
    }

    @Test
    fun `GIVEN stored hash has value WHEN poll THEN finalize without fetching the order`() = runTest {
        coEvery { tangemPayStorage.getWithdrawOrders(userWalletId) } returns listOf(storedOrder(txHash = TX_HASH))

        createRepository().pollWithdrawOrdersIfNeeds(userWallet)
        advanceUntilIdle()

        coVerify(exactly = 0) { orderRepository.getOrderData(any(), any()) }
        assertExchangeSent()
        coVerify { tangemPayStorage.deleteWithdrawOrder(userWalletId, ORDER_ID) }
    }

    @Test
    fun `GIVEN two identical orders WHEN poll THEN only a single polling job runs for the same order`() = runTest {
        val duplicatedOrder = storedOrder(txHash = null)
        coEvery {
            tangemPayStorage.getWithdrawOrders(userWalletId)
        } returns listOf(duplicatedOrder, duplicatedOrder)
        coEvery { orderRepository.getOrderData(any(), any()) } returns orderWithoutHash.right()

        createRepository().pollWithdrawOrdersIfNeeds(userWallet)
        advanceUntilIdle()

        coVerify(exactly = POLLS_IN_WINDOW) { orderRepository.getOrderData(userWalletId, ORDER_ID) }
        coVerify { tangemPayStorage.deleteWithdrawOrder(userWalletId, ORDER_ID) }
    }

    // endregion

    private fun assertExchangeSent() {
        coVerify {
            swapRepository.exchangeSent(
                userWallet = userWallet,
                txId = exchangeData.txId,
                fromNetwork = exchangeData.fromNetwork,
                fromAddress = exchangeData.fromAddress,
                payInAddress = exchangeData.payInAddress,
                txHash = TX_HASH,
                payInExtraId = exchangeData.payInExtraId,
            )
        }
    }

    private fun storedOrder(txHash: String?, createdAtMillis: Long = START_MILLIS) = TangemPayWithdrawState(
        orderId = ORDER_ID,
        exchangeData = exchangeData,
        txHash = txHash,
        createdAtMillis = createdAtMillis,
    )

    private fun expiredStoredOrder() = storedOrder(
        txHash = null,
        createdAtMillis = START_MILLIS - 6.minutes.inWholeMilliseconds,
    )

    @Test
    fun `GIVEN source network is enabled WHEN withdrawWithSwap THEN chain id and contract are sent in both requests`() =
        runTest {
            // Arrange
            val dataRequest = slot<WithdrawDataRequest>()
            val withdrawRequest = slot<WithdrawRequest>()
            coEvery { tangemPayApi.getWithdrawData(any(), capture(dataRequest)) } returns ApiResponse.Success(
                WithdrawDataResponse(result = withdrawData()),
            )
            coEvery { tangemPayApi.withdraw(any(), capture(withdrawRequest)) } returns ApiResponse.Success(
                WithdrawResponse(
                    result = WithdrawResponse.Result(orderId = ORDER_ID, status = "NEW", type = "withdraw"),
                ),
            )
            coEvery { orderRepository.getOrderData(any(), any()) } returns orderWithHash.right()

            // Act
            createRepository().withdrawWithSwap()
            advanceUntilIdle()

            // Assert
            Assertions.assertEquals(CHAIN_ID, dataRequest.captured.chainId)
            Assertions.assertEquals(CONTRACT_ADDRESS, dataRequest.captured.tokenContractAddress)
            Assertions.assertEquals(CHAIN_ID, withdrawRequest.captured.chainId)
            Assertions.assertEquals(CONTRACT_ADDRESS, withdrawRequest.captured.tokenContractAddress)
        }

    @Test
    fun `GIVEN source network is missing from an issued account WHEN withdrawWithSwap THEN nothing is attempted`() =
        runTest {
            // Arrange
            every { paymentAccountStatusSupplier.invoke(userWalletId) } returns flowOf(
                MockAccounts.createPaymentAccountStatus(
                    userWalletId = userWalletId,
                    cryptoCurrency = sourceCurrency,
                    networks = listOf(
                        PaymentNetworkStatus.Available(
                            network = MockAccounts.createPaymentAccountToken(networkId = "tron").network,
                            depositAddress = "0xDeposit",
                            chainId = 728126428L,
                            cryptoCurrencyStatuses = emptyList(),
                        ),
                    ),
                ),
            )

            // Act
            val result = createRepository().withdrawWithSwap()

            // Assert
            Assertions.assertEquals(VisaApiError.WithdrawalDataError.left(), result)
            coVerify(exactly = 0) { tangemPayApi.getWithdrawData(any(), any()) }
        }

    @Test
    fun `GIVEN source network is enabled WHEN withdraw THEN chain id and contract are sent in both requests`() =
        runTest {
            // Arrange
            val dataRequest = slot<WithdrawDataRequest>()
            val withdrawRequest = slot<WithdrawRequest>()
            coEvery { tangemPayApi.getWithdrawData(any(), capture(dataRequest)) } returns ApiResponse.Success(
                WithdrawDataResponse(result = withdrawData()),
            )
            coEvery { tangemPayApi.withdraw(any(), capture(withdrawRequest)) } returns ApiResponse.Success(
                WithdrawResponse(
                    result = WithdrawResponse.Result(orderId = ORDER_ID, status = "NEW", type = "withdraw"),
                ),
            )
            coEvery { orderRepository.getOrderData(any(), any()) } returns orderWithHash.right()

            // Act
            createRepository().withdraw()
            advanceUntilIdle()

            // Assert
            Assertions.assertEquals(CHAIN_ID, dataRequest.captured.chainId)
            Assertions.assertEquals(CONTRACT_ADDRESS, dataRequest.captured.tokenContractAddress)
            Assertions.assertEquals(CHAIN_ID, withdrawRequest.captured.chainId)
            Assertions.assertEquals(CONTRACT_ADDRESS, withdrawRequest.captured.tokenContractAddress)
        }

    @Test
    fun `GIVEN account without issued networks WHEN withdraw THEN backend picks the default network`() =
        runTest {
            // Arrange — a pre-multichain account: its only network is the one the backend defaults to.
            every { paymentAccountStatusSupplier.invoke(userWalletId) } returns flowOf(
                MockAccounts.createPaymentAccountStatus(
                    userWalletId = userWalletId,
                    cryptoCurrency = sourceCurrency,
                    networks = emptyList(),
                ),
            )
            val dataRequest = slot<WithdrawDataRequest>()
            coEvery { tangemPayApi.getWithdrawData(any(), capture(dataRequest)) } returns ApiResponse.Success(
                WithdrawDataResponse(result = withdrawData()),
            )
            coEvery { orderRepository.getOrderData(any(), any()) } returns orderWithHash.right()

            // Act
            createRepository().withdraw()
            advanceUntilIdle()

            // Assert
            Assertions.assertEquals(null, dataRequest.captured.chainId)
            Assertions.assertEquals(null, dataRequest.captured.tokenContractAddress)
        }

    @Test
    fun `GIVEN source network is missing from an issued account WHEN withdraw THEN no withdrawal is attempted`() =
        runTest {
            // Arrange — the account has issued networks, but none matches the source: withdrawing anyway would
            // debit whatever network the backend defaults to.
            every { paymentAccountStatusSupplier.invoke(userWalletId) } returns flowOf(
                MockAccounts.createPaymentAccountStatus(
                    userWalletId = userWalletId,
                    cryptoCurrency = sourceCurrency,
                    networks = listOf(
                        PaymentNetworkStatus.Available(
                            network = MockAccounts.createPaymentAccountToken(networkId = "tron").network,
                            depositAddress = "0xDeposit",
                            chainId = 728126428L,
                            cryptoCurrencyStatuses = emptyList(),
                        ),
                    ),
                ),
            )

            // Act
            val result = createRepository().withdraw()

            // Assert
            Assertions.assertEquals(VisaApiError.WithdrawalDataError.left(), result)
            coVerify(exactly = 0) { tangemPayApi.getWithdrawData(any(), any()) }
        }

    private suspend fun DefaultTangemPayWithdrawRepository.withdrawWithSwap() = withdrawWithSwap(
        userWallet = userWallet,
        receiverAddress = RECEIVER_ADDRESS,
        cryptoAmount = BigDecimal("1.5"),
        sourceCurrency = sourceCurrency,
        exchangeData = exchangeData,
    )

    private suspend fun DefaultTangemPayWithdrawRepository.withdraw() = withdraw(
        userWallet = userWallet,
        receiverAddress = RECEIVER_ADDRESS,
        cryptoAmount = BigDecimal("1.5"),
        sourceCurrency = sourceCurrency,
    )

    private fun TestScope.createRepository() = DefaultTangemPayWithdrawRepository(
        tangemPayApi = tangemPayApi,
        requestHelper = requestHelper,
        authDataSource = authDataSource,
        quotesFetcher = quotesFetcher,
        tangemPayStorage = tangemPayStorage,
        swapRepository = swapRepository,
        orderRepository = orderRepository,
        withdrawSourceResolver = WithdrawSourceResolver(paymentAccountStatusSupplier),
        withdrawPollingScope = TestAppCoroutineScope(this),
        currentTimeMillis = { START_MILLIS + testScheduler.currentTime },
    )

    companion object {
        private const val START_MILLIS = 1_700_000_000_000L
        private const val POLLING_WINDOW_MILLIS = 300_000L
        private const val POLLS_IN_WINDOW = 64
        private const val CURRENCY_ID = "ethereum"
        private const val ORDER_ID = "order-1"
        private const val TX_HASH = "0xTxHash"
        private const val SIGNATURE = "0xSignature"
        private const val AUTH_HEADER = "auth-header"
        private const val RECEIVER_ADDRESS = "0x059dbd5f24a000d28f83268db1c85c9c3ab6a123"
        private const val CONTRACT_ADDRESS = "0x3c499c542cEF5E3811e1192ce70d8cC03d5c3359"
        private const val OTHER_ADDRESS = "0xfde4C96c8593536E31F229EA8f37b2ADa2699bb2"
        private const val CHAIN_ID = 137L
        private const val TOKEN_AMOUNT = "1500000"

        @JvmStatic
        fun provideTestModels() = listOf(
            withdrawData(amount = "1600000"),
            withdrawData(recipient = OTHER_ADDRESS),
            withdrawData(chainId = 1L),
            withdrawData(asset = OTHER_ADDRESS),
            withdrawData().copy(hash = "0xdeadbeef"),
            withdrawData().copy(structuredData = null),
        )

        @JvmStatic
        fun provideFinalResponsesWithoutHash(): List<Either<VisaApiError, OrderData>> = listOf(
            VisaApiError.WithdrawError.left(),
            OrderData(customerId = "customer", status = OrderStatus.PROCESSING, withdrawTxHash = null).right(),
        )

        private fun withdrawData(
            chainId: Long = CHAIN_ID,
            asset: String = CONTRACT_ADDRESS,
            recipient: String = RECEIVER_ADDRESS,
            amount: String = TOKEN_AMOUNT,
        ): WithdrawDataResponse.Result {
            val structuredData = """
                {
                  "types": {
                    "EIP712Domain": [
                      {"name": "name", "type": "string"},
                      {"name": "chainId", "type": "uint256"}
                    ],
                    "Withdraw": [
                      {"name": "asset", "type": "address"},
                      {"name": "recipient", "type": "address"},
                      {"name": "amount", "type": "uint256"}
                    ]
                  },
                  "primaryType": "Withdraw",
                  "domain": {"name": "Collateral", "chainId": $chainId},
                  "message": {"asset": "$asset", "recipient": "$recipient", "amount": "$amount"}
                }
            """.trimIndent()
            return WithdrawDataResponse.Result(
                hash = "0x" + StructuredDataEncoder(structuredData).hashStructuredData().toHexString(),
                salt = "salt",
                senderAddress = "sender",
                structuredData = structuredData,
            )
        }
    }
}