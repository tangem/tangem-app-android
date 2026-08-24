package com.tangem.domain.cloudbackup.usecase

import arrow.core.Either
import arrow.core.raise.either
import com.tangem.domain.cloudbackup.models.CloudBackupError
import com.tangem.domain.cloudbackup.repository.CloudBackupRepository
import javax.inject.Inject

/**
 * Deletes the cloud backup of the wallet with the given id.
 *
 * Callers know the wallet, not the backup file, so the file is located by listing the cloud backups and
 * matching [com.tangem.domain.cloudbackup.models.CloudBackupInfo.walletId]. A wallet without a backup
 * is a success. Deletion goes through [DeleteCloudBackupWithRetryUseCase], since this runs during
 * wallet teardown (forget / upgrade) where the cloud storage may be temporarily unreachable.
 */
class DeleteWalletCloudBackupUseCase @Inject constructor(
    private val cloudBackupRepository: CloudBackupRepository,
    private val deleteCloudBackupWithRetryUseCase: DeleteCloudBackupWithRetryUseCase,
) {

    suspend operator fun invoke(walletId: String): Either<CloudBackupError, Unit> = either {
        val backups = cloudBackupRepository.findBackups().bind()
        val info = backups.firstOrNull { it.walletId == walletId }

        if (info != null) {
            deleteCloudBackupWithRetryUseCase(info.fileId).bind()
        }
    }
}