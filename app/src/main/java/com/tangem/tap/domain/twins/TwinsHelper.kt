package com.tangem.tap.domain.twins

import com.tangem.commands.common.card.Card
import com.tangem.commands.common.card.CardStatus
import com.tangem.commands.common.card.masks.Product
import com.tangem.commands.wallet.WalletStatus
import com.tangem.tap.common.extensions.isEven

class TwinsHelper {
    companion object {
        const val TWIN_FILE_NAME = "TwinPublicKey"

        fun getTwinCardNumber(cardId: String): TwinCardNumber? {
            return when {
                firstCardSeries.map { cardId.startsWith(it) }.contains(true) -> {
                    TwinCardNumber.First
                }
                secondCardSeries.map { cardId.startsWith(it) }.contains(true) -> {
                    TwinCardNumber.Second
                }
                else -> {
                    null
                }
            }
        }

        fun getPairCardSeries(cardId: String): String? {
            return when (getTwinCardNumber(cardId) ?: return null) {
                TwinCardNumber.First -> {
                    val index = firstCardSeries.indexOf(cardId.take(4))
                    secondCardSeries[index]
                }
                TwinCardNumber.Second -> {
                    val index = secondCardSeries.indexOf(cardId.take(4))
                    firstCardSeries[index]
                }
            }
        }

        fun getTwinsCardId(cardId: String): String? {
            val cardIdWithNewSeries = when (getTwinCardNumber(cardId) ?: return null) {
                TwinCardNumber.First -> {
                    val index = if (cardId.startsWith(firstCardSeries[0])) 0 else 1
                    cardId.replace(firstCardSeries[index], secondCardSeries[index])
                }
                TwinCardNumber.Second -> {
                    val index = if (cardId.startsWith(secondCardSeries[0])) 0 else 1
                    cardId.replace(secondCardSeries[index], firstCardSeries[index])
                }
            }
            val cardIdWithoutChecksum = cardIdWithNewSeries.dropLast(1)
            val checkSum = cardIdWithoutChecksum.calculateLuhn()
            return cardIdWithoutChecksum + checkSum
        }

        fun getTwinCardIdForUser(cardId: String): String {
            if (cardId.length < 16) return cardId

            val twinCardId = cardId.substring(11..14)
            val twinCardNumber = getTwinCardNumber(cardId)?.number ?: 1
            return "$twinCardId #$twinCardNumber"
        }

        private val firstCardSeries = listOf("CB61", "CB64")
        private val secondCardSeries = listOf("CB62", "CB65")
    }
}

private fun String.calculateLuhn(): Int {
    val checksum = this.reversed()
        .mapIndexed { index, c ->
            val digit = if (c in '0'..'9') c - '0' else c - 'A'
            if (!index.isEven()) {
                digit
            } else {
                val newDigit = digit * 2
                if (newDigit >= 10) newDigit - 9 else newDigit
            }
        }.sum()
        .rem(10)
    return (10 - checksum) % 10
}

enum class TwinCardNumber(val number: Int) {
    First(1), Second(2);

    fun pairNumber(): TwinCardNumber = when (this) {
        First -> Second
        Second -> First
    }
}

fun Card.isTwinCard(): Boolean {
    return this.cardData?.productMask?.contains(Product.TwinCard) == true
}

fun Card.getTwinCardIdForUser(): String {
    return TwinsHelper.getTwinCardIdForUser(this.cardId)
}

fun Card.changeStatusToLoaded(): Card {
    val wallets = wallets.map { it.copy(status = WalletStatus.Loaded) }
    return copy(status = CardStatus.Loaded, wallets = wallets)
}

fun Card.changeStatusToEmpty(): Card {
    val wallets = wallets.map { it.copy(status = WalletStatus.Empty) }
    return copy(status = CardStatus.Empty, wallets = wallets)
}