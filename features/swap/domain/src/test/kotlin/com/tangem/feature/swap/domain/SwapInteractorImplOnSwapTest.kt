package com.tangem.feature.swap.domain

import android.util.Base64
import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.TransactionData
import com.tangem.blockchainsdk.utils.toNetworkId
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.swap.models.SwapCurrencyStatus
import com.tangem.domain.transaction.error.SendTransactionError
import com.tangem.domain.transaction.models.TransactionFeeExtended
import com.tangem.feature.swap.domain.fee.TransactionFeeResult
import com.tangem.feature.swap.domain.models.ExpressDataError
import com.tangem.feature.swap.domain.models.SwapAmount
import com.tangem.feature.swap.domain.models.domain.ExchangeProviderType
import com.tangem.feature.swap.domain.models.domain.ExpressTransactionModel
import com.tangem.feature.swap.domain.models.domain.IncludeFeeInAmount
import com.tangem.feature.swap.domain.models.domain.SwapDataModel
import com.tangem.feature.swap.domain.models.ui.*
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.*
import org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS
import java.math.BigDecimal
import java.math.BigInteger

@TestInstance(PER_CLASS)
internal class SwapInteractorImplOnSwapTest : SwapInteractorImplTestBase() {

    private val ethNetwork = Blockchain.Ethereum.toNetworkId()
    private val solanaNetwork = Blockchain.Solana.toNetworkId()

    @BeforeEach
    fun setupOnSwap() {
        // Clear recorded calls so that coVerify(exactly = 1) counts only the current test's call.
        clearMocks(
            sendTransactionUseCase,
            createTransactionUseCase,
            createTransferTransactionUseCase,
            createAndSendGaslessTransactionUseCase,
            repository,
            swapTransactionRepository,
            answers = false,
        )
        // isDemoCardUseCase should return false by default so the non-demo path is exercised.
        // Individual tests that need demo mode override this.
        every { isDemoCardUseCase(any()) } returns false
    }

    // region — shared helpers

    /**
     * Builds a SwapCurrencyStatus backed by an explicit UserWallet.Hot mock so that
     * `userWallet is UserWallet.Cold` evaluates to false reliably.
     */
    private fun buildHotSwapCurrencyStatus(
        networkRawId: String = ethNetwork,
        isCoin: Boolean = true,
    ): SwapCurrencyStatus {
        val hotWallet = mockk<UserWallet.Hot>(relaxed = true)
        return buildSwapCurrencyStatus(networkRawId = networkRawId, isCoin = isCoin).let {
            SwapCurrencyStatus(userWallet = hotWallet, status = it.status, account = it.account)
        }
    }

    private fun buildCexSwapDataModel(
        txTo: String = "0xCexAddress",
        txId: String = "cex-tx-id",
        txExtraId: String? = null,
        externalTxUrl: String = "https://explorer.com/tx/123",
        externalTxId: String = "ext-id-123",
        toAmount: BigDecimal = BigDecimal("0.9"),
    ): SwapDataModel = SwapDataModel(
        toTokenAmount = SwapAmount(toAmount, 18),
        transaction = ExpressTransactionModel.CEX(
            fromAmount = SwapAmount(BigDecimal.ONE, 18),
            toAmount = SwapAmount(toAmount, 18),
            txValue = null,
            txId = txId,
            txTo = txTo,
            txExtraId = txExtraId,
            externalTxId = externalTxId,
            externalTxUrl = externalTxUrl,
            txExtraIdName = null,
        ),
    )

    // endregion

    // -------------------------------------------------------------------------
    // Dispatcher Branches
    // -------------------------------------------------------------------------

    @Nested
    inner class DispatcherBranches {

        @Test
        fun `should return DemoMode for Cold card when isDemoCardUseCase returns true`() = runTest {
            // Given
            val coldWallet = mockk<UserWallet.Cold>(relaxed = true)
            every { isDemoCardUseCase(any()) } returns true

            val fromStatus = buildSwapCurrencyStatus(networkRawId = ethNetwork).let {
                SwapCurrencyStatus(userWallet = coldWallet, status = it.status, account = it.account)
            }
            val toStatus = buildHotSwapCurrencyStatus()
            val dexProvider = buildSwapProvider(ExchangeProviderType.DEX)
            val swapData = buildSwapDataModelDex()

            // When
            val result = sut.onSwap(
                fromSwapCurrencyStatus = fromStatus,
                toSwapCurrencyStatus = toStatus,
                swapProvider = dexProvider,
                swapData = swapData,
                amountToSwap = "1.0",
                includeFeeInAmount = IncludeFeeInAmount.Excluded,
                fee = buildTxFee(),
                expressOperationType = com.tangem.domain.express.models.ExpressOperationType.SWAP,
                isTangemPayWithdrawal = false,
            )

            // Then
            assertThat(result).isInstanceOf(SwapTransactionState.DemoMode::class.java)
            coVerify(exactly = 0) {
                createTransactionUseCase(
                    amount = any(), fee = any(), memo = any(),
                    destination = any(), userWalletId = any(), network = any(),
                )
            }
            coVerify(exactly = 0) {
                createTransferTransactionUseCase(
                    amount = any(), fee = any(), memo = any(),
                    destination = any(), userWalletId = any(), network = any(),
                )
            }
            coVerify(exactly = 0) {
                repository.getExchangeData(
                    userWallet = any(), fromContractAddress = any(), fromNetwork = any(),
                    toContractAddress = any(), fromAddress = any(), toNetwork = any(),
                    fromAmount = any(), fromDecimals = any(), toDecimals = any(),
                    providerId = any(), rateType = any(), toAddress = any(),
                    expressOperationType = any(), refundAddress = any(),
                )
            }
        }

        @Test
        fun `should route to onSwapCex and call getExchangeData for CEX provider`() = runTest {
            // Given
            val cexProvider = buildSwapProvider(ExchangeProviderType.CEX, providerId = "cex-route-id")
            val fromStatus = buildHotSwapCurrencyStatus()
            val toStatus = buildHotSwapCurrencyStatus()
            val cexSwapData = buildCexSwapDataModel()

            coEvery {
                repository.getExchangeData(
                    userWallet = any(), fromContractAddress = any(), fromNetwork = any(),
                    toContractAddress = any(), fromAddress = any(), toNetwork = any(),
                    fromAmount = any(), fromDecimals = any(), toDecimals = any(),
                    providerId = cexProvider.providerId, rateType = any(), toAddress = any(),
                    expressOperationType = any(), refundAddress = any(),
                )
            } returns cexSwapData.right()

            val txDataMock = mockk<TransactionData.Uncompiled>(relaxed = true) {
                every { extras } returns null
            }
            coEvery {
                createTransferTransactionUseCase(
                    amount = any(), fee = any(), memo = any(),
                    destination = any(), userWalletId = any(), network = any(),
                )
            } returns txDataMock.right()

            coEvery {
                sendTransactionUseCase(txData = any(), userWallet = any(), network = any())
            } returns "0xhash".right()

            // When
            sut.onSwap(
                fromSwapCurrencyStatus = fromStatus,
                toSwapCurrencyStatus = toStatus,
                swapProvider = cexProvider,
                swapData = null,
                amountToSwap = "1.0",
                includeFeeInAmount = IncludeFeeInAmount.Excluded,
                fee = buildTxFee(),
                expressOperationType = com.tangem.domain.express.models.ExpressOperationType.SWAP,
                isTangemPayWithdrawal = false,
            )

            // Then
            coVerify(exactly = 1) {
                repository.getExchangeData(
                    userWallet = any(), fromContractAddress = any(), fromNetwork = any(),
                    toContractAddress = any(), fromAddress = any(), toNetwork = any(),
                    fromAmount = any(), fromDecimals = any(), toDecimals = any(),
                    providerId = cexProvider.providerId, rateType = any(), toAddress = any(),
                    expressOperationType = any(), refundAddress = any(),
                )
            }
        }

        @Test
        fun `should return UnknownError for DEX non-Solana when fee is null`() = runTest {
            // Given
            val dexProvider = buildSwapProvider(ExchangeProviderType.DEX)
            val fromStatus = buildHotSwapCurrencyStatus()
            val toStatus = buildHotSwapCurrencyStatus()
            val swapData = buildSwapDataModelDex()

            // When
            val result = sut.onSwap(
                fromSwapCurrencyStatus = fromStatus,
                toSwapCurrencyStatus = toStatus,
                swapProvider = dexProvider,
                swapData = swapData,
                amountToSwap = "1.0",
                includeFeeInAmount = IncludeFeeInAmount.Excluded,
                fee = null,
                expressOperationType = com.tangem.domain.express.models.ExpressOperationType.SWAP,
                isTangemPayWithdrawal = false,
            )

            // Then
            assertThat(result).isInstanceOf(SwapTransactionState.Error.UnknownError::class.java)
            coVerify(exactly = 0) {
                createTransactionUseCase(
                    amount = any(), fee = any(), memo = any(),
                    destination = any(), userWalletId = any(), network = any(),
                )
            }
        }

        @Test
        fun `should route to onSwapDex for DEX_BRIDGE non-Solana with valid fee`() = runTest {
            // Given
            val dexBridgeProvider = buildSwapProvider(ExchangeProviderType.DEX_BRIDGE)
            val fromStatus = buildHotSwapCurrencyStatus()
            val toStatus = buildHotSwapCurrencyStatus()
            val swapData = buildSwapDataModelDex(txValue = "1000000000000000")
            val fee = buildTxFee()

            every {
                createTransactionExtrasUseCase.invoke(
                    data = any(), network = any(), gasLimit = any(),
                )
            } returns mockk<com.tangem.blockchain.common.TransactionExtras>(relaxed = true).right()

            val txDataMock = mockk<TransactionData.Uncompiled>(relaxed = true)
            coEvery {
                createTransactionUseCase(
                    amount = any(), fee = any(), memo = any(),
                    destination = any(), userWalletId = any(), network = any(),
                    txExtras = any(),
                )
            } returns txDataMock.right()

            coEvery {
                sendTransactionUseCase(txData = any(), userWallet = any(), network = any())
            } returns "0xhash-bridge".right()

            // When
            sut.onSwap(
                fromSwapCurrencyStatus = fromStatus,
                toSwapCurrencyStatus = toStatus,
                swapProvider = dexBridgeProvider,
                swapData = swapData,
                amountToSwap = "1.0",
                includeFeeInAmount = IncludeFeeInAmount.Excluded,
                fee = fee,
                expressOperationType = com.tangem.domain.express.models.ExpressOperationType.SWAP,
                isTangemPayWithdrawal = false,
            )

            // Then
            coVerify(exactly = 1) {
                createTransactionUseCase(
                    amount = any(), fee = any(), memo = any(),
                    destination = any(), userWalletId = any(), network = any(),
                    txExtras = any(),
                )
            }
        }

        @Test
        fun `should route to onSwapSolanaDex for DEX Solana without calling createTransactionUseCase`() = runTest {
            // Given
            mockkStatic(Base64::class)
            every { Base64.decode(any<String>(), any()) } returns ByteArray(100)

            val dexProvider = buildSwapProvider(ExchangeProviderType.DEX)
            val fromStatus = buildHotSwapCurrencyStatus(networkRawId = solanaNetwork)
            val toStatus = buildHotSwapCurrencyStatus(networkRawId = solanaNetwork)
            val swapData = buildSwapDataModelDex(txData = "dGVzdA==")

            coEvery {
                sendTransactionUseCase(txData = any(), userWallet = any(), network = any())
            } returns "0xsolana-hash".right()

            // When
            sut.onSwap(
                fromSwapCurrencyStatus = fromStatus,
                toSwapCurrencyStatus = toStatus,
                swapProvider = dexProvider,
                swapData = swapData,
                amountToSwap = "1.0",
                includeFeeInAmount = IncludeFeeInAmount.Excluded,
                fee = buildTxFee(),
                expressOperationType = com.tangem.domain.express.models.ExpressOperationType.SWAP,
                isTangemPayWithdrawal = false,
            )

            // Then
            coVerify(exactly = 0) {
                createTransactionUseCase(
                    amount = any(), fee = any(), memo = any(),
                    destination = any(), userWalletId = any(), network = any(),
                )
            }

            unmockkStatic(Base64::class)
        }
    }

    // -------------------------------------------------------------------------
    // OnSwapDex
    // -------------------------------------------------------------------------

    @Nested
    inner class OnSwapDex {

        @Test
        fun `should return TxSent and call exchangeSent and storeTransaction on happy path`() = runTest {
            // Given
            val dexProvider = buildSwapProvider(ExchangeProviderType.DEX)
            val fromStatus = buildHotSwapCurrencyStatus()
            val toStatus = buildHotSwapCurrencyStatus()
            val swapData = buildSwapDataModelDex(txValue = "1000000000000000")
            val fee = buildTxFee()

            every {
                createTransactionExtrasUseCase.invoke(data = any(), network = any(), gasLimit = any())
            } returns mockk<com.tangem.blockchain.common.TransactionExtras>(relaxed = true).right()

            val txDataMock = mockk<TransactionData.Uncompiled>(relaxed = true)
            coEvery {
                createTransactionUseCase(
                    amount = any(), fee = any(), memo = any(),
                    destination = any(), userWalletId = any(), network = any(),
                    txExtras = any(),
                )
            } returns txDataMock.right()

            coEvery {
                sendTransactionUseCase(txData = any(), userWallet = any(), network = any())
            } returns "0xdex-hash".right()

            every { amountFormatter.formatSwapAmountToUI(any(), any()) } returns "1.0 ETH"

            // When
            val result = sut.onSwap(
                fromSwapCurrencyStatus = fromStatus,
                toSwapCurrencyStatus = toStatus,
                swapProvider = dexProvider,
                swapData = swapData,
                amountToSwap = "1.0",
                includeFeeInAmount = IncludeFeeInAmount.Excluded,
                fee = fee,
                expressOperationType = com.tangem.domain.express.models.ExpressOperationType.SWAP,
                isTangemPayWithdrawal = false,
            )

            // Then
            assertThat(result).isInstanceOf(SwapTransactionState.TxSent::class.java)
            val txSent = result as SwapTransactionState.TxSent
            assertThat(txSent.txHash).isEqualTo("0xdex-hash")

            coVerify(exactly = 1) {
                repository.exchangeSent(
                    userWallet = any(), txId = any(), fromNetwork = any(),
                    fromAddress = any(), payInAddress = any(),
                    txHash = "0xdex-hash", payInExtraId = any(),
                )
            }
            coVerify(exactly = 1) {
                swapTransactionRepository.storeTransaction(
                    fromUserWalletId = any(), toUserWalletId = any(),
                    fromCryptoCurrency = any(), toCryptoCurrency = any(),
                    fromAccount = any(), toAccount = any(), transaction = any(),
                )
            }
        }

        @Test
        fun `should return UnknownError and not send when createTransactionUseCase fails`() = runTest {
            // Given
            val dexProvider = buildSwapProvider(ExchangeProviderType.DEX)
            val fromStatus = buildHotSwapCurrencyStatus()
            val toStatus = buildHotSwapCurrencyStatus()
            val swapData = buildSwapDataModelDex(txValue = "1000000000000000")
            val fee = buildTxFee()

            every {
                createTransactionExtrasUseCase.invoke(data = any(), network = any(), gasLimit = any())
            } returns mockk<com.tangem.blockchain.common.TransactionExtras>(relaxed = true).right()

            coEvery {
                createTransactionUseCase(
                    amount = any(), fee = any(), memo = any(),
                    destination = any(), userWalletId = any(), network = any(),
                    txExtras = any(),
                )
            } returns RuntimeException("create tx failed").left()

            // When
            val result = sut.onSwap(
                fromSwapCurrencyStatus = fromStatus,
                toSwapCurrencyStatus = toStatus,
                swapProvider = dexProvider,
                swapData = swapData,
                amountToSwap = "1.0",
                includeFeeInAmount = IncludeFeeInAmount.Excluded,
                fee = fee,
                expressOperationType = com.tangem.domain.express.models.ExpressOperationType.SWAP,
                isTangemPayWithdrawal = false,
            )

            // Then
            assertThat(result).isInstanceOf(SwapTransactionState.Error.UnknownError::class.java)
            coVerify(exactly = 0) {
                sendTransactionUseCase(txData = any(), userWallet = any(), network = any())
            }
        }

        @Test
        fun `should return TransactionError and not call exchangeSent when sendTransactionUseCase fails`() = runTest {
            // Given
            val dexProvider = buildSwapProvider(ExchangeProviderType.DEX)
            val fromStatus = buildHotSwapCurrencyStatus()
            val toStatus = buildHotSwapCurrencyStatus()
            val swapData = buildSwapDataModelDex(txValue = "1000000000000000")
            val fee = buildTxFee()
            val sendError = SendTransactionError.NetworkError(message = "timeout", code = "503")

            every {
                createTransactionExtrasUseCase.invoke(data = any(), network = any(), gasLimit = any())
            } returns mockk<com.tangem.blockchain.common.TransactionExtras>(relaxed = true).right()

            val txDataMock = mockk<TransactionData.Uncompiled>(relaxed = true)
            coEvery {
                createTransactionUseCase(
                    amount = any(), fee = any(), memo = any(),
                    destination = any(), userWalletId = any(), network = any(),
                    txExtras = any(),
                )
            } returns txDataMock.right()

            coEvery {
                sendTransactionUseCase(txData = any(), userWallet = any(), network = any())
            } returns sendError.left()

            // When
            val result = sut.onSwap(
                fromSwapCurrencyStatus = fromStatus,
                toSwapCurrencyStatus = toStatus,
                swapProvider = dexProvider,
                swapData = swapData,
                amountToSwap = "1.0",
                includeFeeInAmount = IncludeFeeInAmount.Excluded,
                fee = fee,
                expressOperationType = com.tangem.domain.express.models.ExpressOperationType.SWAP,
                isTangemPayWithdrawal = false,
            )

            // Then
            assertThat(result).isInstanceOf(SwapTransactionState.Error.TransactionError::class.java)
            val txError = result as SwapTransactionState.Error.TransactionError
            assertThat(txError.error).isEqualTo(sendError)

            coVerify(exactly = 0) {
                repository.exchangeSent(any(), any(), any(), any(), any(), any(), any())
            }
            coVerify(exactly = 0) {
                swapTransactionRepository.storeTransaction(any(), any(), any(), any(), any(), any(), any())
            }
        }
    }

    // -------------------------------------------------------------------------
    // OnSwapSolanaDex
    // -------------------------------------------------------------------------

    @Nested
    inner class OnSwapSolanaDex {

        @AfterEach
        fun tearDown() {
            unmockkStatic(Base64::class)
        }

        @Test
        fun `should return TxSent and call exchangeSent and storeTransaction on happy path`() = runTest {
            // Given
            mockkStatic(Base64::class)
            every { Base64.decode(any<String>(), any()) } returns ByteArray(100)

            val dexProvider = buildSwapProvider(ExchangeProviderType.DEX)
            val fromStatus = buildHotSwapCurrencyStatus(networkRawId = solanaNetwork)
            val toStatus = buildHotSwapCurrencyStatus(networkRawId = solanaNetwork)
            val swapData = buildSwapDataModelDex(txData = "dGVzdA==")

            coEvery {
                sendTransactionUseCase(txData = any(), userWallet = any(), network = any())
            } returns "0xsolana-hash".right()

            every { amountFormatter.formatSwapAmountToUI(any(), any()) } returns "1.0 SOL"

            // When
            val result = sut.onSwap(
                fromSwapCurrencyStatus = fromStatus,
                toSwapCurrencyStatus = toStatus,
                swapProvider = dexProvider,
                swapData = swapData,
                amountToSwap = "1.0",
                includeFeeInAmount = IncludeFeeInAmount.Excluded,
                fee = null,
                expressOperationType = com.tangem.domain.express.models.ExpressOperationType.SWAP,
                isTangemPayWithdrawal = false,
            )

            // Then
            assertThat(result).isInstanceOf(SwapTransactionState.TxSent::class.java)
            val txSent = result as SwapTransactionState.TxSent
            assertThat(txSent.txHash).isEqualTo("0xsolana-hash")

            coVerify(exactly = 1) {
                repository.exchangeSent(
                    userWallet = any(), txId = any(), fromNetwork = any(),
                    fromAddress = any(), payInAddress = any(),
                    txHash = "0xsolana-hash", payInExtraId = any(),
                )
            }
            coVerify(exactly = 1) {
                swapTransactionRepository.storeTransaction(
                    fromUserWalletId = any(), toUserWalletId = any(),
                    fromCryptoCurrency = any(), toCryptoCurrency = any(),
                    fromAccount = any(), toAccount = any(), transaction = any(),
                )
            }
        }

        @Test
        fun `should return TransactionError when sendTransactionUseCase fails on Solana path`() = runTest {
            // Given
            mockkStatic(Base64::class)
            every { Base64.decode(any<String>(), any()) } returns ByteArray(100)

            val dexProvider = buildSwapProvider(ExchangeProviderType.DEX)
            val fromStatus = buildHotSwapCurrencyStatus(networkRawId = solanaNetwork)
            val toStatus = buildHotSwapCurrencyStatus(networkRawId = solanaNetwork)
            val swapData = buildSwapDataModelDex(txData = "dGVzdA==")
            val sendError = SendTransactionError.UserCancelledError

            coEvery {
                sendTransactionUseCase(txData = any(), userWallet = any(), network = any())
            } returns sendError.left()

            // When
            val result = sut.onSwap(
                fromSwapCurrencyStatus = fromStatus,
                toSwapCurrencyStatus = toStatus,
                swapProvider = dexProvider,
                swapData = swapData,
                amountToSwap = "1.0",
                includeFeeInAmount = IncludeFeeInAmount.Excluded,
                fee = null,
                expressOperationType = com.tangem.domain.express.models.ExpressOperationType.SWAP,
                isTangemPayWithdrawal = false,
            )

            // Then
            assertThat(result).isInstanceOf(SwapTransactionState.Error.TransactionError::class.java)
            val txError = result as SwapTransactionState.Error.TransactionError
            assertThat(txError.error).isEqualTo(sendError)
        }
    }

    // -------------------------------------------------------------------------
    // OnSwapCex
    // -------------------------------------------------------------------------

    @Nested
    inner class OnSwapCex {

        private val cexProvider = buildSwapProvider(ExchangeProviderType.CEX, providerId = "cex-id")

        // Both from and to use Hot wallets to avoid spurious is-Cold checks
        private val fromStatus = buildHotSwapCurrencyStatus()
        private val toStatus = buildHotSwapCurrencyStatus()

        private fun stubGetExchangeData(result: SwapDataModel) {
            coEvery {
                repository.getExchangeData(
                    userWallet = any(), fromContractAddress = any(), fromNetwork = any(),
                    toContractAddress = any(), fromAddress = any(), toNetwork = any(),
                    fromAmount = any(), fromDecimals = any(), toDecimals = any(),
                    providerId = any(), rateType = any(), toAddress = any(),
                    expressOperationType = any(), refundAddress = any(),
                )
            } returns result.right()
        }

        private fun stubCreateTransferTx(txDataMock: TransactionData.Uncompiled = mockk(relaxed = true)) {
            coEvery {
                createTransferTransactionUseCase(
                    amount = any(), fee = any(), memo = any(),
                    destination = any(), userWalletId = any(), network = any(),
                )
            } returns txDataMock.right()
        }

        private suspend fun callOnSwap(
            fee: TxFee? = buildTxFee(),
            isTangemPayWithdrawal: Boolean = false,
        ) = sut.onSwap(
            fromSwapCurrencyStatus = fromStatus,
            toSwapCurrencyStatus = toStatus,
            swapProvider = cexProvider,
            swapData = null,
            amountToSwap = "1.0",
            includeFeeInAmount = IncludeFeeInAmount.Excluded,
            fee = fee,
            expressOperationType = com.tangem.domain.express.models.ExpressOperationType.SWAP,
            isTangemPayWithdrawal = isTangemPayWithdrawal,
        )

        @Test
        fun `should return ExpressError when getExchangeData fails`() = runTest {
            // Given
            val expressError = ExpressDataError.UnknownError
            coEvery {
                repository.getExchangeData(
                    userWallet = any(), fromContractAddress = any(), fromNetwork = any(),
                    toContractAddress = any(), fromAddress = any(), toNetwork = any(),
                    fromAmount = any(), fromDecimals = any(), toDecimals = any(),
                    providerId = any(), rateType = any(), toAddress = any(),
                    expressOperationType = any(), refundAddress = any(),
                )
            } returns expressError.left()

            // When
            val result = callOnSwap()

            // Then
            assertThat(result).isInstanceOf(SwapTransactionState.Error.ExpressError::class.java)
            val error = result as SwapTransactionState.Error.ExpressError
            assertThat(error.error).isEqualTo(expressError)

            coVerify(exactly = 0) {
                createTransferTransactionUseCase(
                    amount = any(), fee = any(), memo = any(),
                    destination = any(), userWalletId = any(), network = any(),
                )
            }
        }

        @Test
        fun `should return UnknownError when getExchangeData returns DEX transaction type`() = runTest {
            // Given — DEX-typed SwapDataModel where CEX path expects CEX type
            val dexSwapData = buildSwapDataModelDex()
            coEvery {
                repository.getExchangeData(
                    userWallet = any(), fromContractAddress = any(), fromNetwork = any(),
                    toContractAddress = any(), fromAddress = any(), toNetwork = any(),
                    fromAmount = any(), fromDecimals = any(), toDecimals = any(),
                    providerId = any(), rateType = any(), toAddress = any(),
                    expressOperationType = any(), refundAddress = any(),
                )
            } returns dexSwapData.right()

            // When
            val result = callOnSwap()

            // Then — cast to CEX returns null → UnknownError
            assertThat(result).isInstanceOf(SwapTransactionState.Error.UnknownError::class.java)
        }

        @Test
        fun `should return TangemPayWithdrawalData without sending when isTangemPayWithdrawal is true`() = runTest {
            // Given
            val cexSwapData = buildCexSwapDataModel(txTo = "0xCexDepositAddress")
            stubGetExchangeData(cexSwapData)
            every { amountFormatter.formatSwapAmountToUI(any(), any()) } returns "1.0 ETH"

            // When
            val result = callOnSwap(isTangemPayWithdrawal = true)

            // Then
            assertThat(result).isInstanceOf(SwapTransactionState.TangemPayWithdrawalData::class.java)
            val withdrawalData = result as SwapTransactionState.TangemPayWithdrawalData
            assertThat(withdrawalData.cexAddress).isEqualTo("0xCexDepositAddress")
            assertThat(withdrawalData.storeData).isNotNull()
            assertThat(withdrawalData.exchangeData).isNotNull()

            coVerify(exactly = 0) {
                sendTransactionUseCase(txData = any(), userWallet = any(), network = any())
            }
            coVerify(exactly = 0) {
                createAndSendGaslessTransactionUseCase.invoke(
                    transactionData = any(), userWallet = any(), fee = any(),
                )
            }
        }

        @Test
        fun `should return UnknownError for Cold demo card checked inside onSwapCex after getExchangeData`() = runTest {
            // Given
            // This demo check is at line ~818 of SwapInteractorImpl, AFTER getExchangeData succeeds.
            // The dispatcher-level check is bypassed by returning false on the first call.
            val coldWallet = mockk<UserWallet.Cold>(relaxed = true)

            // First call → false (dispatcher check passes), second call → true (onSwapCex internal check)
            every { isDemoCardUseCase(any()) } returnsMany listOf(false, true)

            val fromStatusCold = buildSwapCurrencyStatus(networkRawId = ethNetwork).let {
                SwapCurrencyStatus(userWallet = coldWallet, status = it.status, account = it.account)
            }
            val cexSwapData = buildCexSwapDataModel()
            coEvery {
                repository.getExchangeData(
                    userWallet = any(), fromContractAddress = any(), fromNetwork = any(),
                    toContractAddress = any(), fromAddress = any(), toNetwork = any(),
                    fromAmount = any(), fromDecimals = any(), toDecimals = any(),
                    providerId = any(), rateType = any(), toAddress = any(),
                    expressOperationType = any(), refundAddress = any(),
                )
            } returns cexSwapData.right()

            // When
            val result = sut.onSwap(
                fromSwapCurrencyStatus = fromStatusCold,
                toSwapCurrencyStatus = toStatus,
                swapProvider = cexProvider,
                swapData = null,
                amountToSwap = "1.0",
                includeFeeInAmount = IncludeFeeInAmount.Excluded,
                fee = buildTxFee(),
                expressOperationType = com.tangem.domain.express.models.ExpressOperationType.SWAP,
                isTangemPayWithdrawal = false,
            )

            // Then
            assertThat(result).isInstanceOf(SwapTransactionState.Error.UnknownError::class.java)
        }

        @Test
        fun `should return UnknownError when createTransferTransactionUseCase fails`() = runTest {
            // Given
            val cexSwapData = buildCexSwapDataModel()
            stubGetExchangeData(cexSwapData)
            coEvery {
                createTransferTransactionUseCase(
                    amount = any(), fee = any(), memo = any(),
                    destination = any(), userWalletId = any(), network = any(),
                )
            } returns RuntimeException("create transfer failed").left()

            // When
            val result = callOnSwap()

            // Then
            assertThat(result).isInstanceOf(SwapTransactionState.Error.UnknownError::class.java)
        }

        @Test
        fun `should return UnknownError when txData extras is null but txExtraId is present`() = runTest {
            // Given
            val cexSwapData = buildCexSwapDataModel(txExtraId = "extra-id-required")
            stubGetExchangeData(cexSwapData)

            val txDataMock = mockk<TransactionData.Uncompiled>(relaxed = true) {
                every { extras } returns null
            }
            stubCreateTransferTx(txDataMock)

            // When
            val result = callOnSwap()

            // Then — extras == null AND txExtraId != null → UnknownError
            assertThat(result).isInstanceOf(SwapTransactionState.Error.UnknownError::class.java)
        }

        @Test
        fun `should invoke createAndSendGaslessTransactionUseCase when FeeComponent with Token and LoadedExtended`() =
            runTest {
                // Given
                val cexSwapData = buildCexSwapDataModel()
                stubGetExchangeData(cexSwapData)

                val txDataMock = mockk<TransactionData.Uncompiled>(relaxed = true) {
                    every { extras } returns null
                }
                stubCreateTransferTx(txDataMock)

                val tokenCurrencyStatus = buildSwapCurrencyStatus(networkRawId = ethNetwork, isCoin = false)
                val extendedFee = mockk<TransactionFeeExtended>(relaxed = true)
                val gaslessFee = TxFee.FeeComponent(
                    fee = mockk(relaxed = true),
                    transactionFeeResult = TransactionFeeResult.LoadedExtended(extendedFee),
                    selectedToken = tokenCurrencyStatus.status,
                )

                coEvery {
                    createAndSendGaslessTransactionUseCase.invoke(
                        transactionData = any(), userWallet = any(), fee = any(),
                    )
                } returns "0xgasless-hash".right()

                // When
                val result = sut.onSwap(
                    fromSwapCurrencyStatus = fromStatus,
                    toSwapCurrencyStatus = toStatus,
                    swapProvider = cexProvider,
                    swapData = null,
                    amountToSwap = "1.0",
                    includeFeeInAmount = IncludeFeeInAmount.Excluded,
                    fee = gaslessFee,
                    expressOperationType = com.tangem.domain.express.models.ExpressOperationType.SWAP,
                    isTangemPayWithdrawal = false,
                )

                // Then
                coVerify(exactly = 1) {
                    createAndSendGaslessTransactionUseCase.invoke(
                        transactionData = any(), userWallet = any(), fee = extendedFee,
                    )
                }
                coVerify(exactly = 0) {
                    sendTransactionUseCase(txData = any(), userWallet = any(), network = any())
                }
                assertThat(result).isInstanceOf(SwapTransactionState.TxSent::class.java)
            }

        @Test
        fun `should invoke sendTransactionUseCase when FeeComponent but selectedToken is null`() = runTest {
            // Given
            val cexSwapData = buildCexSwapDataModel()
            stubGetExchangeData(cexSwapData)

            val txDataMock = mockk<TransactionData.Uncompiled>(relaxed = true) {
                every { extras } returns null
            }
            stubCreateTransferTx(txDataMock)

            val feeNoToken = TxFee.FeeComponent(
                fee = mockk(relaxed = true),
                transactionFeeResult = TransactionFeeResult.Loaded(mockk(relaxed = true)),
                selectedToken = null,
            )

            coEvery {
                sendTransactionUseCase(txData = any(), userWallet = any(), network = any())
            } returns "0xhash-notgasless".right()

            // When
            sut.onSwap(
                fromSwapCurrencyStatus = fromStatus,
                toSwapCurrencyStatus = toStatus,
                swapProvider = cexProvider,
                swapData = null,
                amountToSwap = "1.0",
                includeFeeInAmount = IncludeFeeInAmount.Excluded,
                fee = feeNoToken,
                expressOperationType = com.tangem.domain.express.models.ExpressOperationType.SWAP,
                isTangemPayWithdrawal = false,
            )

            // Then
            coVerify(exactly = 1) {
                sendTransactionUseCase(txData = any(), userWallet = any(), network = any())
            }
            coVerify(exactly = 0) {
                createAndSendGaslessTransactionUseCase.invoke(any(), any(), any())
            }
        }

        @Test
        fun `should invoke sendTransactionUseCase for Legacy fee`() = runTest {
            // Given
            val cexSwapData = buildCexSwapDataModel()
            stubGetExchangeData(cexSwapData)
            val txDataMock = mockk<TransactionData.Uncompiled>(relaxed = true) {
                every { extras } returns null
            }
            stubCreateTransferTx(txDataMock)

            val legacyFee = buildTxFeeLegacy()
            coEvery {
                sendTransactionUseCase(txData = any(), userWallet = any(), network = any())
            } returns "0xlegacy-hash".right()

            // When
            sut.onSwap(
                fromSwapCurrencyStatus = fromStatus,
                toSwapCurrencyStatus = toStatus,
                swapProvider = cexProvider,
                swapData = null,
                amountToSwap = "1.0",
                includeFeeInAmount = IncludeFeeInAmount.Excluded,
                fee = legacyFee,
                expressOperationType = com.tangem.domain.express.models.ExpressOperationType.SWAP,
                isTangemPayWithdrawal = false,
            )

            // Then
            coVerify(exactly = 1) {
                sendTransactionUseCase(txData = any(), userWallet = any(), network = any())
            }
            coVerify(exactly = 0) {
                createAndSendGaslessTransactionUseCase.invoke(any(), any(), any())
            }
        }

        @Test
        fun `should return TxSent and call all three side effects on CEX send success`() = runTest {
            // Given
            val cexSwapData = buildCexSwapDataModel()
            stubGetExchangeData(cexSwapData)
            val txDataMock = mockk<TransactionData.Uncompiled>(relaxed = true) {
                every { extras } returns null
            }
            stubCreateTransferTx(txDataMock)

            coEvery {
                sendTransactionUseCase(txData = any(), userWallet = any(), network = any())
            } returns "0xcex-hash".right()

            every { amountFormatter.formatSwapAmountToUI(any(), any()) } returns "1.0 ETH"

            // When
            val result = callOnSwap()

            // Then
            assertThat(result).isInstanceOf(SwapTransactionState.TxSent::class.java)
            val txSent = result as SwapTransactionState.TxSent
            assertThat(txSent.txHash).isEqualTo("0xcex-hash")

            coVerify(exactly = 1) {
                repository.exchangeSent(
                    userWallet = any(), txId = any(), fromNetwork = any(),
                    fromAddress = any(), payInAddress = any(),
                    txHash = "0xcex-hash", payInExtraId = any(),
                )
            }
            coVerify(exactly = 1) {
                swapTransactionRepository.storeTransaction(
                    fromUserWalletId = any(), toUserWalletId = any(),
                    fromCryptoCurrency = any(), toCryptoCurrency = any(),
                    fromAccount = any(), toAccount = any(), transaction = any(),
                )
            }
            coVerify(exactly = 1) {
                swapTransactionRepository.storeLastSwappedCryptoCurrencyId(
                    userWalletId = any(), cryptoCurrencyId = any(),
                )
            }
        }

        @Test
        fun `should return TransactionError when CEX send fails`() = runTest {
            // Given
            val cexSwapData = buildCexSwapDataModel()
            stubGetExchangeData(cexSwapData)
            val txDataMock = mockk<TransactionData.Uncompiled>(relaxed = true) {
                every { extras } returns null
            }
            stubCreateTransferTx(txDataMock)

            val sendError = SendTransactionError.DataError("connection reset")
            coEvery {
                sendTransactionUseCase(txData = any(), userWallet = any(), network = any())
            } returns sendError.left()

            // When
            val result = callOnSwap()

            // Then
            assertThat(result).isInstanceOf(SwapTransactionState.Error.TransactionError::class.java)
            val txError = result as SwapTransactionState.Error.TransactionError
            assertThat(txError.error).isEqualTo(sendError)
        }
    }
}

// region — file-private builders

private fun buildSwapDataModelDex(
    txData: String = "dGVzdA==",
    txValue: String? = "0",
    toAmount: BigDecimal = BigDecimal("0.5"),
): SwapDataModel = SwapDataModel(
    toTokenAmount = SwapAmount(toAmount, 18),
    transaction = ExpressTransactionModel.DEX(
        fromAmount = SwapAmount(BigDecimal.ONE, 18),
        toAmount = SwapAmount(toAmount, 18),
        txValue = txValue,
        txId = "tx-id-123",
        txTo = "0xRecipient",
        txExtraId = null,
        txFrom = "0xSender",
        txData = txData,
        otherNativeFeeWei = null,
        gas = BigInteger.valueOf(21_000L),
    ),
)

private fun buildTxFeeLegacy(
    feeValue: BigDecimal = BigDecimal("0.001"),
): TxFee.Legacy = TxFee.Legacy(
    feeValue = feeValue,
    feeFiatFormatted = "$0.01",
    feeCryptoFormatted = "0.001 ETH",
    feeIncludeOtherNativeFee = feeValue,
    feeFiatFormattedWithNative = "$0.01",
    feeCryptoFormattedWithNative = "0.001 ETH",
    cryptoSymbol = "ETH",
    feeType = FeeType.NORMAL,
    fee = mockk(relaxed = true),
)

// endregion