package com.tangem.data.wallets.converters

import com.tangem.datasource.api.tangemTech.models.WalletCardDTO
import com.tangem.domain.wallets.models.backup.CardBackupStatus
import com.tangem.domain.wallets.models.backup.WalletCardBackup
import com.tangem.utils.converter.Converter

internal object WalletCardDTOConverter : Converter<WalletCardBackup, WalletCardDTO> {

    private const val MAX_ERROR_CODE_LENGTH = 256
    private const val MAX_ERROR_MESSAGE_LENGTH = 1024

    override fun convert(value: WalletCardBackup): WalletCardDTO {
        return WalletCardDTO(
            cardId = value.cardId,
            cardPublicKey = value.cardPublicKey,
            role = value.role.toDTO(),
            backupStatus = value.backupStatus.toDTO(),
            curves = value.curves.map { it.curve },
            errorCode = value.error?.code?.take(MAX_ERROR_CODE_LENGTH),
            errorMessage = value.error?.message?.take(MAX_ERROR_MESSAGE_LENGTH),
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