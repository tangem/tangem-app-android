package com.tangem.domain.common.card

import timber.log.Timber

/**
* [REDACTED_AUTHOR]
 */
class CardIdRange(
    val cardIdStart: String,
    val cardIdEnd: String,
) {
    val range: LongRange

    init {
        checkCardId(cardIdStart)
        checkCardId(cardIdEnd)
        range = toLong(cardIdStart)..toLong(cardIdEnd)
    }

    fun contains(cardId: String): Boolean = try {
        checkCardId(cardId)
        range.contains(toLong(cardId))
    } catch (ex: Exception) {
        Timber.e(ex, "CardIdRange: check for the cardId: [$cardId] in range failed.")
        false
    }

    private fun stripBatchPrefix(cardId: String): String = cardId.drop(4)

    @Throws(NumberFormatException::class)
    private fun toLong(cardId: String): Long = stripBatchPrefix(cardId).toLong()

    private fun checkCardId(cardId: String) {
        if (cardId.length != 16) throw IllegalArgumentException("CardId must be 16 characters long.")
    }
}
