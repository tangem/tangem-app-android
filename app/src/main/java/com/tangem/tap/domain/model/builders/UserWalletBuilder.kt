package com.tangem.tap.domain.model.builders

import com.tangem.domain.common.ScanResponse
import com.tangem.domain.common.util.userWalletId
import com.tangem.tap.domain.extensions.getOrLoadCardArtworkUrl
import com.tangem.tap.domain.model.UserWallet

class UserWalletBuilder(
    private val scanResponse: ScanResponse,
) {
    private var backupCardsIds: Set<String> = emptySet()

    fun backupCardsIds(backupCardsIds: Set<String>?) = this.apply {
        if (backupCardsIds != null) {
            this.backupCardsIds = backupCardsIds
        }
    }

    suspend fun build(): UserWallet {
        return with(scanResponse) {
            UserWallet(
                walletId = card.userWalletId,
                name = productType.name,
                artworkUrl = card.getOrLoadCardArtworkUrl(),
                cardsInWallet = backupCardsIds.plus(card.cardId),
                scanResponse = this,
            )
        }
    }
}
