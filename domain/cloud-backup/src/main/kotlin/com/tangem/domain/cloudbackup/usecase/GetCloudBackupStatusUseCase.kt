package com.tangem.domain.cloudbackup.usecase

import com.tangem.domain.cloudbackup.models.CloudBackupStatus
import com.tangem.domain.cloudbackup.repository.CloudBackupRepository
import javax.inject.Inject

class GetCloudBackupStatusUseCase @Inject constructor(
    private val cloudBackupRepository: CloudBackupRepository,
) {

    suspend operator fun invoke(walletId: String): CloudBackupStatus {
        if (!cloudBackupRepository.isBackedUp(walletId)) return CloudBackupStatus.Incomplete

        val found = cloudBackupRepository.withAccountOf(walletId) { cloudBackupRepository.findBackups() }

        return found.fold(
            ifLeft = { CloudBackupStatus.ActionRequired },
            ifRight = { backups ->
                if (backups.any { it.walletId == walletId }) {
                    CloudBackupStatus.Done
                } else {
                    CloudBackupStatus.ActionRequired
                }
            },
        )
    }
}