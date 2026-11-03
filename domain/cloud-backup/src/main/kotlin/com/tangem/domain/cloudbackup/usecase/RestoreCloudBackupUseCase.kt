package com.tangem.domain.cloudbackup.usecase

import arrow.core.Either
import com.tangem.domain.cloudbackup.models.CloudBackupError
import com.tangem.domain.cloudbackup.models.RestoredCloudBackup
import com.tangem.domain.cloudbackup.repository.CloudBackupRepository
import javax.inject.Inject

/**
 * Downloads a cloud backup and decrypts it with the user password, returning the wallet secret and name.
 * [accountName] is the account the backup was listed in, so the download does not depend on what stays pinned.
 */
class RestoreCloudBackupUseCase @Inject constructor(
    private val cloudBackupRepository: CloudBackupRepository,
) {

    suspend operator fun invoke(
        fileId: String,
        password: CharArray,
        accountName: String? = null,
    ): Either<CloudBackupError, RestoredCloudBackup> {
        val read = suspend { cloudBackupRepository.readBackup(fileId = fileId, password = password) }
        return if (accountName != null) cloudBackupRepository.withAccount(accountName, read) else read()
    }
}