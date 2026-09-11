package com.tangem.domain.cloudbackup.usecase

import com.tangem.domain.cloudbackup.repository.BackupPasswordRepository
import javax.inject.Inject

class SaveBackupPasswordUseCase @Inject constructor(
    private val backupPasswordRepository: BackupPasswordRepository,
) {

    suspend operator fun invoke(username: String, password: CharArray): Boolean {
        return backupPasswordRepository.savePassword(username = username, password = password)
    }
}