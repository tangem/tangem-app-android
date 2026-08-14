package com.tangem.domain.transaction.usecase.gasless

import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.Token
import com.tangem.blockchain.common.TransactionData
import com.tangem.common.test.domain.token.MockCryptoCurrencyFactory
import com.tangem.domain.transaction.TronGaslessTransactionRepository
import com.tangem.domain.transaction.error.GetFeeError
import com.tangem.domain.transaction.models.tron.TronGaslessQuote
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.math.BigInteger

internal class GetTronGaslessFeeUseCaseTest {

    private val repository: TronGaslessTransactionRepository = mockk()
    private val useCase = GetTronGaslessFeeUseCase(tronGaslessTransactionRepository = repository)

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

    @Test
    fun `GIVEN estimate succeeds WHEN invoke THEN fee carries quote and compensation amount`() = runTest {
        // Arrange
        coEvery { repository.estimate(any()) } returns quote

        // Act
        val result = useCase(transactionData = transactionData, feeToken = usdtToken)

        // Assert
        val extended = result.getOrNull()
        assertThat(extended).isNotNull()
        assertThat(extended!!.tronGaslessQuote).isEqualTo(quote)
        assertThat(extended.feeTokenId).isEqualTo(usdtToken.id)
        assertThat(extended.transactionFee.normal.amount.value).isEquivalentAccordingToCompareTo(BigDecimal("2.75"))
        assertThat(extended.transactionFee.normal.amount.currencySymbol).isEqualTo(usdtToken.symbol)
        // The 50 USDT send amount must reach the backend as raw base units (50 * 10^6).
        coVerify {
            repository.estimate(
                match { it.amount == "50000000" && it.feeTokenContract == usdtContract },
            )
        }
    }

    @Test
    fun `GIVEN estimate throws WHEN invoke THEN GaslessError DataError`() = runTest {
        // Arrange
        coEvery { repository.estimate(any()) } throws RuntimeException("boom")

        // Act
        val result = useCase(transactionData = transactionData, feeToken = usdtToken)

        // Assert
        assertThat(result.isLeft()).isTrue()
        assertThat(result.leftOrNull()).isInstanceOf(GetFeeError.GaslessError.DataError::class.java)
    }
}