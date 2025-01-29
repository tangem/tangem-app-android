package com.tangem.domain.models.scan

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.tangem.common.card.WalletData
import com.tangem.common.extensions.ByteArrayKey
import com.tangem.domain.models.scan.serialization.ScanResponseAsStringSerializer
import com.tangem.domain.visa.model.VisaCardActivationStatus
import com.tangem.operations.backup.PrimaryCard
import com.tangem.operations.derivation.ExtendedPublicKeysMap
import kotlinx.serialization.Serializable

// TODO: Move to :domain:card:models
/**
[REDACTED_AUTHOR]
 */
@Serializable(with = ScanResponseAsStringSerializer::class)
@JsonClass(generateAdapter = true)
data class ScanResponse(
    @Json(name = "card") val card: CardDTO,
    @Json(name = "productType") val productType: ProductType,
    @Json(name = "walletData") val walletData: WalletData?,
    @Json(name = "secondTwinPublicKey") val secondTwinPublicKey: String? = null,
    @Json(name = "visaCardActivationStatus") val visaCardActivationStatus: VisaCardActivationStatus? = null,
    @Json(name = "derivedKeys") val derivedKeys: Map<KeyWalletPublicKey, ExtendedPublicKeysMap> = mapOf(),
    @Json(name = "primaryCard") val primaryCard: PrimaryCard? = null,
)

typealias KeyWalletPublicKey = ByteArrayKey

@JsonClass(generateAdapter = false)
enum class ProductType {
    @Json(name = "Note")
    Note,

    @Json(name = "Twins")
    Twins,

    @Json(name = "Wallet")
    Wallet,

    @Json(name = "Start2Coin")
    Start2Coin,

    @Json(name = "Wallet2")
    Wallet2,

    @Json(name = "Ring")
    Ring,

    @Json(name = "Visa")
    Visa,
}