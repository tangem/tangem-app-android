package com.tangem.domain.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.tangem.common.card.EllipticCurve
import com.tangem.crypto.hdWallet.DerivationPath
import com.tangem.crypto.hdWallet.bip32.ExtendedPublicKey
import com.tangem.domain.models.serialization.MobileWalletAsStringSerializer
import kotlinx.serialization.Serializable

@Serializable(with = MobileWalletAsStringSerializer::class)
@JsonClass(generateAdapter = true)
data class MobileWallet(
    @Json(name = "publicKey")
    val publicKey: ByteArray,
    @Json(name = "chainCode")
    val chainCode: ByteArray?,
    @Json(name = "curve")
    val curve: EllipticCurve,
    @Json(name = "derivedKeys")
    val derivedKeys: Map<DerivationPath, ExtendedPublicKey>,
) {

    val extendedPublicKey: ExtendedPublicKey?
        get() = chainCode?.let {
            ExtendedPublicKey(
                publicKey = publicKey,
                chainCode = it,
            )
        }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is MobileWallet) return false

        if (!publicKey.contentEquals(other.publicKey)) return false
        if (chainCode != null) {
            if (other.chainCode == null || !chainCode.contentEquals(other.chainCode)) return false
        } else if (other.chainCode != null) return false

        if (curve != other.curve) return false
        if (derivedKeys != other.derivedKeys) return false

        return true
    }

    override fun hashCode(): Int {
        var result = publicKey.contentHashCode()
        result = 31 * result + (chainCode?.contentHashCode() ?: 0)
        result = 31 * result + curve.hashCode()
        result = 31 * result + derivedKeys.hashCode()
        return result
    }
}