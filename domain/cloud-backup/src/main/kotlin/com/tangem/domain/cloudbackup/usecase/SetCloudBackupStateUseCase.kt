package com.tangem.domain.cloudbackup.usecase

import com.tangem.domain.cloudbackup.repository.CloudBackupRepository
import javax.inject.Inject

class SetCloudBackupStateUseCase @Inject constructor(
    private val cloudBackupRepository: CloudBackupRepository,
) {

    suspend operator fun invoke(walletId: String, isBackedUp: Boolean) {
        cloudBackupRepository.setBackedUp(walletId, isBackedUp)
    }
}