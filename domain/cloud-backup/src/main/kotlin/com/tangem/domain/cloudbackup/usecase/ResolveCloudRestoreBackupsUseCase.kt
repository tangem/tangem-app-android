package com.tangem.domain.cloudbackup.usecase

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensure
import com.tangem.domain.cloudbackup.models.CloudBackupError
import com.tangem.domain.cloudbackup.repository.CloudBackupRepository
import com.tangem.domain.cloudbackup.restore.CloudRestoreResult
import com.tangem.domain.cloudbackup.restore.CloudRestoreResultHolder
import javax.inject.Inject

/**
 * The account is signed out first, so the picker asks which account to look in every time instead of silently
 * reusing the last one. An account without backups is [CloudBackupError.BackupNotFound]; a result is always
 * non-empty and already parked in [CloudRestoreResultHolder] for the restore screen that opens next.
 */
class ResolveCloudRestoreBackupsUseCase @Inject constructor(
    private val cloudBackupRepository: CloudBackupRepository,
    private val cloudRestoreResultHolder: CloudRestoreResultHolder,
) {

    suspend operator fun invoke(): Either<CloudBackupError, CloudRestoreResult> = either {
        cloudRestoreResultHolder.clear()
        cloudBackupRepository.signOut()
        val backups = cloudBackupRepository.findBackups(interactive = true, validateContent = true).bind()
        ensure(backups.isNotEmpty()) { CloudBackupError.BackupNotFound }

        val email = cloudBackupRepository.getAccountInfo(interactive = false).getOrNull()?.email
        CloudRestoreResult(backups = backups, accountEmail = email).also(cloudRestoreResultHolder::set)
    }
}