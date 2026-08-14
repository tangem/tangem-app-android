package com.tangem.domain.cloudbackup.usecase

import arrow.core.Either
import com.tangem.domain.cloudbackup.models.CloudBackupError
import com.tangem.domain.cloudbackup.models.CloudBackupSecretData
import com.tangem.domain.cloudbackup.repository.CloudBackupRepository
import javax.inject.Inject

/** Downloads a cloud backup and decrypts it with the user password, returning the wallet secret. */
class RestoreCloudBackupUseCase @Inject constructor(
    private val cloudBackupRepository: CloudBackupRepository,
) {

    suspend operator fun invoke(fileId: String, password: CharArray): Either<CloudBackupError, CloudBackupSecretData> {
        return cloudBackupRepository.readBackup(fileId = fileId, password = password)
    }
}