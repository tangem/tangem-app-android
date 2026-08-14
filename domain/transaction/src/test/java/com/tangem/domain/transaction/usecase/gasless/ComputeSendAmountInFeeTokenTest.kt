package com.tangem.domain.transaction.usecase.gasless

import arrow.core.raise.either
import com.tangem.blockchain.blockchains.ethereum.EthereumTransactionExtras
import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.AmountType
import com.tangem.blockchain.common.Token
import com.tangem.blockchain.common.TransactionData
import com.tangem.blockchain.yieldsupply.providers.ethereum.yield.EthereumYieldSupplySendCallData
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.transaction.error.GetFeeError
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.math.BigDecimal

/**
 * Unit tests for [computeSendAmountInFeeToken].
 *
 * Cases:
 * (a) Different token → ZERO (fee token ≠ sent token).
 * (b) Same token via AmountType.Token → the actual sent amount.
 * (c) Same token via AmountType.TokenYieldSupply → the actual sent amount.
 * (d) Same token but amount.value == null → raises (loud error, never silent ZERO).
 * (e) Compiled tx → raises (gasless token-fee requires uncompiled data).
 */
class ComputeSendAmountInFeeTokenTest {

    private val feeContract = "0xUSDC"
    private val otherContract = "0xDAI"
    private val sentAmount = BigDecimal("50.0")

    private fun makeToken(contract: String) = Token(
        name = "TestToken",
        symbol = "TST",
        contractAddress = contract,
        decimals = 6,
    )

    private fun uncompiledWith(type: AmountType, value: BigDecimal?) = TransactionData.Uncompiled(
        amount = Amount(
            currencySymbol = "TST",
            value = value,
            maxValue = null,
            decimals = 6,
            type = type,
        ),
        sourceAddress = "0xSrc",
        destinationAddress = "0xDst",
        fee = null,
    )

    // (a) Sent token is different from fee token → ZERO
    @Test
    fun `returns ZERO when sent token differs from fee token`() {
        val tx = uncompiledWith(
            type = AmountType.Token(makeToken(otherContract)),
            value = sentAmount,
        )

        val result = either<GetFeeError, BigDecimal> {
            computeSendAmountInFeeToken(tx, feeContract)
        }

        assertTrue(result.isRight())
        assertEquals(BigDecimal.ZERO, result.getOrNull())
    }

    // (b) AmountType.Token — same contract as fee token → returns the sent amount
    @Test
    fun `returns sent amount when AmountType Token matches fee token contract`() {
        val tx = uncompiledWith(
            type = AmountType.Token(makeToken(feeContract)),
            value = sentAmount,
        )

        val result = either<GetFeeError, BigDecimal> {
            computeSendAmountInFeeToken(tx, feeContract)
        }

        assertTrue(result.isRight())
        assertEquals(sentAmount, result.getOrNull())
    }

    // (b) Case-insensitive contract address match
    @Test
    fun `contract address comparison is case-insensitive`() {
        val tx = uncompiledWith(
            type = AmountType.Token(makeToken(feeContract.uppercase())),
            value = sentAmount,
        )

        val result = either<GetFeeError, BigDecimal> {
            computeSendAmountInFeeToken(tx, feeContract.lowercase())
        }

        assertTrue(result.isRight())
        assertEquals(sentAmount, result.getOrNull())
    }

    // (c) AmountType.TokenYieldSupply — same contract as fee token → returns the sent amount
    @Test
    fun `returns sent amount when AmountType TokenYieldSupply matches fee token contract`() {
        val tx = uncompiledWith(
            type = AmountType.TokenYieldSupply(
                token = makeToken(feeContract),
                isActive = true,
                isInitialized = true,
                isAllowedToSpend = true,
            ),
            value = sentAmount,
        )

        val result = either<GetFeeError, BigDecimal> {
            computeSendAmountInFeeToken(tx, feeContract)
        }

        assertTrue(result.isRight())
        assertEquals(sentAmount, result.getOrNull())
    }

    // (d) Same token but amount.value == null → raises (never silently under-accounts as ZERO)
    @Test
    fun `raises when same token is sent but amount value is null`() {
        val tx = uncompiledWith(
            type = AmountType.Token(makeToken(feeContract)),
            value = null,
        )

        val result = either<GetFeeError, BigDecimal> {
            computeSendAmountInFeeToken(tx, feeContract)
        }

        assertTrue(result.isLeft(), "Expected Left (error) when sent amount is null")
        assertTrue(
            result.leftOrNull() is GetFeeError.DataError,
            "Expected GetFeeError.DataError wrapping IllegalStateException",
        )
    }

    /**
     * A yield-supply send keeps the real amount in the module call data and zeroes TransactionData.amount,
     * so the caller-supplied amount must win.
     */
    @Test
    fun `returns caller amount for a yield-supply send whose transaction amount is zeroed`() {
        val tx = yieldSupplySendTx(value = BigDecimal.ZERO)

        val result = either<GetFeeError, BigDecimal> {
            computeSendAmountInFeeToken(tx, feeContract, sentAmount)
        }

        assertTrue(result.isRight())
        assertEquals(sentAmount, result.getOrNull())
    }

    // Without the caller-supplied amount the plan would silently under-account the send — raise instead.
    @Test
    fun `raises for a yield-supply send when the sent amount is not supplied`() {
        val tx = yieldSupplySendTx(value = BigDecimal.ZERO)

        val result = either<GetFeeError, BigDecimal> {
            computeSendAmountInFeeToken(tx, feeContract)
        }

        assertTrue(result.isLeft(), "Expected Left (error) when a yield-supply send has no explicit amount")
        assertTrue(
            result.leftOrNull() is GetFeeError.DataError,
            "Expected GetFeeError.DataError wrapping IllegalStateException",
        )
    }

    // A different fee token short-circuits to ZERO before the yield-supply branch is reached.
    @Test
    fun `returns ZERO for a yield-supply send paid in another token`() {
        val tx = yieldSupplySendTx(value = BigDecimal.ZERO, contract = otherContract)

        val result = either<GetFeeError, BigDecimal> {
            computeSendAmountInFeeToken(tx, feeContract)
        }

        assertTrue(result.isRight())
        assertEquals(BigDecimal.ZERO, result.getOrNull())
    }

    private fun yieldSupplySendTx(value: BigDecimal?, contract: String = feeContract): TransactionData.Uncompiled {
        val token = makeToken(contract)
        return uncompiledWith(
            type = AmountType.TokenYieldSupply(
                token = token,
                isActive = true,
                isInitialized = true,
                isAllowedToSpend = true,
            ),
            value = value,
        ).copy(
            extras = EthereumTransactionExtras(
                callData = EthereumYieldSupplySendCallData(
                    tokenContractAddress = contract,
                    destinationAddress = "0xDst",
                    amount = Amount(value = sentAmount, token = token),
                ),
            ),
        )
    }

    /**
     * The estimate-path overload used by [EstimateFeeForGaslessTxUseCase] / [EstimateFeeForTokenUseCase]:
     * no transaction exists yet, so the sent amount is supplied by the caller ([REDACTED_TASK_KEY]).
     */
    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class FromSendingCurrency {

        @Test
        fun `GIVEN fee token equals sent token WHEN compute THEN returns the sent amount`() {
            // Arrange
            val sendingCurrency = token(feeContract)

            // Act
            val actual = computeSendAmountInFeeToken(sendingCurrency, feeContract, sentAmount)

            // Assert
            assertEquals(sentAmount, actual)
        }

        @Test
        fun `GIVEN contracts differ only in case WHEN compute THEN returns the sent amount`() {
            // Arrange
            val sendingCurrency = token(feeContract.uppercase())

            // Act
            val actual = computeSendAmountInFeeToken(sendingCurrency, feeContract.lowercase(), sentAmount)

            // Assert
            assertEquals(sentAmount, actual)
        }

        @Test
        fun `GIVEN a different fee token WHEN compute THEN returns ZERO`() {
            // Arrange
            val sendingCurrency = token(otherContract)

            // Act
            val actual = computeSendAmountInFeeToken(sendingCurrency, feeContract, sentAmount)

            // Assert
            assertEquals(BigDecimal.ZERO, actual)
        }

        @Test
        fun `GIVEN a coin is being sent WHEN compute THEN returns ZERO`() {
            // Arrange
            val sendingCurrency = mockk<CryptoCurrency.Coin>()

            // Act
            val actual = computeSendAmountInFeeToken(sendingCurrency, feeContract, sentAmount)

            // Assert
            assertEquals(BigDecimal.ZERO, actual)
        }

        private fun token(contract: String): CryptoCurrency.Token = mockk {
            every { contractAddress } returns contract
        }
    }

    // (e) Compiled tx → raises (gasless token-fee requires uncompiled data)
    @Test
    fun `raises when transactionData is Compiled`() {
        val compiled = TransactionData.Compiled(
            value = TransactionData.Compiled.Data.Bytes(byteArrayOf(0x01, 0x02)),
        )

        val result = either<GetFeeError, BigDecimal> {
            computeSendAmountInFeeToken(compiled, feeContract)
        }

        assertTrue(result.isLeft(), "Expected Left (error) for compiled tx")
        assertTrue(
            result.leftOrNull() is GetFeeError.DataError,
            "Expected GetFeeError.DataError wrapping IllegalStateException",
        )
    }
}