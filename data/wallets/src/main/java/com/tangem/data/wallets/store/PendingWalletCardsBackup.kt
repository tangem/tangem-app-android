package com.tangem.data.wallets.store

import com.tangem.datasource.api.tangemTech.models.WalletCardDTO
import kotlinx.serialization.Serializable

/**
 * A cards-backup report that has been made but not yet accepted by the backend.
 *
 * Mirrors the request rather than the domain models it was built from, so replaying it is a plain re-POST and
 * a report queued by an older build cannot be reinterpreted by a newer one.
 *
 * @property id       identifies the entry within the queue, so a wallet reported twice yields two entries
 * @property walletId wallet the report is about, the `wallet_id` path parameter
 * @property cards    cards known for the wallet at the moment the report was made
 * @property usedSeed `true` if a seed phrase was used to create or import the wallet
 */
@Serializable
@Suppress("BooleanPropertyNaming")
internal data class PendingWalletCardsBackup(
    val id: String,
    val walletId: String,
    val cards: List<Card>,
    val usedSeed: Boolean,
) {

    @Serializable
    internal data class Card(
        val cardId: String,
        val cardPublicKey: String,
        val role: WalletCardDTO.Role,
        val backupStatus: WalletCardDTO.BackupStatus,
        val curves: List<String>,
        val errorCode: String? = null,
        val errorMessage: String? = null,
    )
}