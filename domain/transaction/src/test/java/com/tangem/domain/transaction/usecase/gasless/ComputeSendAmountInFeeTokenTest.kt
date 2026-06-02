package com.tangem.domain.transaction.usecase.gasless

import arrow.core.raise.either
import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.AmountType
import com.tangem.blockchain.common.Token
import com.tangem.blockchain.common.TransactionData
import com.tangem.domain.transaction.error.GetFeeError
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
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