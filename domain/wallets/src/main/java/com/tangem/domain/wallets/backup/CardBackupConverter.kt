package com.tangem.domain.wallets.backup

import com.tangem.common.extensions.toHexString
import com.tangem.domain.models.scan.CardDTO
import com.tangem.domain.wallets.models.backup.CardBackupError
import com.tangem.domain.wallets.models.backup.CardBackupStatus
import com.tangem.domain.wallets.models.backup.WalletCardBackup

/**
 * Builds the [WalletCardBackup] reported to the backend from a card the app has just scanned.
 *
 * The single entry point for every trigger that reports cards, so the mapping of the card's backup state and
 * curves is derived in one place.
 */
object CardBackupConverter {

    fun convert(card: CardDTO, role: WalletCardBackup.Role, error: CardBackupError? = null): WalletCardBackup {
        return WalletCardBackup(
            cardId = card.cardId,
            cardPublicKey = card.cardPublicKey.toHexString(),
            role = role,
            backupStatus = convertStatus(card.backupStatus),
            curves = card.wallets.map { it.curve },
            error = error,
        )
    }

    /**
     * Builds a card the app knows only by identity — a backup card that has been added to the backup but not
     * finalized yet, so it holds no wallets and its curves are empty.
     */
    fun convert(
        cardId: String,
        cardPublicKey: ByteArray,
        role: WalletCardBackup.Role,
        backupStatus: CardBackupStatus,
        error: CardBackupError? = null,
    ): WalletCardBackup {
        return WalletCardBackup(
            cardId = cardId,
            cardPublicKey = cardPublicKey.toHexString(),
            role = role,
            backupStatus = backupStatus,
            curves = emptyList(),
            error = error,
        )
    }

    /**
     * A `null` status means the card's firmware has no notion of backup. Such wallets never reach the backend —
     * [com.tangem.domain.wallets.usecase.GetWalletBackupIntegrityUseCase] filters them out — so the value is
     * only a safe default here.
     */
    private fun convertStatus(status: CardDTO.BackupStatus?): CardBackupStatus = when (status) {
        is CardDTO.BackupStatus.Active -> CardBackupStatus.ACTIVE
        is CardDTO.BackupStatus.CardLinked -> CardBackupStatus.CARD_LINKED
        CardDTO.BackupStatus.NoBackup, null -> CardBackupStatus.NO_BACKUP
    }
}