package com.tangem.domain.wallets.models.backup

import com.tangem.common.card.EllipticCurve

/**
 * A card associated with a wallet together with the state of its backup.
 *
 * Reported to the backend so an interrupted backup stays detectable after the app is reinstalled or the wallet
 * is opened on another device, where no local state is available.
 *
 * @property cardId        card identifier
 * @property cardPublicKey public key of the card, hex encoded — the same encoding the `card_public_key` request
 *                         header uses
 * @property role          role of the card within the wallet
 * @property backupStatus  backup state of the card

 * @property error         error received while performing a card command, if any
 */
data class WalletCardBackup(
    val cardId: String,
    val cardPublicKey: String,
    val role: Role,
    val backupStatus: CardBackupStatus,
    val curves: List<EllipticCurve>,
    val error: CardBackupError? = null,
) {

    /** Role of a card within the wallet */
    enum class Role {
        PRIMARY,
        BACKUP_1,
        BACKUP_2,
    }
}