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
}