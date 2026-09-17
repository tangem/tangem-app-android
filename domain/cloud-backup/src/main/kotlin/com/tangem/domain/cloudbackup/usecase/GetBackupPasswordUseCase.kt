package com.tangem.domain.cloudbackup.usecase

import com.tangem.domain.cloudbackup.repository.BackupPasswordRepository
import javax.inject.Inject

class GetBackupPasswordUseCase @Inject constructor(
    private val backupPasswordRepository: BackupPasswordRepository,
) {

    suspend operator fun invoke(): CharArray? = backupPasswordRepository.getPassword()
}