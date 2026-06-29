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
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.math.BigInteger

internal class GetTronGaslessFeeUseCaseTest {

    private val repository: TronGaslessTransactionRepository = mockk()
    private val useCase = GetTronGaslessFeeUseCase(repository)

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
        compensationAmountRaw = BigInteger("2750000"),
        compensationAmountDecimal = BigDecimal("2.75"),
        energy = 78000,
        bandwidth = 345,
        trxCostSun = BigInteger("27500000"),
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
        assertThat(extended.transactionFee.normal.amount.value).isEqualTo(BigDecimal("2.75"))
        assertThat(extended.transactionFee.normal.amount.currencySymbol).isEqualTo(usdtToken.symbol)
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