package com.tangem.data.source.card.model

import com.tangem.common.card.Card
import com.tangem.common.card.WalletData
import com.tangem.common.extensions.ByteArrayKey
import com.tangem.operations.backup.PrimaryCard
import com.tangem.operations.derivation.ExtendedPublicKeysMap

data class ScanResult(
    val card: Card,
    val productType: ProductType,
    val walletData: WalletData?,
    val secondTwinPublicKey: String? = null,
    val derivedKeys: Map<ByteArrayKey, ExtendedPublicKeysMap> = mapOf(),
    val primaryCard: PrimaryCard? = null,
)

enum class ProductType {
    Note, Twins, Wallet, SaltPay, Start2Coin
}
