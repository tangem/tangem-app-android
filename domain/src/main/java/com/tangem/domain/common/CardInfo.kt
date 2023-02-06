package com.tangem.domain.common

import com.tangem.common.card.WalletData
import com.tangem.operations.backup.PrimaryCard
import com.tangem.operations.derivation.ExtendedPublicKeysMap

data class CardInfo(
    val card: CardDTO,
    val productType: ProductType,
    val walletData: WalletData?,
    val secondTwinPublicKey: String?,
    val derivedKeys: Map<KeyWalletPublicKey, ExtendedPublicKeysMap>,
    val primaryCard: PrimaryCard?,
)
