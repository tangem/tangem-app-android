package com.tangem.feature.swap.converters

import com.google.common.truth.Truth.assertThat
import com.tangem.datasource.api.express.models.response.ExchangeDataResponse
import com.tangem.datasource.api.express.models.response.ExchangeDataResponseWithTxDetails
import com.tangem.datasource.api.express.models.response.TxDetails
import com.tangem.datasource.api.express.models.response.TxType
import com.tangem.feature.swap.domain.models.domain.ExpressTransactionModel
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.math.BigDecimal
import java.math.BigInteger

/**
 * Unit tests for [ExpressDataConverter].
 *
 * Covered:
 *  - SWAP -> DEX with all fields propagated (allowanceContract, gas).
 *  - SWAP with gas null -> DEX without throwing.
 *  - SWAP with allowanceContract null -> DEX with allowanceContract null.
 *  - otherNativeFee "0" -> BigDecimal.ZERO parse path.
 *  - SEND -> CEX with externalTxId/externalTxUrl preserved.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class ExpressDataConverterTest {

    private val sut = ExpressDataConverter()

    @Test
    fun `GIVEN txType SWAP with allowanceContract and gas WHEN convert THEN returns DEX with all fields`() {
        val dataResponse = buildDataResponse(fromAmount = "1000000000000000000", toAmount = "500000000000000000")
        val txDetails = buildTxDetails(
            txType = TxType.SWAP,
            txFrom = "0xFrom",
            txTo = "0xSwapContract",
            txData = "0xdeadbeef",
            txValue = "0",
            gas = "21000",
            allowanceContract = "0xSpender",
        )

        val result = sut.convert(ExchangeDataResponseWithTxDetails(dataResponse, txDetails))

        assertThat(result.transaction).isInstanceOf(ExpressTransactionModel.DEX::class.java)
        val dex = result.transaction as ExpressTransactionModel.DEX
        assertThat(dex.txFrom).isEqualTo("0xFrom")
        assertThat(dex.txTo).isEqualTo("0xSwapContract")
        assertThat(dex.txData).isEqualTo("0xdeadbeef")
        assertThat(dex.txValue).isEqualTo("0")
        assertThat(dex.gas).isEqualTo(BigInteger.valueOf(21_000L))
        assertThat(dex.allowanceContract).isEqualTo("0xSpender")
    }

    @Test
    fun `GIVEN txType SWAP with gas null WHEN convert THEN returns DEX with gas null without throwing`() {
        // Regression guard: the converter must accept a null gas value instead of raising.
        val dataResponse = buildDataResponse()
        val txDetails = buildTxDetails(txType = TxType.SWAP, gas = null)

        val result = sut.convert(ExchangeDataResponseWithTxDetails(dataResponse, txDetails))

        assertThat(result.transaction).isInstanceOf(ExpressTransactionModel.DEX::class.java)
        val dex = result.transaction as ExpressTransactionModel.DEX
        assertThat(dex.gas).isNull()
    }

    @Test
    fun `GIVEN txType SWAP with allowanceContract null WHEN convert THEN returns DEX with allowanceContract null`() {
        // Native EVM transfer / pre-approved scenario — no allowance required.
        val dataResponse = buildDataResponse()
        val txDetails = buildTxDetails(txType = TxType.SWAP, allowanceContract = null)

        val result = sut.convert(ExchangeDataResponseWithTxDetails(dataResponse, txDetails))

        assertThat(result.transaction).isInstanceOf(ExpressTransactionModel.DEX::class.java)
        val dex = result.transaction as ExpressTransactionModel.DEX
        assertThat(dex.allowanceContract).isNull()
    }

    @Test
    fun `GIVEN txType SWAP with otherNativeFee zero string WHEN convert THEN returns DEX with otherNativeFeeWei zero`() {
        // "0" string must round-trip to BigDecimal.ZERO without parse errors.
        val dataResponse = buildDataResponse()
        val txDetails = buildTxDetails(txType = TxType.SWAP, otherNativeFee = "0")

        val result = sut.convert(ExchangeDataResponseWithTxDetails(dataResponse, txDetails))

        val dex = result.transaction as ExpressTransactionModel.DEX
        assertThat(dex.otherNativeFeeWei).isEquivalentAccordingToCompareTo(BigDecimal.ZERO)
    }

    @Test
    fun `GIVEN txType SEND with externalTxId and externalTxUrl WHEN convert THEN returns CEX`() {
        val dataResponse = buildDataResponse()
        val txDetails = buildTxDetails(
            txType = TxType.SEND,
            txFrom = null,
            txTo = "0xCexDepositAddress",
            txData = null,
            externalTxId = "ext-tx-id-1",
            externalTxUrl = "https://explorer.example/tx/ext-tx-id-1",
            txExtraIdName = "memo",
            txExtraId = "12345",
        )

        val result = sut.convert(ExchangeDataResponseWithTxDetails(dataResponse, txDetails))

        assertThat(result.transaction).isInstanceOf(ExpressTransactionModel.CEX::class.java)
        val cex = result.transaction as ExpressTransactionModel.CEX
        assertThat(cex.txTo).isEqualTo("0xCexDepositAddress")
        assertThat(cex.externalTxId).isEqualTo("ext-tx-id-1")
        assertThat(cex.externalTxUrl).isEqualTo("https://explorer.example/tx/ext-tx-id-1")
        assertThat(cex.txExtraIdName).isEqualTo("memo")
        assertThat(cex.txExtraId).isEqualTo("12345")
    }

    // -------------------------------------------------------------------------
    // Builders
    // -------------------------------------------------------------------------

    private fun buildDataResponse(
        fromAmount: String = "1000000000000000000",
        fromDecimals: Int = 18,
        toAmount: String = "500000",
        toDecimals: Int = 6,
        txId: String = "inner-tx-id",
    ): ExchangeDataResponse = ExchangeDataResponse(
        fromAmount = fromAmount,
        fromDecimals = fromDecimals,
        toAmount = toAmount,
        toDecimals = toDecimals,
        txId = txId,
        txDetailsJson = "{}",
        signature = "sig",
    )

    @Suppress("LongParameterList")
    private fun buildTxDetails(
        txType: TxType = TxType.SWAP,
        payoutAddress: String = "0xPayout",
        requestId: String = "req-1",
        txFrom: String? = "0xFrom",
        txTo: String = "0xTo",
        txData: String? = "0xdata",
        txValue: String? = "0",
        otherNativeFee: String? = null,
        externalTxId: String? = null,
        externalTxUrl: String? = null,
        txExtraIdName: String? = null,
        txExtraId: String? = null,
        gas: String? = "21000",
        allowanceContract: String? = null,
    ): TxDetails = TxDetails(
        payoutAddress = payoutAddress,
        requestId = requestId,
        txType = txType,
        txFrom = txFrom,
        txTo = txTo,
        txData = txData,
        txValue = txValue,
        otherNativeFee = otherNativeFee,
        externalTxId = externalTxId,
        externalTxUrl = externalTxUrl,
        txExtraIdName = txExtraIdName,
        txExtraId = txExtraId,
        gas = gas,
        allowanceContract = allowanceContract,
    )
}