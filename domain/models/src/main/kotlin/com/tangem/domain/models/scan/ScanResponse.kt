package com.tangem.domain.models.scan

import com.squareup.moshi.JsonClass
import com.tangem.common.card.WalletData
import com.tangem.common.extensions.ByteArrayKey
import com.tangem.domain.models.scan.serialization.ScanResponseAsStringSerializer
import com.tangem.operations.backup.PrimaryCard
import com.tangem.operations.derivation.ExtendedPublicKeysMap
import kotlinx.serialization.Serializable

// TODO: Move to :domain:card:models
/**
 * Created by Anton Zhilenkov on 07/04/2022.
 */
@Serializable(with = ScanResponseAsStringSerializer::class)
@JsonClass(generateAdapter = true)
data class ScanResponse(
    val card: CardDTO,
    val productType: ProductType,
    val walletData: WalletData?,
    val secondTwinPublicKey: String? = null,
    val derivedKeys: Map<KeyWalletPublicKey, ExtendedPublicKeysMap> = mapOf(),
    val primaryCard: PrimaryCard? = null,
)

typealias KeyWalletPublicKey = ByteArrayKey

enum class ProductType {
    Note, Twins, Wallet, Start2Coin, Wallet2, Ring, Visa,
}
