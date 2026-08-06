package com.tangem.feature.swap.domain.tron

import com.tangem.blockchain.blockchains.tron.TronTransactionExtras
import com.tangem.blockchain.common.smartcontract.CompiledSmartContractCallData
import com.tangem.blockchain.extensions.decodeBase58
import com.tangem.common.extensions.hexToBytes
import com.tangem.common.extensions.toHexString
import com.tangem.feature.swap.domain.models.domain.ExpressTransactionModel
import org.tron.protos.Transaction
import org.tron.protos.contract.TransferContract
import org.tron.protos.contract.TriggerSmartContract

/**
 * What the app has to build for a TRON swap once the provider's payload is unpacked.
 *
 * Both shapes are rebuilt locally rather than signed as delivered: the provider sets the expiration
 * when it builds the transaction (observed ~59 s for li-fi, ~5 min for SwapKit), which does not
 * reliably survive an NFC signing session on a cold wallet. Rebuilding leaves the provider's offer
 * untouched — for a contract call the offer parameters, deadline and provider signature all live
 * inside the call data bytes, which are copied verbatim.
 */
internal sealed interface TronSwapPayload {

    /** Transaction-level memo to carry over, `null` when the provider set none. */
    val memo: ByteArray?

    /** Native-value call of the router at `txTo` — the DEX shape (li-fi). */
    data class ContractCall(val callDataHex: String, override val memo: ByteArray?) : TronSwapPayload

    /** Plain TRX transfer to the provider's deposit address at `txTo` (SwapKit). */
    data class Transfer(override val memo: ByteArray?) : TronSwapPayload
}

/**
 * Resolves what to build from [ExpressTransactionModel.DEX.txData].
 *
 * `txData` is a whole serialized `Transaction.raw` protobuf — carrying its own ref block,
 * expiration, timestamp and fee limit — whose single contract is either a `TriggerSmartContract`
 * (swap through a router) or a `TransferContract` (deposit transfer). It may or may not be
 * `0x`-prefixed; providers disagree, so the prefix is not a reliable discriminator.
 *
 * @return `null` when the payload must not be signed: an unknown contract type, an empty contract
 * call, or a destination/amount that disagrees with the enclosing [ExpressTransactionModel.DEX].
 * A payload that is not a serialized transaction at all is treated as plain EVM-style call data and
 * passed through, so a backend switch back to that shape needs no change here.
 */
internal fun ExpressTransactionModel.DEX.tronSwapPayload(): TronSwapPayload? {
    val rawData = decodeTronRawTransaction(txData)
        ?: return TronSwapPayload.ContractCall(callDataHex = txData, memo = null)
    val contract = rawData.contract.singleOrNull() ?: return null
    val memo = rawData.data_.toByteArray().takeIf { it.isNotEmpty() }

    return when (contract.type) {
        Transaction.Contract.ContractType.TriggerSmartContract -> resolveContractCall(contract, memo)
        Transaction.Contract.ContractType.TransferContract -> resolveTransfer(contract, memo)
        else -> null
    }
}

private fun ExpressTransactionModel.DEX.resolveContractCall(
    contract: Transaction.Contract,
    memo: ByteArray?,
): TronSwapPayload? {
    // `unpack` validates the Any type url against the adapter, so a mismatched parameter throws.
    val trigger = runCatching { contract.parameter?.unpack(TriggerSmartContract.ADAPTER) }.getOrNull() ?: return null
    val callData = trigger.data_.toByteArray()

    if (callData.isEmpty()) return null
    if (!matchesTxValue(trigger.call_value)) return null
    if (!matchesTxTo(trigger.contract_address.toByteArray())) return null

    return TronSwapPayload.ContractCall(callDataHex = callData.toHexString(), memo = memo)
}

private fun ExpressTransactionModel.DEX.resolveTransfer(
    contract: Transaction.Contract,
    memo: ByteArray?,
): TronSwapPayload? {
    val transfer = runCatching { contract.parameter?.unpack(TransferContract.ADAPTER) }.getOrNull() ?: return null

    if (!matchesTxValue(transfer.amount)) return null
    if (!matchesTxTo(transfer.to_address.toByteArray())) return null

    return TronSwapPayload.Transfer(memo = memo)
}

/** The rebuilt transaction sends [ExpressTransactionModel.DEX.txValue], so the payload must agree. */
private fun ExpressTransactionModel.DEX.matchesTxValue(payloadValue: Long): Boolean {
    val expected = txValue?.toLongOrNull() ?: return true
    return expected == payloadValue
}

/** The rebuilt transaction is addressed to [ExpressTransactionModel.DEX.txTo], likewise. */
private fun ExpressTransactionModel.DEX.matchesTxTo(payloadAddress: ByteArray): Boolean {
    val expected = txTo.decodeBase58(checked = true) ?: return false
    return expected.contentEquals(payloadAddress)
}

/** Returns `null` when [txData] is not a serialized TRON transaction (e.g. plain call data). */
private fun decodeTronRawTransaction(txData: String): Transaction.raw? = runCatching {
    Transaction.raw.ADAPTER.decode(txData.hexToBytes()).takeIf { it.contract.isNotEmpty() }
}.getOrNull()

/**
 * Extras for the locally rebuilt transaction. `null` means nothing has to be attached — a plain
 * transfer without a memo, which the SDK builds as a `TransferContract`.
 */
internal fun TronSwapPayload.toTransactionExtras(): TronTransactionExtras? = when (this) {
    is TronSwapPayload.ContractCall -> TronTransactionExtras(
        callData = CompiledSmartContractCallData(callDataHex.hexToBytes()),
        memo = memo,
    )
    is TronSwapPayload.Transfer -> memo?.let { TronTransactionExtras(memo = it) }
}