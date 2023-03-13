package com.tangem.domain.card.model

data class TwinData(
    val series: TwinCardSeries,
    val pairPublicKey: ByteArray?,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is TwinData) return false
        if (!super.equals(other)) return false

        if (series != other.series) return false
        if (pairPublicKey != null) {
            if (other.pairPublicKey == null) return false
            if (!pairPublicKey.contentEquals(other.pairPublicKey)) return false
        } else if (other.pairPublicKey != null) return false

        return true
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + series.hashCode()
        result = 31 * result + (pairPublicKey?.contentHashCode() ?: 0)
        return result
    }
}

enum class TwinCardSeries {
    CB61,
    CB62,
    CB64,
    CB65;

    val number: Int
        get() = when (this) {
            CB61, CB64 -> 1
            CB62, CB65 -> 2
        }

    val pair: TwinCardSeries
        get() = when (this) {
            CB61 -> CB62
            CB62 -> CB61
            CB64 -> CB65
            CB65 -> CB64
        }

    companion object {
        fun series(cardId: String) = TwinCardSeries.values()
            .firstOrNull { cardId.startsWith(prefix = it.name) }
    }
}
