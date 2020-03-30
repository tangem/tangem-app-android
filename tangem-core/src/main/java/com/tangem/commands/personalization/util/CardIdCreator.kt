package com.tangem.commands.personalization.util

/**
[REDACTED_AUTHOR]
 */
internal class CardIdCreator {

    companion object {
        private val Alf = "ABCDEF0123456789"

        fun create(nsSeries: String?, number: Long): String? {
            val series = nsSeries ?: return null
            if (number <= 0 || (series.length != 2 && series.length != 4)) return null
            if (!checkSeries(series)) return null

            val tail = if (series.length == 2) String.format("%013d", number) else String.format("%011d", number)
            val cardId = series + tail
            return completeCardID(cardId)
        }

        private fun checkSeries(series: String): Boolean {
            val containsList = series.filter { Alf.contains(it) }
            return containsList.length == series.length
        }

        private fun completeCardID(imCardId: String): String? {
            var cardId = imCardId
            cardId = cardId.replace(" ", "")
            if (cardId.length != 15 || Alf.indexOf(cardId[0]) == -1 || Alf.indexOf(cardId[1]) == -1)
                return null

            cardId += "0"
            val length = cardId.length
            var sum = 0
            for (i in 0 until length) {
                // get digits in reverse order
                var digit: Int
                val cDigit = cardId[length - i - 1]
                digit = if (cDigit in '0'..'9') cDigit - '0' else cDigit - 'A'

                // every 2nd number multiply with 2
                if (i % 2 == 1) digit *= 2
                sum += if (digit > 9) digit - 9 else digit
            }
            val lunh = (10 - sum % 10) % 10
            return cardId.substring(0, 15) + String.format("%d", lunh)
        }
    }
}