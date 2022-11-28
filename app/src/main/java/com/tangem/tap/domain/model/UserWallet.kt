package com.tangem.tap.domain.model

import com.tangem.domain.common.ScanResponse
import com.tangem.domain.common.util.UserWalletId
import com.tangem.domain.common.util.userWalletId
import com.tangem.tap.domain.extensions.getOrLoadCardArtworkUrl

data class UserWallet(
    val name: String,
    val walletId: UserWalletId,
    val artworkUrl: String,
    val cardsInWallet: Set<String>,
    val scanResponse: ScanResponse,
) {
    val cardId: String
        get() = scanResponse.card.cardId

    companion object {
        suspend operator fun invoke(
            scanResponse: ScanResponse,
            backupCardsIds: Set<String>? = null,
        ): UserWallet {
            return with(scanResponse) {
                UserWallet(
                    walletId = card.userWalletId,
                    name = productType.name,
                    artworkUrl = card.getOrLoadCardArtworkUrl(),
                    cardsInWallet = backupCardsIds?.plus(card.cardId) ?: setOf(card.cardId),
                    scanResponse = this,
                )
            }
        }
    }
}