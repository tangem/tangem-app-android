package com.tangem.features.send.send.confirm.model

import com.tangem.core.ui.utils.parseToBigDecimal
import com.tangem.features.send.send.confirm.SendConfirmComponent
import com.tangem.lib.crypto.BlockchainUtils
import java.math.BigDecimal

/** What a send intent's transfer was actually sent as. */
internal data class SentTransfer(
    val amount: BigDecimal?,
    val recipient: String,
    val memo: String?,
    val decimals: Int,
    val networkRawId: String,
)

/**
 * Tells whether [sent] differs from what the intent asked for. The sent amount is compared, not the entered one, as
 * taking the fee out of the amount changes the transfer too. An EVM address differs only by its checksum casing; an
 * absent memo equals an empty one.
 */
internal fun SendConfirmComponent.Intent.isModifiedBy(sent: SentTransfer): Boolean {
    val intentAmount = amount?.parseToBigDecimal(sent.decimals)
    val isAmountModified = intentAmount == null || sent.amount == null || sent.amount.compareTo(intentAmount) != 0
    val isAddressCaseInsensitive = BlockchainUtils.isCaseInsensitiveContractAddress(sent.networkRawId)
    val isRecipientModified = !sent.recipient.equals(destinationAddress, ignoreCase = isAddressCaseInsensitive)

    return isAmountModified || isRecipientModified || sent.memo.orEmpty() != memo.orEmpty()
}