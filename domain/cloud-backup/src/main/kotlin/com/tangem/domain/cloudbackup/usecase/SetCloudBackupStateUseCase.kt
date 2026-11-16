package com.tangem.domain.cloudbackup.usecase

import com.tangem.domain.cloudbackup.repository.CloudBackupRepository
import javax.inject.Inject

class SetCloudBackupStateUseCase @Inject constructor(
    private val cloudBackupRepository: CloudBackupRepository,
) {

    /** [accountName] is the account the backup is in, when known; otherwise the current one is remembered */
    suspend operator fun invoke(walletId: String, isBackedUp: Boolean, accountName: String? = null) {
        if (accountName != null) {
            cloudBackupRepository.withAccount(accountName) { cloudBackupRepository.setBackedUp(walletId, isBackedUp) }
        } else {
            cloudBackupRepository.setBackedUp(walletId, isBackedUp)
        }
    }
}