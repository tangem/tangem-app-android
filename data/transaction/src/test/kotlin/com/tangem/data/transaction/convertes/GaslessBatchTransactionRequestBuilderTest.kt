package com.tangem.data.transaction.convertes

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.transaction.models.Eip7702Authorization
import com.tangem.domain.transaction.models.GaslessBatchTransactionData
import com.tangem.domain.transaction.models.GaslessTransactionData
import org.junit.jupiter.api.Test
import java.math.BigInteger

class GaslessBatchTransactionRequestBuilderTest {

    private val builder = GaslessBatchTransactionRequestBuilder()

    // byteArrayOf(0x12, 0x34).toHexString() == "1234" (uppercase), .formatHex() prepends "0x" → "0x1234"
    private val tx1Data = byteArrayOf(0x12, 0x34)
    private val tx1DataHex = "0x1234"

    // byteArrayOf(0xAB.toByte(), 0xCD.toByte()).toHexString() == "ABCD", .formatHex() → "0xABCD"
    private val tx2Data = byteArrayOf(0xAB.toByte(), 0xCD.toByte())
    private val tx2DataHex = "0xABCD"

    private val tx1 = GaslessTransactionData.Transaction(
        to = "0xContractA",
        value = BigInteger("100"),
        gasLimit = BigInteger("120000"),
        data = tx1Data,
    )
    private val tx2 = GaslessTransactionData.Transaction(
        to = "0xContractB",
        value = BigInteger("0"),
        gasLimit = BigInteger("150000"),
        data = tx2Data,
    )
    private val fee = GaslessTransactionData.Fee(
        feeToken = "0xFeeToken",
        maxTokenFee = BigInteger("500"),
        coinPriceInToken = BigInteger("200"),
        feeTransferGasLimit = BigInteger("21000"),
        baseGas = BigInteger("60000"),
        feeReceiver = "0xFeeReceiver",
    )
    private val nonce = BigInteger("42")

    private val batchData = GaslessBatchTransactionData(
        transactions = listOf(tx1, tx2),
        fee = fee,
        nonce = nonce,
    )

    @Test
    fun `build - transactions list has correct size and order`() {
        val result = builder.build(
            gaslessBatchTransaction = batchData,
            signature = "0xSig",
            userAddress = "0xUser",
            chainId = 1,
        )

        assertThat(result.gaslessTransaction.transactions).hasSize(2)
        assertThat(result.gaslessTransaction.transactions[0].to).isEqualTo("0xContractA")
        assertThat(result.gaslessTransaction.transactions[1].to).isEqualTo("0xContractB")
    }

    @Test
    fun `build - transaction data fields are encoded correctly`() {
        val result = builder.build(
            gaslessBatchTransaction = batchData,
            signature = "0xSig",
            userAddress = "0xUser",
            chainId = 1,
        )

        val txDtoList = result.gaslessTransaction.transactions
        // data bytes are hex-encoded with 0x prefix (uppercase)
        assertThat(txDtoList[0].data).isEqualTo(tx1DataHex)
        assertThat(txDtoList[1].data).isEqualTo(tx2DataHex)
        // value is BigInteger.toString()
        assertThat(txDtoList[0].value).isEqualTo("100")
        assertThat(txDtoList[1].value).isEqualTo("0")
        // v2: per-call gasLimit is BigInteger.toString()
        assertThat(txDtoList[0].gasLimit).isEqualTo("120000")
        assertThat(txDtoList[1].gasLimit).isEqualTo("150000")
    }

    @Test
    fun `build - fee fields are all toString of BigInteger inputs`() {
        val result = builder.build(
            gaslessBatchTransaction = batchData,
            signature = "0xSig",
            userAddress = "0xUser",
            chainId = 1,
        )

        val feeDto = result.gaslessTransaction.fee
        assertThat(feeDto.feeToken).isEqualTo("0xFeeToken")
        assertThat(feeDto.maxTokenFee).isEqualTo("500")
        assertThat(feeDto.coinPriceInToken).isEqualTo("200")
        assertThat(feeDto.feeTransferGasLimit).isEqualTo("21000")
        assertThat(feeDto.baseGas).isEqualTo("60000")
        assertThat(feeDto.feeReceiver).isEqualTo("0xFeeReceiver")
    }

    @Test
    fun `build - nonce is toString of BigInteger input`() {
        val result = builder.build(
            gaslessBatchTransaction = batchData,
            signature = "0xSig",
            userAddress = "0xUser",
            chainId = 1,
        )

        assertThat(result.gaslessTransaction.nonce).isEqualTo("42")
    }

    @Test
    fun `build - top-level signature, userAddress, chainId pass through`() {
        val result = builder.build(
            gaslessBatchTransaction = batchData,
            signature = "0xDeadBeef",
            userAddress = "0xAlice",
            chainId = 137,
        )

        assertThat(result.signature).isEqualTo("0xDeadBeef")
        assertThat(result.userAddress).isEqualTo("0xAlice")
        assertThat(result.chainId).isEqualTo(137)
    }

    @Test
    fun `build - eip7702Auth is null when not provided`() {
        val result = builder.build(
            gaslessBatchTransaction = batchData,
            signature = "0xSig",
            userAddress = "0xUser",
            chainId = 1,
        )

        assertThat(result.eip7702Auth).isNull()
    }

    @Test
    fun `build - eip7702Auth maps correctly when provided`() {
        val auth = Eip7702Authorization(
            chainId = 1,
            address = "0xEntryPoint",
            nonce = BigInteger("7"),
            yParity = 0,
            r = "0xRValue",
            s = "0xSValue",
        )

        val result = builder.build(
            gaslessBatchTransaction = batchData,
            signature = "0xSig",
            userAddress = "0xUser",
            chainId = 1,
            eip7702Auth = auth,
        )

        val authDto = result.eip7702Auth
        assertThat(authDto).isNotNull()
        assertThat(authDto!!.chainId).isEqualTo(1)
        assertThat(authDto.address).isEqualTo("0xEntryPoint")
        assertThat(authDto.nonce).isEqualTo("7")
        assertThat(authDto.yParity).isEqualTo(0)
        assertThat(authDto.r).isEqualTo("0xRValue")
        assertThat(authDto.s).isEqualTo("0xSValue")
    }
}