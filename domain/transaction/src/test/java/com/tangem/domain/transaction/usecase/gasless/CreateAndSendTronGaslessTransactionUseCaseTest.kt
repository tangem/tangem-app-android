package com.tangem.domain.transaction.usecase.gasless

import arrow.core.some
import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.blockchains.tron.TronTransactionExtras
import com.tangem.blockchain.blockchains.tron.tokenmethods.TronApprovalTokenCallData
import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.Token
import com.tangem.blockchain.common.TransactionData
import com.tangem.blockchain.common.TransactionSigner
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.common.test.domain.token.MockCryptoCurrencyFactory
import com.tangem.common.test.domain.wallet.MockUserWalletFactory
import com.tangem.domain.account.models.AccountList
import com.tangem.domain.account.status.utils.CryptoCurrencyOperations
import com.tangem.domain.account.supplier.SingleAccountListSupplier
import com.tangem.domain.transaction.TransactionRepository
import com.tangem.domain.transaction.TronGaslessTransactionRepository
import com.tangem.domain.transaction.error.SendTransactionError
import com.tangem.domain.transaction.models.GaslessProtocolData
import com.tangem.domain.transaction.models.TransactionFeeExtended
import com.tangem.domain.transaction.models.tron.TronGaslessApprove
import com.tangem.domain.transaction.models.tron.TronGaslessApproveEstimateParams
import com.tangem.domain.transaction.models.tron.TronGaslessEstimateParams
import com.tangem.domain.transaction.models.tron.TronGaslessQuote
import com.tangem.domain.transaction.models.tron.TronGaslessSubmitResult
import com.tangem.domain.transaction.models.tron.TronGaslessSwap
import com.tangem.domain.transaction.models.tron.TronGaslessSwapEstimateParams
import com.tangem.domain.walletmanager.WalletManagersFacade
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.math.BigInteger

internal class CreateAndSendTronGaslessTransactionUseCaseTest {

    private val transactionRepository: TransactionRepository = mockk()
    private val walletManagersFacade: WalletManagersFacade = mockk()
    private val tronGaslessTransactionRepository: TronGaslessTransactionRepository = mockk()
    private val singleAccountListSupplier: SingleAccountListSupplier = mockk()
    private val gaslessSignerProvider: GaslessSignerProvider = mockk()
    private val accountList: AccountList = mockk()
    private val signer: TransactionSigner = mockk()

    private val useCase = CreateAndSendTronGaslessTransactionUseCase(
        transactionRepository = transactionRepository,
        walletManagersFacade = walletManagersFacade,
        tronGaslessTransactionRepository = tronGaslessTransactionRepository,
        singleAccountListSupplier = singleAccountListSupplier,
        gaslessSignerProvider = gaslessSignerProvider,
    )

    private val userWallet = MockUserWalletFactory.create()
    private val usdtContract = "TR7NHqjeKQxGTCi8q8ZY4pL8otSzgjLj6t"
    private val usdtCurrency = MockCryptoCurrencyFactory().createToken(
        blockchain = Blockchain.Tron,
        contractAddress = usdtContract,
    )
    private val sentToken = Token(symbol = "USDT", contractAddress = usdtContract, decimals = 6)

    private val originalTx = TransactionData.Uncompiled(
        amount = Amount(token = sentToken, value = BigDecimal("50")),
        fee = null,
        sourceAddress = "TFrom",
        destinationAddress = "TTo",
    )
    private val compensationTx = TransactionData.Uncompiled(
        amount = Amount(token = sentToken, value = BigDecimal("2.75")),
        fee = null,
        sourceAddress = "TFrom",
        destinationAddress = "TFee",
    )

    private val quote = TronGaslessQuote(
        quoteId = "q_1",
        quotedAmountRaw = BigInteger("50000000"),
        quotedToAddress = "TTo",
        feeRecipient = "TFee",
        compensationToken = usdtContract,
        compensationAmountRaw = BigInteger("2750000"),
        compensationAmountDecimal = BigDecimal("2.75"),
        energy = 1,
        bandwidth = 1,
        trxCost = BigDecimal("1"),
        expiresAtEpochMs = 1,
    )
    private val fee = TransactionFeeExtended(
        transactionFee = TransactionFee.Single(
            normal = Fee.Common(amount = Amount(token = sentToken, value = BigDecimal("2.75"))),
        ),
        feeTokenId = usdtCurrency.id,
        gaslessProtocolData = GaslessProtocolData.Tron(quote = quote),
    )

    @BeforeEach
    fun setUp() {
        mockkObject(CryptoCurrencyOperations)
        every { gaslessSignerProvider(userWallet) } returns signer
        coEvery { singleAccountListSupplier.getSyncOrNull(userWallet.walletId) } returns accountList
        with(CryptoCurrencyOperations) {
            every { accountList.getCryptoCurrency(usdtCurrency.id, null) } returns usdtCurrency.some()
        }
        coEvery {
            transactionRepository.createTransferTransaction(any(), any(), any(), any(), any(), any(), any())
        } returns compensationTx
        coEvery {
            walletManagersFacade.signTronGaslessTransactions(any(), any(), any(), any())
        } returns listOf("signedComp", "signedOrig")
        coEvery {
            tronGaslessTransactionRepository.submit(userWallet.walletId, "q_1", "signedComp", "signedOrig")
        } returns TronGaslessSubmitResult(compensationTxHash = "hComp", originalTxHash = "hOrig", status = "BROADCAST")
    }

    @AfterEach
    fun tearDown() {
        unmockkObject(CryptoCurrencyOperations)
    }

    @Test
    fun `GIVEN signing and submit succeed WHEN invoke THEN returns original hash, compensation signed first`() =
        runTest {
            // Act
            val result = useCase(userWallet, originalTx, fee)

            // Assert
            assertThat(result.getOrNull()).isEqualTo("hOrig")
            coVerify {
                transactionRepository.createTransferTransaction(
                    amount = match { it.currencySymbol == "USDT" && it.value?.compareTo(BigDecimal("2.75")) == 0 },
                    fee = null,
                    memo = null,
                    destination = "TFee",
                    userWalletId = any(),
                    network = any(),
                )
            }
            coVerify {
                walletManagersFacade.signTronGaslessTransactions(
                    userWalletId = any(),
                    network = any(),
                    transactionDataList = match { it.size == 2 && it[0] === compensationTx && it[1] === originalTx },
                    signer = any(),
                )
            }
            coVerify { tronGaslessTransactionRepository.submit(userWallet.walletId, "q_1", "signedComp", "signedOrig") }
        }

    @Test
    fun `GIVEN fee token differs from sent token WHEN invoke THEN compensation built in fee token from raw amount`() =
        runTest {
            // Arrange — user pays the fee in USDC while sending USDT (cross-token compensation).
            val usdcContract = "TEkxiTehnzSmSe2XqrBj4w32RUN966rdz8"
            val usdcToken = Token(symbol = "USDC", contractAddress = usdcContract, decimals = 6)
            val crossTokenFee = fee.copy(
                transactionFee = TransactionFee.Single(
                    normal = Fee.Common(amount = Amount(token = usdcToken, value = BigDecimal("2.75"))),
                ),
                gaslessProtocolData = GaslessProtocolData.Tron(
                    quote = quote.copy(compensationToken = usdcContract),
                ),
            )

            // Act
            val result = useCase(userWallet, originalTx, crossTokenFee)

            // Assert
            assertThat(result.getOrNull()).isEqualTo("hOrig")
            coVerify {
                transactionRepository.createTransferTransaction(
                    amount = match { it.currencySymbol == "USDC" && it.value?.compareTo(BigDecimal("2.75")) == 0 },
                    fee = null,
                    memo = null,
                    destination = "TFee",
                    userWalletId = any(),
                    network = any(),
                )
            }
        }

    @Test
    fun `GIVEN signing returns null WHEN invoke THEN error`() = runTest {
        // Arrange
        coEvery { walletManagersFacade.signTronGaslessTransactions(any(), any(), any(), any()) } returns null

        // Act
        val result = useCase(userWallet, originalTx, fee)

        // Assert
        assertThat(result.isLeft()).isTrue()
    }

    @Test
    fun `GIVEN compensation token differs from fee token WHEN invoke THEN error`() = runTest {
        // Arrange — backend quotes a compensation token other than the selected fee token.
        val mismatchedFee = fee.copy(
            gaslessProtocolData = GaslessProtocolData.Tron(
                quote = quote.copy(compensationToken = "TOtherContractAddress00000000000000"),
            ),
        )

        // Act
        val result = useCase(userWallet, originalTx, mismatchedFee)

        // Assert
        assertThat(result.isLeft()).isTrue()
        coVerify(exactly = 0) { walletManagersFacade.signTronGaslessTransactions(any(), any(), any(), any()) }
    }

    @Test
    fun `GIVEN amount matches the quote WHEN invoke THEN quote is not refreshed`() = runTest {
        // Act
        val result = useCase(userWallet, originalTx, fee)

        // Assert
        assertThat(result.getOrNull()).isEqualTo("hOrig")
        coVerify(exactly = 0) { tronGaslessTransactionRepository.estimate(any()) }
    }

    @Test
    fun `GIVEN amount reduced by the fee after quoting WHEN invoke THEN re-quotes and submits the fresh quote`() =
        runTest {
            // Arrange — sending the whole balance: the send step lowers the transfer by the compensation.
            val reducedTx = originalTx.copy(
                amount = Amount(token = sentToken, value = BigDecimal("47.25")),
            )
            val refreshedQuote = quote.copy(quoteId = "q_2", quotedAmountRaw = BigInteger("47250000"))
            coEvery { tronGaslessTransactionRepository.estimate(any()) } returns refreshedQuote
            coEvery {
                tronGaslessTransactionRepository.submit(userWallet.walletId, "q_2", "signedComp", "signedOrig")
            } returns TronGaslessSubmitResult(
                compensationTxHash = "hComp",
                originalTxHash = "hOrig2",
                status = "BROADCAST",
            )

            // Act
            val result = useCase(userWallet, reducedTx, fee)

            // Assert
            assertThat(result.getOrNull()).isEqualTo("hOrig2")
            coVerify(exactly = 1) {
                tronGaslessTransactionRepository.estimate(
                    TronGaslessEstimateParams(
                        userWalletId = userWallet.walletId,
                        fromAddress = "TFrom",
                        toAddress = "TTo",
                        tokenContract = usdtContract,
                        amount = BigInteger("47250000"),
                        feeTokenContract = usdtContract,
                    ),
                )
            }
            coVerify { tronGaslessTransactionRepository.submit(userWallet.walletId, "q_2", "signedComp", "signedOrig") }
        }

    @Test
    fun `GIVEN quote taken for another destination WHEN invoke THEN re-quotes for the actual destination`() = runTest {
        // Arrange
        val estimationFee = fee.copy(
            gaslessProtocolData = GaslessProtocolData.Tron(quote.copy(quotedToAddress = "TEstimation")),
        )
        coEvery { tronGaslessTransactionRepository.estimate(any()) } returns quote.copy(quoteId = "q_2")
        coEvery {
            tronGaslessTransactionRepository.submit(userWallet.walletId, "q_2", "signedComp", "signedOrig")
        } returns TronGaslessSubmitResult(compensationTxHash = "hComp", originalTxHash = "hOrig2", status = "BROADCAST")

        // Act
        val result = useCase(userWallet, originalTx, estimationFee)

        // Assert
        assertThat(result.getOrNull()).isEqualTo("hOrig2")
        coVerify(exactly = 1) {
            tronGaslessTransactionRepository.estimate(
                TronGaslessEstimateParams(
                    userWalletId = userWallet.walletId,
                    fromAddress = "TFrom",
                    toAddress = "TTo",
                    tokenContract = usdtContract,
                    amount = BigInteger("50000000"),
                    feeTokenContract = usdtContract,
                ),
            )
        }
    }

    @Test
    fun `GIVEN refreshed compensation exceeds the confirmed one WHEN invoke THEN fee increased error, nothing signed`() =
        runTest {
            // Arrange — a bigger compensation would leave the reduced transfer short of funds and revert it.
            val reducedTx = originalTx.copy(
                amount = Amount(token = sentToken, value = BigDecimal("47.25")),
            )
            coEvery { tronGaslessTransactionRepository.estimate(any()) } returns quote.copy(
                quoteId = "q_2",
                quotedAmountRaw = BigInteger("47250000"),
                compensationAmountRaw = BigInteger("2750001"),
            )

            // Act
            val result = useCase(userWallet, reducedTx, fee)

            // Assert
            assertThat(result.leftOrNull()).isEqualTo(SendTransactionError.GaslessFeeIncreased)
            coVerify(exactly = 0) { walletManagersFacade.signTronGaslessTransactions(any(), any(), any(), any()) }
        }

    @Test
    fun `GIVEN backend reports partial failure WHEN invoke THEN error`() = runTest {
        // Arrange
        coEvery {
            tronGaslessTransactionRepository.submit(userWallet.walletId, "q_1", "signedComp", "signedOrig")
        } returns TronGaslessSubmitResult(compensationTxHash = "hComp", originalTxHash = "hOrig", status = "PARTIAL_FAILED")

        // Act
        val result = useCase(userWallet, originalTx, fee)

        // Assert
        assertThat(result.isLeft()).isTrue()
    }

    @Nested
    inner class SendSwap {

        private val swap = TronGaslessSwap(
            fromAddress = "TFrom",
            tokenContract = usdtContract,
            amount = BigInteger("50000000"),
            routerAddress = "TRouter",
            callDataHex = "4c279d6b",
            spenderAddress = "TRouter",
        )
        private val swapTx = TransactionData.Uncompiled(
            amount = Amount(value = BigDecimal.ZERO, blockchain = Blockchain.Tron),
            fee = null,
            sourceAddress = "TFrom",
            destinationAddress = "TRouter",
        )
        private val swapQuote = quote.copy(quotedToAddress = "TRouter", quotedSwap = swap)
        private val swapFee = fee.copy(gaslessProtocolData = GaslessProtocolData.Tron(quote = swapQuote))
        private val submitted = TronGaslessSubmitResult(
            compensationTxHash = "hComp",
            originalTxHash = "hSwap",
            status = "BROADCAST",
        )

        @Test
        fun `GIVEN quoted swap WHEN sendSwap THEN signs compensation and swap in one session and submits them`() =
            runTest {
                // Arrange
                coEvery {
                    walletManagersFacade.signTronGaslessTransactions(any(), any(), any(), any())
                } returns listOf("signedComp", "signedSwap")
                coEvery { tronGaslessTransactionRepository.submit(any(), any(), any(), any()) } returns submitted

                // Act
                val result = useCase.sendSwap(userWallet, swap, swapTx, swapFee)

                // Assert
                assertThat(result.getOrNull()).isEqualTo("hSwap")
                coVerify(exactly = 1) {
                    walletManagersFacade.signTronGaslessTransactions(
                        userWalletId = any(),
                        network = any(),
                        transactionDataList = listOf(compensationTx, swapTx),
                        signer = any(),
                    )
                }
                coVerify(exactly = 1) {
                    tronGaslessTransactionRepository.submit(
                        userWalletId = userWallet.walletId,
                        quoteId = "q_1",
                        signedCompensationTx = "signedComp",
                        signedOriginalTx = "signedSwap",
                    )
                }
                coVerify(exactly = 0) { tronGaslessTransactionRepository.estimateSwap(any()) }
            }

        @Test
        fun `GIVEN route refreshed after quoting WHEN sendSwap THEN re-quotes the new route`() = runTest {
            // Arrange
            val refreshedSwap = swap.copy(callDataHex = "4c279d6c")
            coEvery { tronGaslessTransactionRepository.estimateSwap(any()) } returns
                swapQuote.copy(quoteId = "q_2", quotedSwap = refreshedSwap)
            coEvery {
                walletManagersFacade.signTronGaslessTransactions(any(), any(), any(), any())
            } returns listOf("signedComp", "signedSwap")
            coEvery { tronGaslessTransactionRepository.submit(any(), any(), any(), any()) } returns submitted

            // Act
            val result = useCase.sendSwap(userWallet, refreshedSwap, swapTx, swapFee)

            // Assert
            assertThat(result.getOrNull()).isEqualTo("hSwap")
            coVerify(exactly = 1) {
                tronGaslessTransactionRepository.estimateSwap(
                    TronGaslessSwapEstimateParams(
                        userWalletId = userWallet.walletId,
                        swap = refreshedSwap,
                        feeTokenContract = usdtContract,
                    ),
                )
            }
            coVerify { tronGaslessTransactionRepository.submit(any(), "q_2", any(), any()) }
        }

        @Test
        fun `GIVEN re-quoted compensation grew WHEN sendSwap THEN error and nothing is signed`() = runTest {
            // Arrange
            val refreshedSwap = swap.copy(callDataHex = "4c279d6c")
            coEvery { tronGaslessTransactionRepository.estimateSwap(any()) } returns swapQuote.copy(
                quoteId = "q_2",
                quotedSwap = refreshedSwap,
                compensationAmountRaw = BigInteger("2750001"),
            )

            // Act
            val result = useCase.sendSwap(userWallet, refreshedSwap, swapTx, swapFee)

            // Assert
            assertThat(result.isLeft()).isTrue()
            coVerify(exactly = 0) { walletManagersFacade.signTronGaslessTransactions(any(), any(), any(), any()) }
        }
    }

    @Nested
    inner class Approve {

        private val approvalTx = TransactionData.Uncompiled(
            amount = Amount(token = sentToken, value = BigDecimal.ZERO),
            fee = null,
            sourceAddress = "TFrom",
            destinationAddress = usdtContract,
            extras = TronTransactionExtras(
                callData = TronApprovalTokenCallData(
                    spenderAddress = "TRouter",
                    amount = Amount(token = sentToken, value = BigDecimal("3")),
                ),
            ),
        )
        private val approve = TronGaslessApprove(
            fromAddress = "TFrom",
            tokenContract = usdtContract,
            amount = BigInteger("3000000"),
            spenderAddress = "TRouter",
        )

        @Test
        fun `GIVEN quote for this approval WHEN invoke THEN signs compensation and approval without re-quoting`() =
            runTest {
                // Arrange
                val approveFee = fee.copy(
                    gaslessProtocolData = GaslessProtocolData.Tron(quote = quote.copy(quotedApprove = approve)),
                )

                // Act
                val result = useCase(userWallet, approvalTx, approveFee)

                // Assert
                assertThat(result.getOrNull()).isEqualTo("hOrig")
                coVerify {
                    walletManagersFacade.signTronGaslessTransactions(
                        userWalletId = any(),
                        network = any(),
                        transactionDataList = match { it.size == 2 && it[0] === compensationTx && it[1] === approvalTx },
                        signer = any(),
                    )
                }
                coVerify(exactly = 0) { tronGaslessTransactionRepository.estimateApprove(any()) }
                coVerify(exactly = 0) { tronGaslessTransactionRepository.estimate(any()) }
            }

        @Test
        fun `GIVEN quote for another approval amount WHEN invoke THEN re-quotes the approval`() = runTest {
            // Arrange
            val staleFee = fee.copy(
                gaslessProtocolData = GaslessProtocolData.Tron(
                    quote = quote.copy(quotedApprove = approve.copy(amount = BigInteger("1000000"))),
                ),
            )
            coEvery { tronGaslessTransactionRepository.estimateApprove(any()) } returns
                quote.copy(quotedApprove = approve)

            // Act
            val result = useCase(userWallet, approvalTx, staleFee)

            // Assert
            assertThat(result.getOrNull()).isEqualTo("hOrig")
            coVerify(exactly = 1) {
                tronGaslessTransactionRepository.estimateApprove(
                    TronGaslessApproveEstimateParams(
                        userWalletId = userWallet.walletId,
                        approve = approve,
                        feeTokenContract = usdtContract,
                    ),
                )
            }
        }

        @Test
        fun `GIVEN re-quoted approval compensation grew WHEN invoke THEN error without signing`() = runTest {
            // Arrange
            val staleFee = fee.copy(
                gaslessProtocolData = GaslessProtocolData.Tron(
                    quote = quote.copy(quotedApprove = approve.copy(amount = BigInteger("1000000"))),
                ),
            )
            coEvery { tronGaslessTransactionRepository.estimateApprove(any()) } returns
                quote.copy(quotedApprove = approve, compensationAmountRaw = BigInteger("2750001"))

            // Act
            val result = useCase(userWallet, approvalTx, staleFee)

            // Assert
            assertThat(result.isLeft()).isTrue()
            coVerify(exactly = 0) { walletManagersFacade.signTronGaslessTransactions(any(), any(), any(), any()) }
        }
    }
}