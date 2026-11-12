package com.tangem.data.collectibles.vault

import com.tangem.blockchain.common.Blockchain
import com.tangem.domain.card.common.TapWorkarounds.isTangemTwins
import com.tangem.domain.card.common.util.cardTypesResolver
import com.tangem.domain.models.wallet.UserWallet

/**
 * The chain the gacha vault is addressed on. A hot wallet has no card behind it, so it is never a test one.
 */
internal val UserWallet.collectiblesVaultBlockchain: Blockchain
    get() = if (this is UserWallet.Cold && cardTypesResolver.isTestCard()) {
        Blockchain.SolanaTestnet
    } else {
        Blockchain.Solana
    }

/** A card that is not backed up must answer with that very card, so the session is pinned to its id. */
internal fun UserWallet.Cold.sessionCardId(): String? {
    val card = scanResponse.card
    val isBackedUp = card.backupStatus?.isActive == true

    return card.cardId.takeIf { !isBackedUp && !card.isTangemTwins }
}