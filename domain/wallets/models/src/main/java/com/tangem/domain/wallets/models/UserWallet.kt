package com.tangem.domain.wallets.models

import com.tangem.domain.models.scan.CardDTO
import com.tangem.domain.models.scan.ScanResponse

/**
 * Represents user's wallet which stored in app persistence
 *
 * @property name            User wallet name
 * @property walletId        User wallet [UserWalletId]
 * @property artworkUrl      User wallet card artwork URL
 * @property cardsInWallet   List of cards IDs assigned with this user's wallet. The list will be empty if the wallet
 *                              has been backed up on another device.
 * @property isMultiCurrency Indicates whether this user wallet can work with more than one currency
 * @property scanResponse    [ScanResponse] of primary user's wallet card.
 */
data class UserWallet(
    val name: String,
    val walletId: UserWalletId,
    val artworkUrl: String,
    val cardsInWallet: Set<String>,
    val isMultiCurrency: Boolean,
    val hasBackupError: Boolean,
    val scanResponse: ScanResponse, // TODO: Replace with [com.tangem.domain.models.scan.CardDTO]
) {

    /** ID of user's wallet primary card */
    val cardId: String get() = scanResponse.card.cardId

    /** Indicates if the user's wallet primary card has access code */
    val hasAccessCode: Boolean get() = scanResponse.card.isAccessCodeSet

    /** Indicates if this primary card has no currency wallets */
    val isLocked: Boolean get() = scanResponse.card.wallets.isEmpty()

    /** Indicated if this primary card is imported */
    val isImported: Boolean get() = scanResponse.card.wallets.any(CardDTO.Wallet::isImported)
}
