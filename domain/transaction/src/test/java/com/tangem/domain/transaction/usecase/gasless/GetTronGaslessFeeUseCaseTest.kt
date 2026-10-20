package com.tangem.domain.transaction.usecase.gasless

import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.blockchains.tron.TronTransactionExtras
import com.tangem.blockchain.blockchains.tron.tokenmethods.TronApprovalTokenCallData
import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.Token
import com.tangem.blockchain.common.TransactionData
import com.tangem.common.test.domain.token.MockCryptoCurrencyFactory
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.transaction.TronGaslessTransactionRepository
import com.tangem.domain.transaction.error.GaslessServiceException
import com.tangem.domain.transaction.error.GetFeeError
import com.tangem.domain.transaction.error.TronActivationRefusal
import com.tangem.domain.transaction.models.GaslessProtocolData
import com.tangem.domain.transaction.models.tron.TronGaslessApprove
import com.tangem.domain.transaction.models.tron.TronGaslessApproveEstimateParams
import com.tangem.domain.transaction.models.tron.TronGaslessQuote
import com.tangem.domain.transaction.models.tron.TronGaslessSwap
import com.tangem.domain.transaction.models.tron.TronGaslessSwapEstimateParams
import com.tangem.test.core.ProvideTestModels
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import java.math.BigDecimal
import java.math.BigInteger

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class GetTronGaslessFeeUseCaseTest {

    private val repository: TronGaslessTransactionRepository = mockk()
    private val useCase = GetTronGaslessFeeUseCase(tronGaslessTransactionRepository = repository)

    private val userWalletId = UserWalletId("011")
    private val usdtContract = "TR7NHqjeKQxGTCi8q8ZY4pL8otSzgjLj6t"
    private val usdtToken = MockCryptoCurrencyFactory().createToken(
        blockchain = Blockchain.Tron,
        contractAddress = usdtContract,
    )

    private val transactionData = TransactionData.Uncompiled(
        amount = Amount(
            token = Token(symbol = "USDT", contractAddress = usdtContract, decimals = 6),
            value = BigDecimal("50"),
        ),
        fee = null,
        sourceAddress = "TFrom",
        destinationAddress = "TTo",
    )

    private val quote = TronGaslessQuote(
        quoteId = "q_1",
        quotedAmountRaw = BigInteger("50000000"),
        quotedToAddress = "TTo",
        feeRecipient = "TFee",
        compensationToken = usdtContract,
        // Raw matches the mock fee token's 8 decimals (MockCryptoCurrencyFactory): 2.75 * 10^8.
        compensationAmountRaw = BigInteger("275000000"),
        compensationAmountDecimal = BigDecimal("2.75"),
        energy = 78000,
        bandwidth = 345,
        trxCost = BigDecimal("8.0722"),
        expiresAtEpochMs = 1_780_489_496_000,
    )

    @BeforeEach
    fun resetMocks() {
        clearMocks(repository)
    }

    private suspend fun invoke() = useCase(
        userWalletId = userWalletId,
        transactionData = transactionData,
        feeToken = usdtToken,
    )

    @Test
    fun `GIVEN estimate succeeds WHEN invoke THEN fee carries quote and compensation amount`() = runTest {
        // Arrange
        coEvery { repository.estimate(any()) } returns quote

        // Act
        val result = invoke()

        // Assert
        val extended = result.getOrNull()
        assertThat(extended).isNotNull()
        assertThat(extended!!.gaslessProtocolData).isEqualTo(GaslessProtocolData.Tron(quote = quote))
        assertThat(extended.feeTokenId).isEqualTo(usdtToken.id)
        assertThat(extended.transactionFee.normal.amount.value).isEquivalentAccordingToCompareTo(BigDecimal("2.75"))
        assertThat(extended.transactionFee.normal.amount.currencySymbol).isEqualTo(usdtToken.symbol)
        // The 50 USDT send amount must reach the backend as raw base units (50 * 10^6).
        coVerify {
            repository.estimate(
                match {
                    it.userWalletId == userWalletId &&
                        it.amount == BigInteger("50000000") &&
                        it.feeTokenContract == usdtContract
                },
            )
        }
    }

    @Test
    fun `GIVEN activation balance too low WHEN invoke THEN refused with the service minimum`() = runTest {
        // Arrange
        coEvery { repository.estimate(any()) } throws serviceError(
            refusal = TronActivationRefusal.BalanceTooLow(minimumAmountRaw = BigInteger("500000000")),
        )

        // Act
        val result = invoke()

        // Assert
        // The mock fee token has 8 decimals, so 500000000 base units are 5 tokens, shown without trailing zeros.
        assertThat(result.leftOrNull()).isEqualTo(
            GetFeeError.GaslessError.TronActivationNotEnoughFunds(
                minimumAmount = BigDecimal("5"),
                tokenSymbol = usdtToken.symbol,
            ),
        )
    }

    @Test
    fun `GIVEN activation rate limited WHEN invoke THEN TronActivationRateLimited`() = runTest {
        // Arrange
        coEvery { repository.estimate(any()) } throws serviceError(
            refusal = TronActivationRefusal.RateLimited(isPerWallet = true),
            httpCode = 503,
        )

        // Act
        val result = invoke()

        // Assert
        assertThat(result.leftOrNull()).isEqualTo(GetFeeError.GaslessError.TronActivationRateLimited(isPerWallet = true))
    }

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN estimate fails without a refusal WHEN invoke THEN GaslessError DataError`(model: DataErrorModel) =
        runTest {
            // Arrange
            coEvery { repository.estimate(any()) } throws model.error

            // Act
            val result = invoke()

            // Assert
            assertThat(result.leftOrNull()).isEqualTo(GetFeeError.GaslessError.DataError(model.error))
        }

    data class DataErrorModel(val case: String, val error: Throwable) {
        override fun toString(): String = case
    }

    private fun provideTestModels() = listOf(
        DataErrorModel(case = "transport failure", error = RuntimeException("boom")),
        DataErrorModel(case = "service error without a refusal", error = serviceError(refusal = null)),
        DataErrorModel(case = "plain 429", error = serviceError(refusal = null, httpCode = 429)),
    )

    private fun serviceError(refusal: TronActivationRefusal?, httpCode: Int = 422) =
        GaslessServiceException(httpCode = httpCode, message = "error", tronActivationRefusal = refusal)

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class ForSwap {

        private val swap = TronGaslessSwap(
            fromAddress = "TFrom",
            tokenContract = usdtContract,
            amount = BigInteger("50000000"),
            routerAddress = "TRouter",
            callDataHex = "4c279d6b",
            spenderAddress = "TRouter",
        )

        @Test
        fun `GIVEN swap estimate succeeds WHEN forSwap THEN fee carries the swap quote`() = runTest {
            // Arrange
            val swapQuote = quote.copy(quotedToAddress = "TRouter", quotedSwap = swap)
            coEvery { repository.estimateSwap(any()) } returns swapQuote

            // Act
            val result = useCase.forSwap(userWalletId = userWalletId, swap = swap, feeToken = usdtToken)

            // Assert
            val extended = result.getOrNull()
            assertThat(extended?.gaslessProtocolData).isEqualTo(GaslessProtocolData.Tron(quote = swapQuote))
            assertThat(extended?.transactionFee?.normal?.amount?.value)
                .isEquivalentAccordingToCompareTo(BigDecimal("2.75"))
            coVerify(exactly = 1) {
                repository.estimateSwap(
                    TronGaslessSwapEstimateParams(
                        userWalletId = userWalletId,
                        swap = swap,
                        feeTokenContract = usdtContract,
                    ),
                )
            }
            coVerify(exactly = 0) { repository.estimate(any()) }
        }

        @Test
        fun `GIVEN swap estimate fails WHEN forSwap THEN gasless data error`() = runTest {
            // Arrange
            val failure = IllegalStateException("TRON_SWAP_NOT_ALLOWED")
            coEvery { repository.estimateSwap(any()) } throws failure

            // Act
            val result = useCase.forSwap(userWalletId = userWalletId, swap = swap, feeToken = usdtToken)

            // Assert
            assertThat(result.leftOrNull()).isEqualTo(GetFeeError.GaslessError.DataError(failure))
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class ForApprove {

        private val usdt = Token(symbol = "USDT", contractAddress = usdtContract, decimals = 6)

        private fun approval(approvedValue: BigDecimal?) = TransactionData.Uncompiled(
            amount = Amount(token = usdt, value = BigDecimal.ZERO),
            fee = null,
            sourceAddress = "TFrom",
            destinationAddress = usdtContract,
            extras = TronTransactionExtras(
                callData = TronApprovalTokenCallData(
                    spenderAddress = "TRouter",
                    amount = approvedValue?.let { Amount(token = usdt, value = it) },
                ),
            ),
        )

        @Test
        fun `GIVEN exact approval WHEN invoke THEN quotes the approval instead of a transfer`() = runTest {
            // Arrange
            val approve = TronGaslessApprove(
                fromAddress = "TFrom",
                tokenContract = usdtContract,
                amount = BigInteger("3000000"),
                spenderAddress = "TRouter",
            )
            val approveQuote = quote.copy(quotedApprove = approve)
            coEvery { repository.estimateApprove(any()) } returns approveQuote

            // Act
            val result = useCase(
                userWalletId = userWalletId,
                transactionData = approval(approvedValue = BigDecimal("3")),
                feeToken = usdtToken,
            )

            // Assert
            assertThat(
                result.getOrNull()?.gaslessProtocolData,
            ).isEqualTo(GaslessProtocolData.Tron(quote = approveQuote))
            coVerify(exactly = 1) {
                repository.estimateApprove(
                    TronGaslessApproveEstimateParams(
                        userWalletId = userWalletId,
                        approve = approve,
                        feeTokenContract = usdtContract,
                    ),
                )
            }
            coVerify(exactly = 0) { repository.estimate(any()) }
        }

        @Test
        fun `GIVEN unlimited approval WHEN tronGaslessApproveOrNull THEN null`() {
            // Act
            val actual = approval(approvedValue = null).tronGaslessApproveOrNull()

            // Assert
            assertThat(actual).isNull()
        }
    }
}