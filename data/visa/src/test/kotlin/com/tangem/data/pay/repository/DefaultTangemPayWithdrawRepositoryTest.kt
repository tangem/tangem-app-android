package com.tangem.data.pay.repository

import arrow.core.left
import arrow.core.right
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
import java.math.BigDecimal

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
            WithdrawDataResponse(
                result = WithdrawDataResponse.Result(hash = "hash", salt = "salt", senderAddress = "sender"),
            ),
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
    fun `GIVEN no txHash on every attempt WHEN withdrawWithSwap THEN polling deletes order after max attempts`() =
        runTest {
            coEvery { orderRepository.getOrderData(any(), any()) } returns orderWithoutHash.right()

            val result = createRepository().withdrawWithSwap()
            advanceUntilIdle()

            Assertions.assertEquals(WithdrawalResult.Success.right(), result)
            // 1 initial check + MAX_POLLING_ATTEMPTS (6) polling attempts.
            coVerify(exactly = 7) { orderRepository.getOrderData(userWalletId, ORDER_ID) }
            coVerify { tangemPayStorage.deleteWithdrawOrder(userWalletId, ORDER_ID) }
            coVerify(exactly = 0) { swapRepository.exchangeSent(any(), any(), any(), any(), any(), any(), any()) }
        }

    @Test
    fun `GIVEN getOrderData throws while polling WHEN withdrawWithSwap THEN polling deletes order`() = runTest {
        coEvery {
            orderRepository.getOrderData(any(), any())
        } returns orderWithoutHash.right() andThenThrows RuntimeException("boom")

        val result = createRepository().withdrawWithSwap()
        advanceUntilIdle()

        Assertions.assertEquals(WithdrawalResult.Success.right(), result)
        coVerify { tangemPayStorage.deleteWithdrawOrder(userWalletId, ORDER_ID) }
        coVerify(exactly = 0) { swapRepository.exchangeSent(any(), any(), any(), any(), any(), any(), any()) }
    }

    @Test
    fun `GIVEN txHash appears on the last attempt WHEN withdrawWithSwap THEN polling finalizes the withdrawal`() =
        runTest {
            // index 0 = initial check, 1..5 = polling attempts 1-5, 6 = polling attempt 6 (last) returns the hash.
            coEvery { orderRepository.getOrderData(any(), any()) } returnsMany
                List(size = 6) { orderWithoutHash.right() } + listOf(orderWithHash.right())

            val result = createRepository().withdrawWithSwap()
            advanceUntilIdle()

            Assertions.assertEquals(WithdrawalResult.Success.right(), result)
            assertExchangeSent()
            coVerify { tangemPayStorage.deleteWithdrawOrder(userWalletId, ORDER_ID) }
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
    fun `GIVEN stored hash is null and order hash appears on third attempt WHEN poll THEN finalize the withdrawal`() =
        runTest {
            coEvery { tangemPayStorage.getWithdrawOrders(userWalletId) } returns listOf(storedOrder(txHash = null))
            // index 0 = initial fetch, 1..2 = polling attempts 1-2, 3 = polling attempt 3 returns the hash.
            coEvery { orderRepository.getOrderData(any(), any()) } returnsMany
                List(size = 3) { orderWithoutHash.right() } + listOf(orderWithHash.right())

            createRepository().pollWithdrawOrdersIfNeeds(userWallet)
            advanceUntilIdle()

            coVerify(exactly = 4) { orderRepository.getOrderData(userWalletId, ORDER_ID) }
            assertExchangeSent()
            coVerify { tangemPayStorage.deleteWithdrawOrder(userWalletId, ORDER_ID) }
        }

    @Test
    fun `GIVEN stored hash is null and order already has hash WHEN poll THEN finalize without polling`() = runTest {
        coEvery { tangemPayStorage.getWithdrawOrders(userWalletId) } returns listOf(storedOrder(txHash = null))
        coEvery { orderRepository.getOrderData(userWalletId, ORDER_ID) } returns orderWithHash.right()

        createRepository().pollWithdrawOrdersIfNeeds(userWallet)
        advanceUntilIdle()

        coVerify(exactly = 1) { orderRepository.getOrderData(userWalletId, ORDER_ID) }
        assertExchangeSent()
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

        // 2 initial fetches (one per order) + a single deduplicated polling job of 6 attempts = 8.
        coVerify(exactly = 8) { orderRepository.getOrderData(userWalletId, ORDER_ID) }
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

    private fun storedOrder(txHash: String?) = TangemPayWithdrawState(
        orderId = ORDER_ID,
        exchangeData = exchangeData,
        txHash = txHash,
    )

    @Test
    fun `GIVEN source network is enabled WHEN withdrawWithSwap THEN chain id and contract are sent in both requests`() =
        runTest {
            // Arrange
            val dataRequest = slot<WithdrawDataRequest>()
            val withdrawRequest = slot<WithdrawRequest>()
            coEvery { tangemPayApi.getWithdrawData(any(), capture(dataRequest)) } returns ApiResponse.Success(
                WithdrawDataResponse(
                    result = WithdrawDataResponse.Result(hash = "hash", salt = "salt", senderAddress = "sender"),
                ),
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
                WithdrawDataResponse(
                    result = WithdrawDataResponse.Result(hash = "hash", salt = "salt", senderAddress = "sender"),
                ),
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
                WithdrawDataResponse(
                    result = WithdrawDataResponse.Result(hash = "hash", salt = "salt", senderAddress = "sender"),
                ),
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
    )

    private companion object {
        const val CURRENCY_ID = "ethereum"
        const val ORDER_ID = "order-1"
        const val TX_HASH = "0xTxHash"
        const val SIGNATURE = "0xSignature"
        const val AUTH_HEADER = "auth-header"
        const val RECEIVER_ADDRESS = "0xReceiver"
        const val CONTRACT_ADDRESS = "0xContract"
        const val CHAIN_ID = 137L
    }
}