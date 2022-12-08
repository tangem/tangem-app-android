package com.tangem.tap.domain.model

import com.tangem.domain.common.ScanResponse
import com.tangem.domain.common.util.UserWalletId

/**
 * Represents user's wallet which stored in app persistence
 * @param name User's wallet name
 * @param walletId User's wallet [UserWalletId]
 * @param artworkUrl User wallet card artwork URL
 * @param cardsInWallet List of cards IDs assigned with this user's wallet
 * @param scanResponse [ScanResponse] of primary user's wallet card.
 * TODO: Replace with [com.tangem.domain.common.CardDTO]
 * @property cardId ID of user's wallet primary card
 * */
data class UserWallet(
    val name: String,
    val walletId: UserWalletId,
    val artworkUrl: String,
    val cardsInWallet: Set<String>,
    val scanResponse: ScanResponse,
) {
    val cardId: String
        get() = scanResponse.card.cardId

    internal var isSaved: Boolean = true
}