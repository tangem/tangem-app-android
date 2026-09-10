package com.tangem.data.jointaccount.safe

import com.tangem.blockchain.blockchains.ethereum.tokenmethods.TransferERC20TokenCallData
import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.HEX_PREFIX
import com.tangem.blockchain.extensions.formatHex
import com.tangem.domain.jointaccount.model.JointTransferIntent
import com.tangem.domain.jointaccount.model.SafeInnerCall
import com.tangem.domain.jointaccount.model.SafeTx
import com.tangem.domain.jointaccount.safe.JointTransferPayloadCodec
import java.math.BigInteger
import javax.inject.Inject

/**
 * ERC-20 `transfer(address,uint256)` is the blockchain SDK's [TransferERC20TokenCallData] in both directions — it
 * owns the selector, the word layout and the checks that a recipient is a real 20-byte address rather than a
 * left-padded fragment.
 *
 * Amounts cross that boundary raw: the intent carries wei, so the SDK's [Amount] is built over
 * [Blockchain.Unknown], whose `decimals` is zero, and the value passes through unscaled. That is the same shape
 * the SDK's own decoder returns.
 */
internal class DefaultJointTransferPayloadCodec @Inject constructor() : JointTransferPayloadCodec {

    override fun encode(intent: JointTransferIntent): SafeInnerCall? = when (intent) {
        is JointTransferIntent.Native -> SafeInnerCall(
            to = intent.recipient.lowercase(),
            value = intent.amountWei,
            data = HEX_PREFIX,
        )
        is JointTransferIntent.Erc20 -> intent.encodeTransfer()
    }

    /**
     * `null` for the payloads the SDK refuses to encode: a recipient that is not a 20-byte address or is the zero
     * address ([IllegalArgumentException]), an amount that does not fit a uint256 ([IllegalStateException]).
     */
    private fun JointTransferIntent.Erc20.encodeTransfer(): SafeInnerCall? = try {
        SafeInnerCall(
            to = tokenContract.lowercase(),
            value = BigInteger.ZERO,
            data = TransferERC20TokenCallData(
                destination = recipient,
                amount = amountWei.asRawAmount(),
            ).dataHex.formatHex().lowercase(),
        )
    } catch (e: IllegalArgumentException) {
        null
    } catch (e: IllegalStateException) {
        null
    }

    override fun decode(safeTx: SafeTx): JointTransferIntent? = when {
        safeTx.data.isEmptyPayload() -> nativeTransfer(safeTx)
        safeTx.value == BigInteger.ZERO -> tokenTransfer(safeTx)
        else -> null
    }

    private fun nativeTransfer(safeTx: SafeTx): JointTransferIntent? {
        if (safeTx.value <= BigInteger.ZERO) return null

        return JointTransferIntent.Native(recipient = safeTx.to, amountWei = safeTx.value)
    }

    private fun tokenTransfer(safeTx: SafeTx): JointTransferIntent? {
        val transfer = TransferERC20TokenCallData(compiledData = safeTx.data) ?: return null
        val amountWei = transfer.amount.value?.toBigInteger() ?: return null

        return JointTransferIntent.Erc20(
            tokenContract = safeTx.to,
            recipient = transfer.destination,
            amountWei = amountWei,
        )
    }

    private fun BigInteger.asRawAmount(): Amount = Amount(value = toBigDecimal(), blockchain = Blockchain.Unknown)

    private fun String.isEmptyPayload(): Boolean =
        removePrefix(prefix = HEX_PREFIX).removePrefix(prefix = HEX_PREFIX.uppercase()).isEmpty()
}