package com.tangem.domain.transaction.usecase.gasless

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.Token
import com.tangem.blockchain.common.TransactionData
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.common.test.domain.token.MockCryptoCurrencyFactory
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.transaction.error.SendTransactionError
import com.tangem.domain.transaction.models.TransactionFeeExtended
import com.tangem.domain.transaction.models.tron.TronGaslessQuote
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.math.BigInteger

/** Whichever resolver produced the fee must also send it. */
internal class CreateAndSendGaslessTransactionUseCaseTest {

    private val evmUseCase: CreateAndSendEvmGaslessTransactionUseCase = mockk()
    private val tronUseCase: CreateAndSendTronGaslessTransactionUseCase = mockk()

    private val useCase = CreateAndSendGaslessTransactionUseCase(
        createAndSendEvmGaslessTransactionUseCase = evmUseCase,
        createAndSendTronGaslessTransactionUseCase = tronUseCase,
    )

    private val userWallet: UserWallet = mockk()
    private val transactionData: TransactionData = mockk()
    private val usdtContract = "TR7NHqjeKQxGTCi8q8ZY4pL8otSzgjLj6t"
    private val usdtCurrency = MockCryptoCurrencyFactory().createToken(
        blockchain = Blockchain.Tron,
        contractAddress = usdtContract,
    )

    @Test
    fun `GIVEN fee carrying a Tron gasless quote WHEN invoke THEN the Tron flow sends it`() = runTest {
        // Arrange
        val fee = TransactionFeeExtended(
            transactionFee = TransactionFee.Single(
                normal = Fee.Common(
                    amount = Amount(
                        token = Token(symbol = "USDT", contractAddress = usdtContract, decimals = 6),
                        value = BigDecimal("2.75"),
                    ),
                ),
            ),
            feeTokenId = usdtCurrency.id,
            tronGaslessQuote = TronGaslessQuote(
                quoteId = "q_1",
                quotedAmountRaw = BigInteger("50000000"),
                quotedToAddress = "TRecipient",
                feeRecipient = "TFee",
                compensationToken = usdtContract,
                compensationAmountRaw = BigInteger("2750000"),
                compensationAmountDecimal = BigDecimal("2.75"),
                energy = 1,
                bandwidth = 1,
                trxCost = BigDecimal("1"),
                expiresAtEpochMs = 1,
            ),
        )
        coEvery { tronUseCase(userWallet, transactionData, fee) } returns TX_HASH.right()

        // Act
        val actual = useCase(userWallet = userWallet, transactionData = transactionData, fee = fee)

        // Assert
        assertThat(actual).isEqualTo(TX_HASH.right())
        coVerify(exactly = 0) { evmUseCase(any(), any(), any()) }
    }

    @Test
    fun `GIVEN token-denominated EVM fee WHEN invoke THEN the EVM flow sends it`() = runTest {
        // Arrange
        val fee = TransactionFeeExtended(
            transactionFee = TransactionFee.Single(normal = mockk<Fee.Ethereum.TokenCurrency>()),
            feeTokenId = usdtCurrency.id,
        )
        coEvery { evmUseCase(userWallet, transactionData, fee) } returns TX_HASH.right()

        // Act
        val actual = useCase(userWallet = userWallet, transactionData = transactionData, fee = fee)

        // Assert
        assertThat(actual).isEqualTo(TX_HASH.right())
        coVerify(exactly = 0) { tronUseCase(any(), any(), any()) }
    }

    @Test
    fun `GIVEN a native fee WHEN invoke THEN neither flow runs`() = runTest {
        // Arrange — a native fee belongs to SendTransactionUseCase, not here.
        val fee = TransactionFeeExtended(
            transactionFee = TransactionFee.Single(
                normal = Fee.Common(amount = Amount(blockchain = Blockchain.Ethereum, value = BigDecimal.ONE)),
            ),
            feeTokenId = usdtCurrency.id,
        )

        // Act
        val actual = useCase(userWallet = userWallet, transactionData = transactionData, fee = fee)

        // Assert
        assertThat(actual).isEqualTo(SendTransactionError.DataError("Gasless send called with a native fee").left())
        coVerify(exactly = 0) { evmUseCase(any(), any(), any()) }
        coVerify(exactly = 0) { tronUseCase(any(), any(), any()) }
    }

    private companion object {
        const val TX_HASH = "0xhash"
    }
}