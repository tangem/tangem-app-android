package com.tangem.data.wallets.converters

import com.tangem.datasource.api.tangemTech.models.WalletCardDTO
import com.tangem.domain.wallets.models.backup.CardBackupStatus
import com.tangem.domain.wallets.models.backup.WalletCardBackup
import com.tangem.utils.converter.Converter

internal object WalletCardDTOConverter : Converter<WalletCardBackup, WalletCardDTO> {

    override fun convert(value: WalletCardBackup): WalletCardDTO {
        return WalletCardDTO(
            cardId = value.cardId,
            cardPublicKey = value.cardPublicKey,
            role = value.role.toDTO(),
            backupStatus = value.backupStatus.toDTO(),
            curves = value.curves.map { it.curve },
            errorCode = value.error?.code,
            errorMessage = value.error?.message,
        )
    }

    private fun WalletCardBackup.Role.toDTO(): WalletCardDTO.Role = when (this) {
        WalletCardBackup.Role.PRIMARY -> WalletCardDTO.Role.PRIMARY
        WalletCardBackup.Role.BACKUP_1 -> WalletCardDTO.Role.BACKUP_1
        WalletCardBackup.Role.BACKUP_2 -> WalletCardDTO.Role.BACKUP_2
    }

    private fun CardBackupStatus.toDTO(): WalletCardDTO.BackupStatus = when (this) {
        CardBackupStatus.NO_BACKUP -> WalletCardDTO.BackupStatus.NO_BACKUP
        CardBackupStatus.CARD_LINKED -> WalletCardDTO.BackupStatus.CARD_LINKED
        CardBackupStatus.ACTIVE -> WalletCardDTO.BackupStatus.ACTIVE
    }
}