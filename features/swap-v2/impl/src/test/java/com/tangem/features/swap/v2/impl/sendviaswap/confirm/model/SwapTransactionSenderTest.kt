package com.tangem.features.swap.v2.impl.sendviaswap.confirm.model

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.TransactionData
import com.tangem.blockchain.common.TransactionExtras
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.common.test.domain.token.MockCryptoCurrencyFactory
import com.tangem.domain.express.models.ExpressError
import com.tangem.domain.express.models.ExpressOperationType
import com.tangem.domain.express.models.ExpressProviderType
import com.tangem.domain.express.models.ExpressRateType
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.swap.models.SwapAmountType
import com.tangem.domain.swap.models.SwapDataModel
import com.tangem.domain.swap.models.SwapDataTransactionModel
import com.tangem.domain.swap.models.SwapTxType
import com.tangem.domain.swap.usecase.GetSwapDataUseCase
import com.tangem.domain.swap.usecase.SwapTransactionSentUseCase
import com.tangem.domain.transaction.error.SendTransactionError
import com.tangem.domain.transaction.models.TransactionFeeExtended
import com.tangem.domain.transaction.usecase.CreateTransferTransactionUseCase
import com.tangem.domain.transaction.usecase.SendTransactionUseCase
import com.tangem.domain.transaction.usecase.gasless.CreateAndSendGaslessTransactionUseCase
import com.tangem.features.swap.v2.impl.common.ConfirmData
import com.tangem.features.swap.v2.impl.common.entity.SwapQuoteUM
import com.tangem.test.core.ProvideTestModels
import io.mockk.MockKMatcherScope
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import java.math.BigDecimal
import java.math.BigInteger

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class SwapTransactionSenderTest {

    private val getSwapDataUseCase: GetSwapDataUseCase = mockk()
    private val createTransferTransactionUseCase: CreateTransferTransactionUseCase = mockk()
    private val swapTransactionSentUseCase: SwapTransactionSentUseCase = mockk()
    private val sendTransactionUseCase: SendTransactionUseCase = mockk()
    private val createAndSendGaslessTransactionUseCase: CreateAndSendGaslessTransactionUseCase = mockk()
    private val userWallet: UserWallet = mockk(relaxed = true) {
        every { walletId } returns USER_WALLET_ID
    }

    private val sender = SwapTransactionSender(
        getSwapDataUseCase = getSwapDataUseCase,
        createTransferTransactionUseCase = createTransferTransactionUseCase,
        swapTransactionSentUseCase = swapTransactionSentUseCase,
        sendTransactionUseCase = sendTransactionUseCase,
        createAndSendGaslessTransactionUseCase = createAndSendGaslessTransactionUseCase,
        userWallet = userWallet,
    )

    private val currencyFactory = MockCryptoCurrencyFactory()
    private val fromStatus = CryptoCurrencyStatus(
        currency = currencyFactory.createCoin(Blockchain.Ethereum),
        value = CryptoCurrencyStatus.Loading,
    )
    private val toStatus = CryptoCurrencyStatus(
        currency = currencyFactory.createCoin(Blockchain.Bitcoin),
        value = CryptoCurrencyStatus.Loading,
    )
    private val cexProvider = expressProvider(type = ExpressProviderType.CEX)
    private val networkFee = Fee.Common(amount = ethAmount(BigDecimal("0.001")))

    private val sentSuccesses = mutableListOf<Triple<String, Long, SwapDataModel>>()
    private val expressErrors = mutableListOf<ExpressError>()
    private val sendErrors = mutableListOf<SendTransactionError?>()

    @BeforeEach
    fun resetMocks() {
        clearMocks(
            getSwapDataUseCase,
            createTransferTransactionUseCase,
            swapTransactionSentUseCase,
            sendTransactionUseCase,
            createAndSendGaslessTransactionUseCase,
        )
        sentSuccesses.clear()
        expressErrors.clear()
        sendErrors.clear()
    }

    @Test
    fun `GIVEN CEX quote and all steps succeed WHEN sendTransaction THEN swap stored and success reported`() =
        runTest {
            // Arrange
            val swapData = cexSwapData()
            stubSwapData(swapData.right())
            stubTransferTransaction(uncompiledTx().right())
            stubSendTransaction(TX_HASH.right())
            stubSwapTransactionSent()
            val timestamp = slot<Long>()

            // Act
            send()

            // Assert
            coVerify(exactly = 1) {
                swapTransactionSentUseCase(
                    fromUserWallet = userWallet,
                    toUserWallet = userWallet,
                    fromCryptoCurrencyStatus = fromStatus,
                    toCryptoCurrencyStatus = toStatus,
                    fromAccount = null,
                    toAccount = null,
                    swapDataTransactionModel = swapData.transaction,
                    provider = cexProvider,
                    txHash = TX_HASH,
                    payInAddress = PAY_IN_ADDRESS,
                    timestamp = capture(timestamp),
                    swapTxType = SwapTxType.SendWithSwap,
                )
            }
            assertThat(sentSuccesses).containsExactly(Triple(TX_HASH, timestamp.captured, swapData))
            assertThat(expressErrors).isEmpty()
            assertThat(sendErrors).isEmpty()
        }

    @Test
    fun `GIVEN CEX quote WHEN sendTransaction THEN transfer built to pay-in address with network fee`() = runTest {
        // Arrange
        val txData = uncompiledTx()
        stubSwapData(cexSwapData().right())
        stubTransferTransaction(txData.right())
        stubSendTransaction(TX_HASH.right())
        stubSwapTransactionSent()

        // Act
        send()

        // Assert
        coVerify(exactly = 1) {
            createTransferTransactionUseCase(
                amount = any(),
                fee = networkFee,
                memo = null,
                destination = PAY_IN_ADDRESS,
                userWalletId = USER_WALLET_ID,
                network = fromStatus.currency.network,
                nonce = any(),
            )
        }
        coVerify(exactly = 1) {
            sendTransactionUseCase(txData = txData, userWallet = userWallet, network = fromStatus.currency.network)
        }
    }

    @Test
    fun `GIVEN swap data request fails WHEN sendTransaction THEN express error reported and nothing built`() =
        runTest {
            // Arrange
            val error = ExpressError.ExchangeNotPossibleError(code = 2250)
            stubSwapData(error.left())

            // Act
            send()

            // Assert
            assertThat(expressErrors).containsExactly(error)
            coVerify(exactly = 0) { anyTransferTransaction() }
            assertThat(sentSuccesses).isEmpty()
        }

    @Test
    fun `GIVEN swap data with DEX transaction WHEN sendTransaction THEN unknown express error reported`() = runTest {
        // Arrange
        stubSwapData(dexSwapData().right())

        // Act
        send()

        // Assert
        assertThat(expressErrors).containsExactly(ExpressError.UnknownError)
        coVerify(exactly = 0) { anyTransferTransaction() }
    }

    @Test
    fun `GIVEN transfer transaction creation fails WHEN sendTransaction THEN unknown send error wraps cause`() =
        runTest {
            // Arrange
            val cause = IllegalStateException("Burn address")
            stubSwapData(cexSwapData().right())
            stubTransferTransaction(cause.left())

            // Act
            send()

            // Assert
            val sendError = sendErrors.single()
            assertThat(sendError).isInstanceOf(SendTransactionError.UnknownError::class.java)
            assertThat((sendError as SendTransactionError.UnknownError).ex).hasCauseThat().isSameInstanceAs(cause)
            coVerify(exactly = 0) { sendTransactionUseCase(txData = any(), userWallet = any(), network = any()) }
            assertThat(expressErrors).isEmpty()
        }

    @Test
    fun `GIVEN extra id required but tx has no extras WHEN sendTransaction THEN unknown express error reported`() =
        runTest {
            // Arrange
            stubSwapData(cexSwapData(txExtraId = "memo-42").right())
            stubTransferTransaction(uncompiledTx(extras = null).right())

            // Act
            send()

            // Assert
            assertThat(expressErrors).containsExactly(ExpressError.UnknownError)
            coVerify(exactly = 0) { sendTransactionUseCase(txData = any(), userWallet = any(), network = any()) }
            coVerify(exactly = 0) {
                createAndSendGaslessTransactionUseCase(userWallet = any(), transactionData = any(), fee = any())
            }
        }

    @Test
    fun `GIVEN extra id and tx extras present WHEN sendTransaction THEN transaction sent`() = runTest {
        // Arrange
        val swapData = cexSwapData(txExtraId = "memo-42")
        stubSwapData(swapData.right())
        stubTransferTransaction(uncompiledTx(extras = mockk<TransactionExtras>(relaxed = true)).right())
        stubSendTransaction(TX_HASH.right())
        stubSwapTransactionSent()

        // Act
        send()

        // Assert
        assertThat(expressErrors).isEmpty()
        assertThat(sentSuccesses.map { it.first }).containsExactly(TX_HASH)
    }

    @Test
    fun `GIVEN fee paid in token WHEN sendTransaction THEN sent via gasless use case`() = runTest {
        // Arrange
        val tokenFee = Fee.Ethereum.TokenCurrency(
            amount = ethAmount(BigDecimal("0.001")),
            gasLimit = BigInteger.valueOf(100_000),
            coinPriceInToken = BigInteger.ONE,
            feeTransferGasLimit = BigInteger.valueOf(50_000),
            baseGas = BigInteger.valueOf(21_000),
        )
        val feeExtended = TransactionFeeExtended(
            transactionFee = TransactionFee.Single(normal = tokenFee),
            feeTokenId = fromStatus.currency.id,
        )
        val txData = uncompiledTx()
        stubSwapData(cexSwapData().right())
        stubTransferTransaction(txData.right())
        coEvery {
            createAndSendGaslessTransactionUseCase(userWallet = any(), transactionData = any(), fee = any())
        } returns TX_HASH.right()
        stubSwapTransactionSent()

        // Act
        send(feeExtended = feeExtended)

        // Assert
        coVerify(exactly = 1) {
            createAndSendGaslessTransactionUseCase(
                userWallet = userWallet,
                transactionData = txData,
                fee = feeExtended,
            )
        }
        coVerify(exactly = 0) { sendTransactionUseCase(txData = any(), userWallet = any(), network = any()) }
        assertThat(sentSuccesses.map { it.first }).containsExactly(TX_HASH)
    }

    @Test
    fun `GIVEN extended fee paid in native coin WHEN sendTransaction THEN sent via regular use case`() = runTest {
        // Arrange
        val feeExtended = TransactionFeeExtended(
            transactionFee = TransactionFee.Single(normal = networkFee),
            feeTokenId = fromStatus.currency.id,
        )
        stubSwapData(cexSwapData().right())
        stubTransferTransaction(uncompiledTx().right())
        stubSendTransaction(TX_HASH.right())
        stubSwapTransactionSent()

        // Act
        send(feeExtended = feeExtended)

        // Assert
        coVerify(exactly = 1) { sendTransactionUseCase(txData = any(), userWallet = any(), network = any()) }
        coVerify(exactly = 0) {
            createAndSendGaslessTransactionUseCase(userWallet = any(), transactionData = any(), fee = any())
        }
    }

    @Test
    fun `GIVEN sending fails WHEN sendTransaction THEN send error reported and swap not stored`() = runTest {
        // Arrange
        val error = SendTransactionError.NetworkError(message = "timeout", code = "504")
        stubSwapData(cexSwapData().right())
        stubTransferTransaction(uncompiledTx().right())
        stubSendTransaction(error.left())

        // Act
        send()

        // Assert
        assertThat(sendErrors).containsExactly(error)
        assertThat(sentSuccesses).isEmpty()
        coVerify(exactly = 0) {
            swapTransactionSentUseCase(
                fromUserWallet = any(),
                toUserWallet = any(),
                fromCryptoCurrencyStatus = any(),
                toCryptoCurrencyStatus = any(),
                fromAccount = any(),
                toAccount = any(),
                swapDataTransactionModel = any(),
                provider = any(),
                txHash = any(),
                payInAddress = any(),
                timestamp = any(),
                swapTxType = any(),
            )
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class AmountTypeRouting {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN amount type WHEN sendTransaction THEN swap data and transfer use matching amounts`(
            model: AmountTypeModel,
        ) = runTest {
            // Arrange
            stubSwapData(cexSwapData().right())
            stubTransferTransaction(uncompiledTx().right())
            stubSendTransaction(TX_HASH.right())
            stubSwapTransactionSent()

            // Act
            send(confirmData = confirmData(amountType = model.amountType))

            // Assert
            coVerify(exactly = 1) {
                getSwapDataUseCase(
                    userWallet = userWallet,
                    fromCryptoCurrencyStatus = fromStatus,
                    amount = model.expectedSwapDataAmount,
                    amountType = model.amountType,
                    toCryptoCurrency = toStatus.currency,
                    toAddress = DESTINATION,
                    toExtraId = null,
                    expressProvider = cexProvider,
                    rateType = ExpressRateType.Float,
                    expressOperationType = ExpressOperationType.SEND_WITH_SWAP,
                    quoteId = QUOTE_ID,
                )
            }
            coVerify(exactly = 1) {
                createTransferTransactionUseCase(
                    amount = match { it.value == model.expectedTransferAmount },
                    fee = any(),
                    memo = any(),
                    destination = any(),
                    userWalletId = any(),
                    network = any(),
                    nonce = any(),
                )
            }
        }

        private fun provideTestModels() = listOf(
            AmountTypeModel(
                amountType = SwapAmountType.From,
                expectedSwapDataAmount = FROM_AMOUNT,
                expectedTransferAmount = FROM_AMOUNT,
            ),
            AmountTypeModel(
                amountType = SwapAmountType.To,
                expectedSwapDataAmount = TO_AMOUNT,
                expectedTransferAmount = FROM_AMOUNT,
            ),
        )
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class UnsupportedProvider {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN non-CEX provider WHEN sendTransaction THEN unknown express error and no use case invoked`(
            case: NonCexProviderTypeCase,
        ) = runTest {
            // Arrange
            val confirmData = confirmData(
                quote = contentQuote(
                    fromAmount = FROM_AMOUNT,
                    toAmount = TO_AMOUNT,
                    provider = expressProvider(type = case.providerType),
                    quoteId = QUOTE_ID,
                ),
            )

            // Act
            send(confirmData = confirmData)

            // Assert
            assertThat(expressErrors).containsExactly(ExpressError.UnknownError)
            coVerify(exactly = 0) { anySwapData() }
            coVerify(exactly = 0) { anyTransferTransaction() }
            coVerify(exactly = 0) { sendTransactionUseCase(txData = any(), userWallet = any(), network = any()) }
        }

        private fun provideTestModels() = nonCexProviderTypeCases
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class IncompleteConfirmData {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN required confirm data missing WHEN sendTransaction THEN returns silently`(
            model: IncompleteConfirmDataModel,
        ) = runTest {
            // Act
            send(confirmData = model.confirmData)

            // Assert
            assertThat(sentSuccesses).isEmpty()
            assertThat(expressErrors).isEmpty()
            assertThat(sendErrors).isEmpty()
            coVerify(exactly = 0) { anySwapData() }
        }

        private fun provideTestModels() = listOf(
            IncompleteConfirmDataModel("no quote", confirmData(quote = null)),
            IncompleteConfirmDataModel("no from status", confirmData(fromStatus = null)),
            IncompleteConfirmDataModel("no to status", confirmData(toStatus = null)),
            IncompleteConfirmDataModel("no rate type", confirmData(rateType = null)),
            IncompleteConfirmDataModel("no fee", confirmData(fee = null)),
            IncompleteConfirmDataModel("no destination", confirmData(enteredDestination = null)),
            IncompleteConfirmDataModel(
                description = "From type without from amount",
                confirmData = confirmData(amountType = SwapAmountType.From, enteredFromAmount = null),
            ),
            IncompleteConfirmDataModel(
                description = "To type without to amount",
                confirmData = confirmData(amountType = SwapAmountType.To, enteredToAmount = null),
            ),
            IncompleteConfirmDataModel(
                description = "To type without from amount",
                confirmData = confirmData(amountType = SwapAmountType.To, enteredFromAmount = null),
            ),
        )
    }

    data class AmountTypeModel(
        val amountType: SwapAmountType,
        val expectedSwapDataAmount: BigDecimal,
        val expectedTransferAmount: BigDecimal,
    )

    data class IncompleteConfirmDataModel(val description: String, val confirmData: ConfirmData) {
        override fun toString(): String = description
    }

    private suspend fun send(confirmData: ConfirmData = confirmData(), feeExtended: TransactionFeeExtended? = null) {
        sender.sendTransaction(
            confirmData = confirmData,
            isAmountSubtractAvailable = false,
            onSendSuccess = { txHash, timestamp, swapData -> sentSuccesses += Triple(txHash, timestamp, swapData) },
            onExpressError = { expressErrors += it },
            onSendError = { sendErrors += it },
            expressOperationType = ExpressOperationType.SEND_WITH_SWAP,
            feeExtended = feeExtended,
        )
    }

    private fun stubSwapData(result: Either<ExpressError, SwapDataModel>) {
        coEvery { anySwapData() } returns result
    }

    private fun stubTransferTransaction(result: Either<Throwable, TransactionData.Uncompiled>) {
        coEvery { anyTransferTransaction() } returns result
    }

    private fun stubSendTransaction(result: Either<SendTransactionError, String>) {
        coEvery { sendTransactionUseCase(txData = any(), userWallet = any(), network = any()) } returns result
    }

    private fun stubSwapTransactionSent() {
        coEvery {
            swapTransactionSentUseCase(
                fromUserWallet = any(),
                toUserWallet = any(),
                fromCryptoCurrencyStatus = any(),
                toCryptoCurrencyStatus = any(),
                fromAccount = any(),
                toAccount = any(),
                swapDataTransactionModel = any(),
                provider = any(),
                txHash = any(),
                payInAddress = any(),
                timestamp = any(),
                swapTxType = any(),
            )
        } returns Unit.right()
    }

    private suspend fun MockKMatcherScope.anySwapData() = getSwapDataUseCase(
        userWallet = any(),
        fromCryptoCurrencyStatus = any(),
        amount = any(),
        amountType = any(),
        toCryptoCurrency = any(),
        toAddress = any(),
        toExtraId = any(),
        expressProvider = any(),
        rateType = any(),
        expressOperationType = any(),
        quoteId = any(),
    )

    private suspend fun MockKMatcherScope.anyTransferTransaction() = createTransferTransactionUseCase(
        amount = any(),
        fee = any(),
        memo = any(),
        destination = any(),
        userWalletId = any(),
        network = any(),
        nonce = any(),
    )

    @Suppress("LongParameterList")
    private fun confirmData(
        quote: SwapQuoteUM? = contentQuote(
            fromAmount = FROM_AMOUNT,
            toAmount = TO_AMOUNT,
            provider = cexProvider,
            quoteId = QUOTE_ID,
        ),
        amountType: SwapAmountType = SwapAmountType.From,
        enteredFromAmount: BigDecimal? = FROM_AMOUNT,
        enteredToAmount: BigDecimal? = TO_AMOUNT,
        enteredDestination: String? = DESTINATION,
        fee: Fee? = networkFee,
        fromStatus: CryptoCurrencyStatus? = this.fromStatus,
        toStatus: CryptoCurrencyStatus? = this.toStatus,
        rateType: ExpressRateType? = ExpressRateType.Float,
    ) = ConfirmData(
        enteredFromAmount = enteredFromAmount,
        enteredToAmount = enteredToAmount,
        reduceAmountBy = BigDecimal.ZERO,
        isIgnoreReduce = false,
        enteredDestination = enteredDestination,
        enteredMemo = null,
        fee = fee,
        feeError = null,
        fromCryptoCurrencyStatus = fromStatus,
        toCryptoCurrencyStatus = toStatus,
        fromAccount = null,
        quote = quote,
        rateType = rateType,
        amountType = amountType,
        priceImpact = null,
    )

    private fun cexSwapData(txExtraId: String? = null) = SwapDataModel(
        toTokenAmount = TO_AMOUNT,
        transaction = SwapDataTransactionModel.CEX(
            fromAmount = FROM_AMOUNT,
            toAmount = TO_AMOUNT,
            txValue = null,
            txId = "tx-id",
            txTo = PAY_IN_ADDRESS,
            txExtraId = txExtraId,
            externalTxId = "external-tx-id",
            externalTxUrl = "https://provider.example/tx",
            txExtraIdName = null,
        ),
    )

    private fun dexSwapData() = SwapDataModel(
        toTokenAmount = TO_AMOUNT,
        transaction = SwapDataTransactionModel.DEX(
            fromAmount = FROM_AMOUNT,
            toAmount = TO_AMOUNT,
            txValue = null,
            txId = "tx-id",
            txTo = PAY_IN_ADDRESS,
            txExtraId = null,
            txFrom = "0xFrom",
            txData = "0x",
            otherNativeFeeWei = null,
            gas = BigInteger.ONE,
        ),
    )

    private fun uncompiledTx(extras: TransactionExtras? = null) = TransactionData.Uncompiled(
        amount = ethAmount(FROM_AMOUNT),
        fee = networkFee,
        sourceAddress = "0xFrom",
        destinationAddress = PAY_IN_ADDRESS,
        extras = extras,
    )

    private fun ethAmount(value: BigDecimal) = Amount(currencySymbol = "ETH", value = value, decimals = 18)

    private companion object {
        val USER_WALLET_ID = UserWalletId(stringValue = "0123456789")
        val FROM_AMOUNT = BigDecimal("1.5")
        val TO_AMOUNT = BigDecimal("0.05")
        const val DESTINATION = "bc1qdestination"
        const val PAY_IN_ADDRESS = "0xPayIn"
        const val QUOTE_ID = "quote-id"
        const val TX_HASH = "0xhash"
    }
}