package com.tangem.domain.models.scan

import com.tangem.common.card.WalletData
import com.tangem.common.extensions.ByteArrayKey
import com.tangem.operations.backup.PrimaryCard
import com.tangem.operations.derivation.ExtendedPublicKeysMap

// TODO: Move to :domain:card:models
/**
 * Created by Anton Zhilenkov on 07/04/2022.
 */
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
