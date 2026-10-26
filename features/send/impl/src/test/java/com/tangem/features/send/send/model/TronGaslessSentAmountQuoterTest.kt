package com.tangem.features.send.send.model

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.Token
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.common.test.domain.token.MockCryptoCurrencyFactory
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.tokens.IsAmountSubtractAvailableUseCase
import com.tangem.domain.transaction.error.GetFeeError
import com.tangem.domain.transaction.models.GaslessProtocolData
import com.tangem.domain.transaction.models.TransactionFeeExtended
import com.tangem.domain.transaction.models.tron.TronGaslessQuote
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal

internal class TronGaslessSentAmountQuoterTest {

    private val isAmountSubtractAvailableUseCase: IsAmountSubtractAvailableUseCase = mockk()
    private val quoter = TronGaslessSentAmountQuoter(isAmountSubtractAvailableUseCase)

    private val userWalletId = UserWalletId("0011223344556677")
    private val usdtContract = "TR7NHqjeKQxGTCi8q8ZY4pL8otSzgjLj6t"
    private val usdtCurrency = MockCryptoCurrencyFactory().createToken(
        blockchain = Blockchain.Tron,
        contractAddress = usdtContract,
        decimals = 6,
    )
    private val usdtToken = Token(symbol = "USDT", contractAddress = usdtContract, decimals = 6)
    private val balance = BigDecimal("26.157856")
    private val sentCurrencyStatus: CryptoCurrencyStatus = mockk {
        every { currency } returns usdtCurrency
        every { value.amount } returns balance
    }

    private val requotedAmounts = mutableListOf<BigDecimal>()

    @BeforeEach
    fun setUp() {
        clearMocks(isAmountSubtractAvailableUseCase)
        requotedAmounts.clear()
        coEvery { isAmountSubtractAvailableUseCase(userWalletId, usdtCurrency, any()) } returns true.right()
    }

    @Test
    fun `GIVEN balance covers amount and fee WHEN quoteForSentAmount THEN quote kept without re-quoting`() = runTest {
        // Arrange
        val fee = tronFee(quotedAmount = "10", compensation = "4.140160")

        // Act
        val result = quote(fee = fee, enteredAmount = BigDecimal("10")) { error("Must not re-quote") }

        // Assert
        assertThat(result).isEqualTo(fee.right())
    }

    @Test
    fun `GIVEN whole balance sent WHEN quoteForSentAmount THEN re-quoted for balance minus compensation`() = runTest {
        // Arrange
        val enteredQuote = tronFee(quotedAmount = "26.157856", compensation = "4.140160")
        val reducedQuote = tronFee(quotedAmount = "22.017696", compensation = "4.140160")

        // Act
        val result = quote(fee = enteredQuote, enteredAmount = balance) { reducedQuote.right() }

        // Assert
        assertThat(result).isEqualTo(reducedQuote.right())
        assertThat(requotedAmounts).containsExactly(BigDecimal("22.017696"))
    }

    @Test
    fun `GIVEN compensation grew on re-quote WHEN quoteForSentAmount THEN re-quoted until amount settles`() = runTest {
        // Arrange
        val enteredQuote = tronFee(quotedAmount = "26.157856", compensation = "4.140160")
        val firstQuote = tronFee(quotedAmount = "22.017696", compensation = "4.157059")
        val settledQuote = tronFee(quotedAmount = "22.000797", compensation = "4.157059")
        val quotes = ArrayDeque(listOf(firstQuote, settledQuote))

        // Act
        val result = quote(fee = enteredQuote, enteredAmount = balance) { quotes.removeFirst().right() }

        // Assert
        assertThat(result).isEqualTo(settledQuote.right())
        assertThat(requotedAmounts).containsExactly(BigDecimal("22.017696"), BigDecimal("22.000797")).inOrder()
    }

    @Test
    fun `GIVEN compensation never settles WHEN quoteForSentAmount THEN stops after three re-quotes`() = runTest {
        // Arrange
        val enteredQuote = tronFee(quotedAmount = "26.157856", compensation = "4.000000")
        var compensation = BigDecimal("4.000000")

        // Act
        quote(fee = enteredQuote, enteredAmount = balance) { amount ->
            compensation += BigDecimal("0.000001")
            tronFee(quotedAmount = amount.toPlainString(), compensation = compensation.toPlainString()).right()
        }

        // Assert
        assertThat(requotedAmounts).hasSize(3)
    }

    @Test
    fun `GIVEN re-quote fails WHEN quoteForSentAmount THEN last quote kept`() = runTest {
        // Arrange
        val enteredQuote = tronFee(quotedAmount = "26.157856", compensation = "4.140160")

        // Act
        val result = quote(fee = enteredQuote, enteredAmount = balance) {
            GetFeeError.UnknownError.left()
        }

        // Assert
        assertThat(result).isEqualTo(enteredQuote.right())
    }

    @Test
    fun `GIVEN fee is not a Tron gasless quote WHEN quoteForSentAmount THEN fee returned as is`() = runTest {
        // Arrange
        val nativeFee = TransactionFeeExtended(
            transactionFee = TransactionFee.Single(normal = Fee.Common(Amount(BigDecimal("13.4"), Blockchain.Tron))),
            feeTokenId = usdtCurrency.id,
        )

        // Act
        val result = quote(fee = nativeFee, enteredAmount = balance) { error("Must not re-quote") }

        // Assert
        assertThat(result).isEqualTo(nativeFee.right())
    }

    private suspend fun quote(
        fee: TransactionFeeExtended,
        enteredAmount: BigDecimal,
        requote: (BigDecimal) -> Either<GetFeeError, TransactionFeeExtended>,
    ) = quoter.quoteForSentAmount(
        fee = fee,
        userWalletId = userWalletId,
        sentCurrencyStatus = sentCurrencyStatus,
        enteredAmount = enteredAmount,
        reduceAmountBy = BigDecimal.ZERO,
        requote = { amount ->
            requotedAmounts += amount
            requote(amount)
        },
    )

    private fun tronFee(quotedAmount: String, compensation: String): TransactionFeeExtended {
        val compensationValue = BigDecimal(compensation)
        return TransactionFeeExtended(
            transactionFee = TransactionFee.Single(
                normal = Fee.Common(amount = Amount(token = usdtToken, value = compensationValue)),
            ),
            feeTokenId = usdtCurrency.id,
            gaslessProtocolData = GaslessProtocolData.Tron(
                quote = TronGaslessQuote(
                    quoteId = "q_$quotedAmount",
                    quotedAmountRaw = BigDecimal(quotedAmount).movePointRight(usdtToken.decimals).toBigInteger(),
                    quotedToAddress = "TTo",
                    feeRecipient = "TFee",
                    compensationToken = usdtContract,
                    compensationAmountRaw = compensationValue.movePointRight(usdtToken.decimals).toBigInteger(),
                    compensationAmountDecimal = compensationValue,
                    energy = 1,
                    bandwidth = 1,
                    trxCost = BigDecimal.ONE,
                    expiresAtEpochMs = 1,
                ),
            ),
        )
    }
}