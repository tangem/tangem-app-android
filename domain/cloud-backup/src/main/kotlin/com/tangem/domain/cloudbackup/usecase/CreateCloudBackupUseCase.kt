package com.tangem.domain.cloudbackup.usecase

import arrow.core.Either
import com.tangem.domain.cloudbackup.models.CloudBackupError
import com.tangem.domain.cloudbackup.models.CloudBackupInfo
import com.tangem.domain.cloudbackup.models.CloudBackupSecretData
import com.tangem.domain.cloudbackup.repository.CloudBackupRepository
import javax.inject.Inject

/** Encrypts a wallet secret with the user password and uploads it as a cloud backup. */
class CreateCloudBackupUseCase @Inject constructor(
    private val cloudBackupRepository: CloudBackupRepository,
) {

    suspend operator fun invoke(
        walletId: String,
        walletName: String,
        secret: CloudBackupSecretData,
        password: CharArray,
    ): Either<CloudBackupError, CloudBackupInfo> {
        return cloudBackupRepository.uploadBackup(
            walletId = walletId,
            walletName = walletName,
            createdAtMillis = System.currentTimeMillis(),
            secret = secret,
            password = password,
        )
    }
}