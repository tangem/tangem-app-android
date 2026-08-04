package com.tangem.data.wallets.converters

import com.tangem.common.card.EllipticCurve
import com.tangem.datasource.api.tangemTech.models.WalletCardDTO
import com.tangem.domain.wallets.models.backup.CardBackupError
import com.tangem.domain.wallets.models.backup.CardBackupStatus
import com.tangem.domain.wallets.models.backup.WalletCardBackup
import com.tangem.utils.converter.Converter
import com.tangem.utils.logging.TangemLogger

internal object WalletCardBackupConverter : Converter<WalletCardDTO, WalletCardBackup> {

    override fun convert(value: WalletCardDTO): WalletCardBackup {
        return WalletCardBackup(
            cardId = value.cardId,
            cardPublicKey = value.cardPublicKey,
            role = value.role.toDomain(),
            backupStatus = value.backupStatus.toDomain(),
            curves = value.curves.mapNotNull(::toCurve),
            error = value.toError(),
        )
    }

    private fun WalletCardDTO.Role.toDomain(): WalletCardBackup.Role = when (this) {
        WalletCardDTO.Role.PRIMARY -> WalletCardBackup.Role.PRIMARY
        WalletCardDTO.Role.BACKUP_1 -> WalletCardBackup.Role.BACKUP_1
        WalletCardDTO.Role.BACKUP_2 -> WalletCardBackup.Role.BACKUP_2
    }

    private fun WalletCardDTO.BackupStatus.toDomain(): CardBackupStatus = when (this) {
        WalletCardDTO.BackupStatus.NO_BACKUP -> CardBackupStatus.NO_BACKUP
        WalletCardDTO.BackupStatus.CARD_LINKED -> CardBackupStatus.CARD_LINKED
        WalletCardDTO.BackupStatus.ACTIVE -> CardBackupStatus.ACTIVE
    }

    /** Drops curves this app version does not know: a newer backend may report ones added after this release */
    private fun toCurve(curve: String): EllipticCurve? {
        return EllipticCurve.entries.firstOrNull { it.curve == curve }
            .also { expectedCurve ->
                if (expectedCurve == null) {
                    TangemLogger.w("WalletCardBackupConverter: unknown elliptic curve $curve")
                }
            }
    }

    private fun WalletCardDTO.toError(): CardBackupError? {
        return if (errorCode == null && errorMessage == null) {
            null
        } else {
            CardBackupError(code = errorCode, message = errorMessage)
        }
    }
}