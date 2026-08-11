package com.tangem.domain.wallets.usecase

import com.tangem.core.analytics.models.AnalyticsParam
import com.tangem.domain.cloudbackup.repository.CloudBackupRepository
import com.tangem.domain.models.wallet.UserWallet
import javax.inject.Inject

/**
 * Backups that a wallet already has, reported as the `Completed Backups` analytics parameter.
 *
 * Both types can be present at once. Cold wallets are backed up by a second card, which is neither of
 * the mobile-wallet backup types, so they report nothing.
 */
class GetCompletedBackupsUseCase @Inject constructor(
    private val cloudBackupRepository: CloudBackupRepository,
) {

    suspend operator fun invoke(wallet: UserWallet): Set<AnalyticsParam.BackupType> = when {
        !cloudBackupRepository.isCloudBackupEnabled -> emptySet()
        else -> completedBackups(wallet)
    }

    private suspend fun completedBackups(wallet: UserWallet): Set<AnalyticsParam.BackupType> = when (wallet) {
        is UserWallet.Cold -> emptySet()
        is UserWallet.Hot -> buildSet {
            if (wallet.backedUp) add(AnalyticsParam.BackupType.Manual)
            if (cloudBackupRepository.isBackedUp(wallet.walletId.stringValue)) {
                add(AnalyticsParam.BackupType.Cloud)
            }
        }
    }
}