package com.tangem.data.collectibles.vault

import com.tangem.domain.card.common.TapWorkarounds.isTangemTwins
import com.tangem.domain.models.wallet.UserWallet

/** A card that is not backed up must answer with that very card, so the session is pinned to its id. */
internal fun UserWallet.Cold.sessionCardId(): String? {
    val card = scanResponse.card
    val isBackedUp = card.backupStatus?.isActive == true

    return card.cardId.takeIf { !isBackedUp && !card.isTangemTwins }
}