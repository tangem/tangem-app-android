package com.tangem.feature.swap.domain.tron

import com.google.common.truth.Truth.assertThat
import com.squareup.wire.AnyMessage
import com.tangem.blockchain.extensions.decodeBase58
import com.tangem.common.extensions.hexToBytes
import com.tangem.common.extensions.toHexString
import com.tangem.feature.swap.domain.models.SwapAmount
import com.tangem.feature.swap.domain.models.domain.ExpressTransactionModel
import okio.ByteString.Companion.toByteString
import org.junit.jupiter.api.Test
import org.tron.protos.Transaction
import org.tron.protos.contract.TransferContract
import org.tron.protos.contract.TriggerSmartContract
import java.math.BigDecimal

/**
 * Providers deliver a TRON swap as a whole serialized `Transaction.raw`, in one of two shapes:
 * a `TriggerSmartContract` calling the provider's router (li-fi), or a `TransferContract`
 * depositing to its address (SwapKit). Both are unpacked here and rebuilt locally rather than
 * signed as delivered.
 *
 * Fixtures are assembled from synthetic addresses rather than captured from a live quote, so no
 * real wallet or provider address ends up in the repository. They still reproduce the differences
 * observed between the two providers: li-fi `0x`-prefixes its payload and sets a fee limit,
 * SwapKit does neither.
 */
internal class TronSwapPayloadTest {

    @Test
    fun `GIVEN router call WHEN tronSwapPayload THEN nested call data extracted`() {
        // Arrange
        val transaction = createDexTransaction(
            txData = "0x" + serializedRouterCall(callValue = TX_VALUE_SUN),
            txValue = TX_VALUE_SUN.toString(),
            txTo = ROUTER,
        )

        // Act
        val actual = transaction.tronSwapPayload()

        // Assert
        val call = actual as TronSwapPayload.ContractCall
        // The SDK renders hex upper-case; only the bytes matter downstream.
        assertThat(call.callDataHex.lowercase()).isEqualTo(ROUTER_CALL_DATA)
        assertThat(call.memo).isNull()
    }

    @Test
    fun `GIVEN deposit transfer WHEN tronSwapPayload THEN resolved as plain transfer`() {
        // Arrange
        val transaction = createDexTransaction(
            txData = serializedTransfer(amount = TX_VALUE_SUN),
            txValue = TX_VALUE_SUN.toString(),
            txTo = DEPOSIT,
        )

        // Act
        val actual = transaction.tronSwapPayload()

        // Assert
        assertThat(actual).isEqualTo(TronSwapPayload.Transfer(memo = null))
    }

    @Test
    fun `GIVEN transfer with memo WHEN tronSwapPayload THEN memo carried over`() {
        // Arrange
        val transaction = createDexTransaction(
            txData = serializedTransfer(amount = TX_VALUE_SUN, memo = MEMO),
            txValue = TX_VALUE_SUN.toString(),
            txTo = DEPOSIT,
        )

        // Act
        val actual = transaction.tronSwapPayload()

        // Assert
        val transfer = actual as TronSwapPayload.Transfer
        assertThat(transfer.memo!!.decodeToString()).isEqualTo(MEMO)
    }

    @Test
    fun `GIVEN router call with memo WHEN tronSwapPayload THEN memo carried over`() {
        // Arrange
        val transaction = createDexTransaction(
            txData = serializedRouterCall(callValue = TX_VALUE_SUN, memo = MEMO),
            txValue = TX_VALUE_SUN.toString(),
            txTo = ROUTER,
        )

        // Act
        val actual = transaction.tronSwapPayload()

        // Assert
        val call = actual as TronSwapPayload.ContractCall
        assertThat(call.callDataHex.lowercase()).isEqualTo(ROUTER_CALL_DATA)
        assertThat(call.memo!!.decodeToString()).isEqualTo(MEMO)
    }

    @Test
    fun `GIVEN transfer with memo WHEN toTransactionExtras THEN memo attached without call data`() {
        // Arrange
        val payload = TronSwapPayload.Transfer(memo = MEMO.encodeToByteArray())

        // Act
        val actual = payload.toTransactionExtras()

        // Assert
        assertThat(actual).isNotNull()
        assertThat(actual!!.callData).isNull()
        assertThat(actual.memo!!.decodeToString()).isEqualTo(MEMO)
    }

    @Test
    fun `GIVEN transfer without memo WHEN toTransactionExtras THEN no extras needed`() {
        // Arrange
        val payload = TronSwapPayload.Transfer(memo = null)

        // Act
        val actual = payload.toTransactionExtras()

        // Assert
        assertThat(actual).isNull()
    }

    @Test
    fun `GIVEN router call WHEN toTransactionExtras THEN call data attached`() {
        // Arrange
        val payload = TronSwapPayload.ContractCall(callDataHex = ROUTER_CALL_DATA, memo = null)

        // Act
        val actual = payload.toTransactionExtras()

        // Assert
        assertThat(actual!!.callData!!.data.toHexString().lowercase()).isEqualTo(ROUTER_CALL_DATA)
        assertThat(actual.memo).isNull()
    }

    @Test
    fun `GIVEN call value disagrees with txValue WHEN tronSwapPayload THEN null`() {
        // Arrange
        val transaction = createDexTransaction(
            txData = serializedRouterCall(callValue = TX_VALUE_SUN),
            txValue = (TX_VALUE_SUN - 1).toString(),
            txTo = ROUTER,
        )

        // Act
        val actual = transaction.tronSwapPayload()

        // Assert
        assertThat(actual).isNull()
    }

    @Test
    fun `GIVEN transfer amount disagrees with txValue WHEN tronSwapPayload THEN null`() {
        // Arrange
        val transaction = createDexTransaction(
            txData = serializedTransfer(amount = TX_VALUE_SUN),
            txValue = (TX_VALUE_SUN - 1).toString(),
            txTo = DEPOSIT,
        )

        // Act
        val actual = transaction.tronSwapPayload()

        // Assert
        assertThat(actual).isNull()
    }

    @Test
    fun `GIVEN router address disagrees with txTo WHEN tronSwapPayload THEN null`() {
        // Arrange
        val transaction = createDexTransaction(
            txData = serializedRouterCall(callValue = TX_VALUE_SUN),
            txValue = TX_VALUE_SUN.toString(),
            txTo = UNRELATED,
        )

        // Act
        val actual = transaction.tronSwapPayload()

        // Assert
        assertThat(actual).isNull()
    }

    @Test
    fun `GIVEN deposit address disagrees with txTo WHEN tronSwapPayload THEN null`() {
        // Arrange
        val transaction = createDexTransaction(
            txData = serializedTransfer(amount = TX_VALUE_SUN),
            txValue = TX_VALUE_SUN.toString(),
            txTo = UNRELATED,
        )

        // Act
        val actual = transaction.tronSwapPayload()

        // Assert
        assertThat(actual).isNull()
    }

    @Test
    fun `GIVEN empty router call data WHEN tronSwapPayload THEN null`() {
        // Arrange
        val transaction = createDexTransaction(
            txData = serializedRouterCall(callValue = TX_VALUE_SUN, callData = ByteArray(size = 0)),
            txValue = TX_VALUE_SUN.toString(),
            txTo = ROUTER,
        )

        // Act
        val actual = transaction.tronSwapPayload()

        // Assert
        assertThat(actual).isNull()
    }

    @Test
    fun `GIVEN no txValue WHEN tronSwapPayload THEN payload still resolved`() {
        // Arrange
        val transaction = createDexTransaction(
            txData = serializedRouterCall(callValue = TX_VALUE_SUN),
            txValue = null,
            txTo = ROUTER,
        )

        // Act
        val actual = transaction.tronSwapPayload()

        // Assert
        assertThat((actual as TronSwapPayload.ContractCall).callDataHex.lowercase()).isEqualTo(ROUTER_CALL_DATA)
    }

    @Test
    fun `GIVEN plain evm call data WHEN tronSwapPayload THEN passed through unchanged`() {
        // Arrange
        val transaction = createDexTransaction(txData = "0x$ROUTER_CALL_DATA", txValue = "4000000000000000")

        // Act
        val actual = transaction.tronSwapPayload()

        // Assert
        assertThat(actual).isEqualTo(TronSwapPayload.ContractCall(callDataHex = "0x$ROUTER_CALL_DATA", memo = null))
    }

    @Test
    fun `GIVEN malformed hex WHEN tronSwapPayload THEN passed through unchanged`() {
        // Arrange
        val transaction = createDexTransaction(txData = "0xnothex", txValue = "1")

        // Act
        val actual = transaction.tronSwapPayload()

        // Assert
        assertThat(actual).isEqualTo(TronSwapPayload.ContractCall(callDataHex = "0xnothex", memo = null))
    }

    /**
     * A `TriggerSmartContract` transaction against [ROUTER], mirroring the li-fi shape: the swap
     * amount travels as `call_value` and the provider's offer as the call data, plus a fee limit.
     */
    private fun serializedRouterCall(
        callValue: Long,
        callData: ByteArray = ROUTER_CALL_DATA.hexToBytes(),
        memo: String? = null,
    ): String {
        val trigger = TriggerSmartContract(
            owner_address = SENDER.toAddressBytes(),
            contract_address = ROUTER.toAddressBytes(),
            call_value = callValue,
            data_ = callData.toByteString(),
        )
        return serializedTransaction(
            contractType = Transaction.Contract.ContractType.TriggerSmartContract,
            parameter = AnyMessage.pack(trigger),
            feeLimit = FEE_LIMIT_SUN,
            memo = memo,
        )
    }

    /** A `TransferContract` transaction to [DEPOSIT], mirroring the SwapKit shape (no fee limit). */
    private fun serializedTransfer(amount: Long, memo: String? = null): String {
        val transfer = TransferContract(
            owner_address = SENDER.toAddressBytes(),
            to_address = DEPOSIT.toAddressBytes(),
            amount = amount,
        )
        return serializedTransaction(
            contractType = Transaction.Contract.ContractType.TransferContract,
            parameter = AnyMessage.pack(transfer),
            feeLimit = 0,
            memo = memo,
        )
    }

    private fun serializedTransaction(
        contractType: Transaction.Contract.ContractType,
        parameter: AnyMessage,
        feeLimit: Long,
        memo: String?,
    ): String = Transaction.raw(
        contract = listOf(Transaction.Contract(type = contractType, parameter = parameter)),
        fee_limit = feeLimit,
        data_ = memo?.encodeToByteArray()?.toByteString() ?: okio.ByteString.EMPTY,
    ).encode().toHexString().lowercase()

    private fun String.toAddressBytes() = decodeBase58(checked = true)!!.toByteString()

    private fun createDexTransaction(
        txData: String,
        txValue: String?,
        txTo: String = ROUTER,
    ) = ExpressTransactionModel.DEX(
        fromAmount = SwapAmount(BigDecimal.ONE, decimals = 6),
        toAmount = SwapAmount(BigDecimal.ONE, decimals = 18),
        txValue = txValue,
        txId = "tx-id-123",
        txTo = txTo,
        txExtraId = null,
        txFrom = SENDER,
        txData = txData,
        otherNativeFeeWei = null,
        gas = null,
    )

    private companion object {

        /** Synthetic addresses: `0x41` + a repeated byte, so none of them belongs to anyone. */
        const val SENDER = "TBXSw8fM4jpQkGc6zZjsVABFpVN7UvXPdV"
        const val ROUTER = "TD5gsCwxykWsLN9aPrq2TAfNjByuZKYp4E"
        const val DEPOSIT = "TEdvoHEatmDKvTh3o9vBRB9Vdtbhn4QFhy"
        const val UNRELATED = "TGCAjMXComunWZEXCT1LPBdcYbDVuyexBv"

        const val TX_VALUE_SUN = 50_000_000L
        const val FEE_LIMIT_SUN = 150_000_000L

        /** Router entry-point selector followed by a stand-in for the provider's offer blob. */
        const val ROUTER_CALL_DATA = "3110c7b9" + "00112233445566778899aabbccddeeff"

        const val MEMO = "=:ETH.ETH:0xRecipient"
    }
}