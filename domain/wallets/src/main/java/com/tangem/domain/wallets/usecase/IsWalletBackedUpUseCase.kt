package com.tangem.domain.wallets.usecase

import com.tangem.domain.cloudbackup.repository.CloudBackupRepository
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.sociallogin.usecase.IsSocialBackupCompletedUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject

/**
 * Whether a wallet is backed up for the purpose of lifting the "finalize setup" restrictions (FR-11).
 *
 * A hot wallet counts as backed up once it has any of the three — a seed phrase, a cloud backup or
 * social recovery (UC-08 FR-15) — so restriction predicates must read this rather than
 * [UserWallet.Hot.backedUp], which tracks the seed phrase alone.
 */
class IsWalletBackedUpUseCase @Inject constructor(
    private val cloudBackupRepository: CloudBackupRepository,
    private val isSocialBackupCompletedUseCase: IsSocialBackupCompletedUseCase,
) {

    suspend operator fun invoke(wallet: UserWallet): Boolean = when (wallet) {
        is UserWallet.Cold -> wallet.isColdBackedUp()
        is UserWallet.Hot -> wallet.backedUp ||
            cloudBackupRepository.isBackedUp(wallet.walletId.stringValue) ||
            isSocialBackupCompletedUseCase(wallet.walletId)
    }

    fun flow(wallet: UserWallet): Flow<Boolean> = when (wallet) {
        is UserWallet.Cold -> flowOf(wallet.isColdBackedUp())
        is UserWallet.Hot -> if (wallet.backedUp) {
            flowOf(true)
        } else {
            combine(
                cloudBackupRepository.isBackedUpFlow(wallet.walletId.stringValue),
                isSocialBackupCompletedUseCase.flow(wallet.walletId),
            ) { hasCloudBackup, hasSocialBackup -> hasCloudBackup || hasSocialBackup }
                .distinctUntilChanged()
        }
    }

    private fun UserWallet.Cold.isColdBackedUp(): Boolean = scanResponse.card.backupStatus?.isActive == true
}