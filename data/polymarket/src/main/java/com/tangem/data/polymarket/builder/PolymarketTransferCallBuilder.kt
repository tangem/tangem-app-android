package com.tangem.data.polymarket.builder

import com.tangem.domain.polymarket.approval.PolymarketContracts
import com.tangem.domain.polymarket.model.PolymarketApprovalCall
import com.tangem.domain.polymarket.model.PredictionTransferCall
import java.math.BigInteger

object PolymarketTransferCallBuilder {

    private const val TRANSFER_SELECTOR = "a9059cbb"
    private const val ZERO_VALUE = "0"
    private const val HEX_RADIX = 16
    private const val SELECTOR_HEX_LENGTH = 8
    private const val WORD_HEX_LENGTH = 64
    private const val ADDRESS_HEX_LENGTH = 40
    private const val ADDRESS_PAD_HEX_LENGTH = WORD_HEX_LENGTH - ADDRESS_HEX_LENGTH
    private const val DATA_HEX_LENGTH = SELECTOR_HEX_LENGTH + WORD_HEX_LENGTH + WORD_HEX_LENGTH

    fun build(recipient: String, amountBaseUnits: BigInteger): PolymarketApprovalCall = PolymarketApprovalCall(
        target = PolymarketContracts.COLLATERAL,
        value = ZERO_VALUE,
        data = PolymarketContracts.HEX_PREFIX +
            TRANSFER_SELECTOR +
            recipient.removePrefix(PolymarketContracts.HEX_PREFIX).lowercase().padStart(WORD_HEX_LENGTH, '0') +
            amountBaseUnits.toString(HEX_RADIX).padStart(WORD_HEX_LENGTH, '0'),
    )

    /** @return null when [call] is not the 68-byte pUSD `transfer` this builder produces. */
    fun decode(call: PolymarketApprovalCall): PredictionTransferCall? {
        if (!call.target.equals(PolymarketContracts.COLLATERAL, ignoreCase = true)) return null
        if (call.value != ZERO_VALUE) return null

        val body = call.data.removePrefix(PolymarketContracts.HEX_PREFIX)
        if (body.length != DATA_HEX_LENGTH) return null
        if (!body.startsWith(TRANSFER_SELECTOR, ignoreCase = true)) return null

        val recipientWord = body.substring(SELECTOR_HEX_LENGTH, SELECTOR_HEX_LENGTH + WORD_HEX_LENGTH)
        if (recipientWord.take(ADDRESS_PAD_HEX_LENGTH).any { it != '0' }) return null

        val amountWord = body.substring(SELECTOR_HEX_LENGTH + WORD_HEX_LENGTH)
        val amount = runCatching { BigInteger(amountWord, HEX_RADIX) }.getOrNull() ?: return null

        return PredictionTransferCall(
            recipient = PolymarketContracts.HEX_PREFIX + recipientWord.takeLast(ADDRESS_HEX_LENGTH),
            amountBaseUnits = amount,
        )
    }
}