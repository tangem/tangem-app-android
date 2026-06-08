package com.tangem.domain.transaction.usecase.gasless

import com.tangem.blockchain.blockchains.ethereum.EthereumWalletManager
import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.BlockchainSdkError
import com.tangem.blockchain.common.Token
import com.tangem.blockchain.common.TransactionData
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.blockchain.extensions.Result
import com.tangem.domain.demo.models.DemoConfig
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.models.yield.supply.YieldSupplyStatus
import com.tangem.domain.transaction.GaslessTransactionRepository
import com.tangem.domain.transaction.error.GetFeeError
import com.tangem.domain.walletmanager.WalletManagersFacade
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.math.BigInteger

/**
 * Unit tests for [TokenFeeCalculator].
 * Tests cover fee calculation logic, error handling, and edge cases.
 */
class TokenFeeCalculatorTest {

    private lateinit var walletManagersFacade: WalletManagersFacade
    private lateinit var gaslessTransactionRepository: GaslessTransactionRepository
    private lateinit var demoConfig: DemoConfig
    private lateinit var tokenFeeCalculator: TokenFeeCalculator

    private lateinit var mockWalletManager: EthereumWalletManager
    private lateinit var mockNetwork: Network
    private lateinit var mockUserWallet: UserWallet
    private lateinit var mockUserWalletId: UserWalletId
    private lateinit var mockTransactionData: TransactionData

    @BeforeEach
    fun setup() {
        walletManagersFacade = mockk()
        gaslessTransactionRepository = mockk()
        demoConfig = mockk()

        tokenFeeCalculator = TokenFeeCalculator(
            walletManagersFacade = walletManagersFacade,
            gaslessTransactionRepository = gaslessTransactionRepository,
            demoConfig = demoConfig,
        )

        mockWalletManager = mockk()
        mockNetwork = mockk()
        mockUserWallet = mockk<UserWallet.Hot>()
        mockUserWalletId = mockk()
        mockTransactionData = mockk()


        // Default mock behavior
        every { demoConfig.isDemoCardId(any()) } returns false
        every { mockUserWallet.walletId } returns mockUserWalletId
    }

    // ===== calculateInitialFee Tests =====

    @Test
    fun `calculateInitialFee should return success when getFee succeeds`() = runTest {
        // Given
        val expectedFee = createMockTransactionFee()
        coEvery { mockWalletManager.getFee(mockTransactionData) } returns Result.Success(expectedFee)

        // When
        val result = tokenFeeCalculator.calculateInitialFee(
            userWallet = mockUserWallet,
            network = mockNetwork,
            walletManager = mockWalletManager,
            transactionData = mockTransactionData,
        )

        // Then
        assertTrue(result.isRight())
        result.onRight { fee ->
            assertEquals(expectedFee, fee)
        }
        coVerify { mockWalletManager.getFee(mockTransactionData) }
    }

    @Test
    fun `calculateInitialFee should return error when getFee fails`() = runTest {
        // Given
        val failure = Result.Failure(BlockchainSdkError.Ethereum.Api(1, "Failed to get fee"))
        coEvery { mockWalletManager.getFee(mockTransactionData) } returns failure

        // When
        val result = tokenFeeCalculator.calculateInitialFee(
            userWallet = mockUserWallet,
            network = mockNetwork,
            walletManager = mockWalletManager,
            transactionData = mockTransactionData,
        )

        // Then
        assertTrue(result.isLeft())
        result.onLeft { error ->
            assertTrue(error is GetFeeError.GaslessError)
        }
    }

    // ===== estimateInitialFee Tests =====

    @Test
    fun `estimateInitialFee should return success when estimation succeeds`() = runTest {
        // Given
        val amount = BigDecimal("100")
        val tokenStatus = createMockTokenStatus()
        val expectedFee = createMockTransactionFee()

        coEvery { walletManagersFacade.estimateFee(any(), any(), any()) } returns Result.Success(expectedFee)

        // When
        val result = tokenFeeCalculator.estimateInitialFee(
            userWallet = mockUserWallet,
            amount = amount,
            txTokenCurrencyStatus = tokenStatus,
        )

        // Then
        assertTrue(result.isRight())
        result.onRight { fee ->
            assertEquals(expectedFee, fee)
        }
    }

    @Test
    fun `estimateInitialFee should return error when estimation fails`() = runTest {
        // Given
        val amount = BigDecimal("100")
        val tokenStatus = createMockTokenStatus()
        val failure = Result.Failure(BlockchainSdkError.NPError("Estimation failed"))

        coEvery {
            walletManagersFacade.estimateFee(any(), any(), any())
        } returns failure

        // When
        val result = tokenFeeCalculator.estimateInitialFee(
            userWallet = mockUserWallet,
            amount = amount,
            txTokenCurrencyStatus = tokenStatus,
        )

        // Then
        assertTrue(result.isLeft())
    }

    @Test
    fun `estimateInitialFee should return error when result is null`() = runTest {
        // Given
        val amount = BigDecimal("100")
        val tokenStatus = createMockTokenStatus()

        coEvery { walletManagersFacade.estimateFee(any(), any(), any()) } returns null

        // When
        val result = tokenFeeCalculator.estimateInitialFee(
            userWallet = mockUserWallet,
            amount = amount,
            txTokenCurrencyStatus = tokenStatus,
        )

        // Then
        assertTrue(result.isLeft())
        result.onLeft { error ->
            assertTrue(error is GetFeeError.UnknownError)
        }
    }

    // ===== calculateTokenFee Tests =====

    @Test
    fun `calculateTokenFee should calculate correct fee for token payment`() = runTest {
        // Given
        val tokenStatus = createMockTokenStatus(
            balance = BigDecimal("1000"),
            fiatRate = BigDecimal("1"), // 1 USDC = 1 USD
        )
        val nativeStatus = createMockNativeCurrencyStatus(
            fiatRate = BigDecimal("2000"), // 1 ETH = 2000 USD
        )
        val initialFee = createMockEIP1559Fee()

        coEvery { mockWalletManager.getGasLimit(any(), any(), any()) } returns Result.Success(BigInteger("60000"))
        coEvery { gaslessTransactionRepository.getTokenFeeReceiverAddress() } returns "0xFeeReceiver"
        every { gaslessTransactionRepository.getBaseGasForTransaction() } returns BigInteger("21000")

        // When
        val result = tokenFeeCalculator.calculateTokenFee(
            walletManager = mockWalletManager,
            tokenForPayFeeStatus = tokenStatus,
            nativeCurrencyStatus = nativeStatus,
            initialFee = initialFee,
        )

        // Then
        assertTrue(result.isRight())
        result.onRight { feeExtended ->
            assertNotNull(feeExtended)
            assertEquals(tokenStatus.currency.id, feeExtended.feeTokenId)
            assertTrue(feeExtended.transactionFee is TransactionFee.Single)
        }
    }

    @Test
    fun `calculateTokenFee should return error when token balance is insufficient`() = runTest {
        // Given
        val tokenStatus = createMockTokenStatus(
            balance = BigDecimal("0.001"), // Very small balance
            fiatRate = BigDecimal("1"),
        )
        val nativeStatus = createMockNativeCurrencyStatus(
            fiatRate = BigDecimal("2000"),
        )
        val initialFee = createMockEIP1559Fee()

        coEvery { mockWalletManager.getGasLimit(any(), any(), any()) } returns Result.Success(BigInteger("60000"))
        coEvery { gaslessTransactionRepository.getTokenFeeReceiverAddress() } returns "0xFeeReceiver"
        every { gaslessTransactionRepository.getBaseGasForTransaction() } returns BigInteger("21000")

        // When
        val result = tokenFeeCalculator.calculateTokenFee(
            walletManager = mockWalletManager,
            tokenForPayFeeStatus = tokenStatus,
            nativeCurrencyStatus = nativeStatus,
            initialFee = initialFee,
        )

        // Then
        assertTrue(result.isLeft())
        result.onLeft { error ->
            assertTrue(error is GetFeeError.GaslessError.NotEnoughFunds)
        }
    }

    @Test
    fun `calculateTokenFee should return error when fiatRate is null`() = runTest {
        coEvery { mockWalletManager.getGasLimit(any(), any(), any()) } returns Result.Success(BigInteger("60000"))
        coEvery { gaslessTransactionRepository.getTokenFeeReceiverAddress() } returns "0xFeeReceiver"
        every { gaslessTransactionRepository.getBaseGasForTransaction() } returns BigInteger("21000")
        // Given
        val tokenStatus = createMockTokenStatus(
            balance = BigDecimal("1000"),
            fiatRate = null, // No fiat rate
        )
        val nativeStatus = createMockNativeCurrencyStatus(
            fiatRate = BigDecimal("2000"),
        )
        val initialFee = createMockEIP1559Fee()

        // When
        val result = tokenFeeCalculator.calculateTokenFee(
            walletManager = mockWalletManager,
            tokenForPayFeeStatus = tokenStatus,
            nativeCurrencyStatus = nativeStatus,
            initialFee = initialFee,
        )

        // Then
        assertTrue(result.isLeft())
    }

    @Test
    fun `calculateTokenFee should return error when getGasLimit fails`() = runTest {
        // Given
        val tokenStatus = createMockTokenStatus()
        val nativeStatus = createMockNativeCurrencyStatus()
        val initialFee = createMockEIP1559Fee()

        val failure = Result.Failure(BlockchainSdkError.NPError("Gas limit fetch failed"))
        coEvery { mockWalletManager.getGasLimit(any(), any(), any()) } returns failure
        coEvery { gaslessTransactionRepository.getTokenFeeReceiverAddress() } returns "0xFeeReceiver"

        // When
        val result = tokenFeeCalculator.calculateTokenFee(
            walletManager = mockWalletManager,
            tokenForPayFeeStatus = tokenStatus,
            nativeCurrencyStatus = nativeStatus,
            initialFee = initialFee,
        )

        // Then
        assertTrue(result.isLeft())
        result.onLeft { error ->
            assertTrue(error is GetFeeError.GaslessError.DataError)
        }
    }

    @Test
    fun `calculateTokenFee should handle Legacy fee type correctly`() = runTest {
        // Given
        val tokenStatus = createMockTokenStatus()
        val nativeStatus = createMockNativeCurrencyStatus()
        val initialFee = Fee.Ethereum.Legacy(
            amount = mockk(relaxed = true),
            gasLimit = BigInteger("100000"),
            gasPrice = BigInteger("50000000000"), // 50 Gwei
        )

        coEvery { mockWalletManager.getGasLimit(any(), any(), any()) } returns Result.Success(BigInteger("60000"))
        coEvery { gaslessTransactionRepository.getTokenFeeReceiverAddress() } returns "0xFeeReceiver"
        every { gaslessTransactionRepository.getBaseGasForTransaction() } returns BigInteger("21000")

        // When
        val result = tokenFeeCalculator.calculateTokenFee(
            walletManager = mockWalletManager,
            tokenForPayFeeStatus = tokenStatus,
            nativeCurrencyStatus = nativeStatus,
            initialFee = initialFee,
        )

        // Then
        assertTrue(result.isRight())
    }

    @Test
    fun `calculateTokenFee should reject TokenCurrency as initial fee`() = runTest {
        coEvery { mockWalletManager.getGasLimit(any(), any(), any()) } returns Result.Success(BigInteger("60000"))
        coEvery { gaslessTransactionRepository.getTokenFeeReceiverAddress() } returns "0xFeeReceiver"
        every { gaslessTransactionRepository.getBaseGasForTransaction() } returns BigInteger("21000")

        // Given
        val tokenStatus = createMockTokenStatus()
        val nativeStatus = createMockNativeCurrencyStatus()
        val initialFee = Fee.Ethereum.TokenCurrency(
            amount = mockk(relaxed = true),
            gasLimit = BigInteger("100000"),
            coinPriceInToken = BigInteger("1000"),
            feeTransferGasLimit = BigInteger("60000"),
            baseGas = BigInteger("21000"),
        )

        // When
        val result = tokenFeeCalculator.calculateTokenFee(
            walletManager = mockWalletManager,
            tokenForPayFeeStatus = tokenStatus,
            nativeCurrencyStatus = nativeStatus,
            initialFee = initialFee,
        )

        // Then
        assertTrue(result.isLeft())
    }

    @Test
    fun `calculateTokenFee should apply 1 percent increase to token price`() = runTest {
        // Expected

        val expectedAmount = Amount(
            value = BigDecimal("28.050000000000000000000000000000000000"),
            token = Token(
                name = "USDC",
                symbol = "USDC",
                contractAddress = "0xUSDC",
                decimals = 6,
            )
        )
        val expectedGasLimit = "187000".toBigInteger()
        val expectedCoinPriceInToken = BigInteger("2020000000") // 2000 * 1.01 * 10^6
        val expectedFeeTransferLimit = "66000".toBigInteger()
        val expectedBaseGas = "21000".toBigInteger()

        // Given
        val tokenStatus = createMockTokenStatus(
            balance = BigDecimal("1000"),
            fiatRate = BigDecimal("1"),
            decimals = 6,
        )
        val nativeStatus = createMockNativeCurrencyStatus(
            fiatRate = BigDecimal("2000"),
            decimals = 18,
        )
        val initialFee = createMockEIP1559Fee()

        coEvery { mockWalletManager.getGasLimit(any(), any(), any()) } returns Result.Success(BigInteger("60000"))
        coEvery { gaslessTransactionRepository.getTokenFeeReceiverAddress() } returns "0xFeeReceiver"
        every { gaslessTransactionRepository.getBaseGasForTransaction() } returns BigInteger("21000")

        // When
        val result = tokenFeeCalculator.calculateTokenFee(
            walletManager = mockWalletManager,
            tokenForPayFeeStatus = tokenStatus,
            nativeCurrencyStatus = nativeStatus,
            initialFee = initialFee,
        )

        // Then
        assertTrue(result.isRight())
        result.onRight { feeExtended ->
            val fee = feeExtended.transactionFee.normal as Fee.Ethereum.TokenCurrency

            assertEquals(expectedAmount, fee.amount)
            assertEquals(expectedGasLimit, fee.gasLimit)
            assertEquals(expectedCoinPriceInToken, fee.coinPriceInToken)
            assertEquals(expectedFeeTransferLimit, fee.feeTransferGasLimit)
            assertEquals(expectedBaseGas, fee.baseGas)
        }
    }

    // ===== Yield-path Tests =====

    /**
     * With active yield, a token whose plain balance is small (not enough to pay the fee on its own) must NOT
     * raise NotEnoughFunds — the resolver decides coverage. The gas limit must include the extra WITHDRAW_GAS_LIMIT.
     *
     * Expected gasLimit breakdown (matching companion constants):
     *   initialFee.gasLimit  = 100_000
     *   feeTransferGasLimit  = 60_000 * 1.10 = 66_000
     *   baseGas              = 21_000
     *   WITHDRAW_GAS_LIMIT   = 150_000
     *   total                = 337_000
     */
    @Test
    fun `calculateTokenFee with active yield does not raise on insufficient plain balance`() = runTest {
        // Given
        val activeYieldStatus = YieldSupplyStatus(
            isActive = true,
            isInitialized = true,
            isAllowedToSpend = true,
            effectiveProtocolBalance = BigDecimal("100"), // yield covers the rest
        )
        val tokenStatus = createMockTokenStatus(
            balance = BigDecimal("0.001"), // tiny plain balance — insufficient on its own
            fiatRate = BigDecimal("1"),
        ).withYieldSupplyStatus(activeYieldStatus)

        val nativeStatus = createMockNativeCurrencyStatus(fiatRate = BigDecimal("2000"))
        val initialFee = createMockEIP1559Fee() // gasLimit = 100_000

        coEvery { mockWalletManager.getGasLimit(any(), any(), any()) } returns Result.Success(BigInteger("60000"))
        coEvery { gaslessTransactionRepository.getTokenFeeReceiverAddress() } returns "0xFeeReceiver"
        every { gaslessTransactionRepository.getBaseGasForTransaction() } returns BigInteger("21000")

        // When
        val result = tokenFeeCalculator.calculateTokenFee(
            walletManager = mockWalletManager,
            tokenForPayFeeStatus = tokenStatus,
            nativeCurrencyStatus = nativeStatus,
            initialFee = initialFee,
            isYieldActive = true,
        )

        // Then
        assertTrue("Expected success on yield path with small plain balance", result.isRight())
        result.onRight { feeExtended ->
            val fee = feeExtended.transactionFee.normal as Fee.Ethereum.TokenCurrency
            // gasLimit = 100_000 + 66_000 + 21_000 + 150_000 = 337_000
            assertEquals("gasLimit must include WITHDRAW_GAS_LIMIT (150000)", BigInteger("337000"), fee.gasLimit)
            // feeTransferGasLimit stored in the fee object = 66_000
            assertEquals("feeTransferGasLimit = 60000 * 1.10", BigInteger("66000"), fee.feeTransferGasLimit)
        }
    }

    /**
     * With active yield, when getGasLimit reverts due to zero plain balance
     * (BlockchainSdkError.Ethereum.InsufficientFundsForOperation wrapped in WrappedThrowable),
     * calculateTokenFee must use the deterministic FALLBACK_FEE_TRANSFER_GAS_LIMIT (100_000) instead of raising.
     *
     * Expected breakdown:
     *   initialFee.gasLimit     = 100_000
     *   feeTransferGasLimit     = 100_000 * 1.10 = 110_000   (FALLBACK_FEE_TRANSFER_GAS_LIMIT * 1.10)
     *   baseGas                 = 21_000
     *   WITHDRAW_GAS_LIMIT      = 150_000
     *   total gasLimit          = 381_000
     */
    @Test
    fun `calculateTokenFee with active yield uses fallback gas when transfer estimation reverts with insufficient funds`() =
        runTest {
            // Given
            val activeYieldStatus = YieldSupplyStatus(
                isActive = true,
                isInitialized = true,
                isAllowedToSpend = true,
                effectiveProtocolBalance = BigDecimal("100"),
            )
            // Zero plain balance — exactly the condition that causes estimation revert
            val tokenStatus = createMockTokenStatus(
                balance = BigDecimal("0"),
                fiatRate = BigDecimal("1"),
            ).withYieldSupplyStatus(activeYieldStatus)

            val nativeStatus = createMockNativeCurrencyStatus(fiatRate = BigDecimal("2000"))
            val initialFee = createMockEIP1559Fee() // gasLimit = 100_000

            // Simulate on-chain estimation reverting with InsufficientFundsForOperation
            val insufficientFundsException =
                BlockchainSdkError.Ethereum.InsufficientFundsForOperation("insufficient funds for gas")
            val wrappedError = BlockchainSdkError.WrappedThrowable(insufficientFundsException)
            coEvery { mockWalletManager.getGasLimit(any(), any(), any()) } returns Result.Failure(wrappedError)
            coEvery { gaslessTransactionRepository.getTokenFeeReceiverAddress() } returns "0xFeeReceiver"
            every { gaslessTransactionRepository.getBaseGasForTransaction() } returns BigInteger("21000")

            // When
            val result = tokenFeeCalculator.calculateTokenFee(
                walletManager = mockWalletManager,
                tokenForPayFeeStatus = tokenStatus,
                nativeCurrencyStatus = nativeStatus,
                initialFee = initialFee,
                isYieldActive = true,
            )

            // Then
            assertTrue("Expected success with fallback gas on yield path", result.isRight())
            result.onRight { feeExtended ->
                val fee = feeExtended.transactionFee.normal as Fee.Ethereum.TokenCurrency
                // feeTransferGasLimit = FALLBACK_FEE_TRANSFER_GAS_LIMIT (100_000) * 1.10 = 110_000
                assertEquals(
                    "feeTransferGasLimit must use fallback (100000 * 1.10 = 110000)",
                    BigInteger("110000"),
                    fee.feeTransferGasLimit,
                )
                // gasLimit = 100_000 + 110_000 + 21_000 + 150_000 = 381_000
                assertEquals(
                    "gasLimit must include WITHDRAW_GAS_LIMIT (150000)",
                    BigInteger("381000"),
                    fee.gasLimit,
                )
            }
        }

    /**
     * Confirms that the non-yield path (isYieldActive = false, default) is unchanged:
     * a token with insufficient plain balance still raises NotEnoughFunds.
     */
    @Test
    fun `calculateTokenFee without yield still raises NotEnoughFunds on insufficient balance`() = runTest {
        // Given
        val tokenStatus = createMockTokenStatus(
            balance = BigDecimal("0.001"), // very small — insufficient
            fiatRate = BigDecimal("1"),
        )
        val nativeStatus = createMockNativeCurrencyStatus(fiatRate = BigDecimal("2000"))
        val initialFee = createMockEIP1559Fee()

        coEvery { mockWalletManager.getGasLimit(any(), any(), any()) } returns Result.Success(BigInteger("60000"))
        coEvery { gaslessTransactionRepository.getTokenFeeReceiverAddress() } returns "0xFeeReceiver"
        every { gaslessTransactionRepository.getBaseGasForTransaction() } returns BigInteger("21000")

        // When — default isYieldActive = false
        val result = tokenFeeCalculator.calculateTokenFee(
            walletManager = mockWalletManager,
            tokenForPayFeeStatus = tokenStatus,
            nativeCurrencyStatus = nativeStatus,
            initialFee = initialFee,
        )

        // Then
        assertTrue("Non-yield path must still raise NotEnoughFunds for insufficient balance", result.isLeft())
        result.onLeft { error ->
            assertTrue(error is GetFeeError.GaslessError.NotEnoughFunds)
        }
    }

    // ===== Helper Methods =====

    private fun createMockTransactionFee(): TransactionFee {
        val fee = Fee.Ethereum.EIP1559(
            amount = mockk(relaxed = true),
            gasLimit = BigInteger("100000"),
            maxFeePerGas = BigInteger("50000000000"),
            priorityFee = BigInteger("1000000000"),
        )
        return TransactionFee.Single(normal = fee)
    }

    private fun createMockEIP1559Fee(): Fee.Ethereum.EIP1559 {
        return Fee.Ethereum.EIP1559(
            amount = mockk(relaxed = true),
            gasLimit = BigInteger("100000"),
            maxFeePerGas = BigInteger("50000000000"), // 50 Gwei
            priorityFee = BigInteger("1000000000"), // 1 Gwei
        )
    }

    private fun createMockTokenStatus(
        balance: BigDecimal = BigDecimal("1000"),
        fiatRate: BigDecimal? = BigDecimal("1"),
        decimals: Int = 6,
    ): CryptoCurrencyStatus {
        val token = mockk<CryptoCurrency.Token>(relaxed = true)
        every { token.symbol } returns "USDC"
        every { token.contractAddress } returns "0xUSDC"
        every { token.decimals } returns decimals
        every { token.network } returns mockNetwork
        every { token.id } returns mockk(relaxed = true)

        val status = mockk<CryptoCurrencyStatus>()
        every { status.currency } returns token
        every { status.value.amount } returns balance
        every { status.value.fiatRate } returns fiatRate
        every { status.value.yieldSupplyStatus } returns null

        return status
    }

    /**
     * Returns a copy of this [CryptoCurrencyStatus] mock with [yieldSupplyStatus] overridden.
     * Since [CryptoCurrencyStatus] is a mockk, we create a new mock that delegates everything and
     * overrides only [yieldSupplyStatus].
     */
    private fun CryptoCurrencyStatus.withYieldSupplyStatus(yieldSupplyStatus: YieldSupplyStatus?): CryptoCurrencyStatus {
        val original = this
        val newStatus = mockk<CryptoCurrencyStatus>()
        every { newStatus.currency } returns original.currency
        every { newStatus.value.amount } returns original.value.amount
        every { newStatus.value.fiatRate } returns original.value.fiatRate
        every { newStatus.value.yieldSupplyStatus } returns yieldSupplyStatus
        return newStatus
    }

    private fun createMockNativeCurrencyStatus(
        fiatRate: BigDecimal? = BigDecimal("2000"),
        decimals: Int = 18,
    ): CryptoCurrencyStatus {
        val coin = mockk<CryptoCurrency.Coin>(relaxed = true)
        every { coin.symbol } returns "ETH"
        every { coin.decimals } returns decimals
        every { coin.network } returns mockNetwork

        val status = mockk<CryptoCurrencyStatus>()
        every { status.currency } returns coin
        every { status.value.fiatRate } returns fiatRate
        every { status.value.yieldSupplyStatus } returns null

        return status
    }
}