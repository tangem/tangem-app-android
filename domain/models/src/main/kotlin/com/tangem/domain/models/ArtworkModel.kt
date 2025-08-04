package com.tangem.domain.models

data class ArtworkModel(
    val verifiedArtwork: ByteArray? = null,
    val defaultUrl: String,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ArtworkModel

        if (!verifiedArtwork.contentEquals(other.verifiedArtwork)) return false
        if (defaultUrl != other.defaultUrl) return false

        return true
    }

    override fun hashCode(): Int {
        var result = verifiedArtwork?.contentHashCode() ?: 0
        result = 31 * result + defaultUrl.hashCode()
        return result
    }
}