package com.tangem.domain.card.models

import com.tangem.common.extensions.hexToBytes
import com.tangem.domain.models.scan.ProductType
import com.tangem.domain.models.scan.ScanResponse

data class TwinKey(
    val key1: ByteArray,
    val key2: ByteArray,
) {
    fun getPairKey(walletPublicKey: ByteArray): ByteArray? {
        return when {
            walletPublicKey.contentEquals(key1) -> key2
            walletPublicKey.contentEquals(key2) -> key1
            else -> null
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as TwinKey

        if (!key1.contentEquals(other.key1)) return false
        if (!key2.contentEquals(other.key2)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = key1.contentHashCode()
        result = 31 * result + key2.contentHashCode()
        return result
    }

    companion object {

        fun getOrNull(scanResponse: ScanResponse): TwinKey? {
            return if (scanResponse.productType == ProductType.Twins) {
                val key1 = scanResponse.card.wallets.firstOrNull()?.publicKey ?: return null
                val key2 = scanResponse.secondTwinPublicKey?.hexToBytes() ?: return null
                TwinKey(key1 = key1, key2 = key2)
            } else {
                null
            }
        }
    }
}