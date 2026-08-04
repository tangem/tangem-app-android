package com.tangem.domain.wallets.usecase

import com.tangem.domain.cloudbackup.repository.CloudBackupRepository
import com.tangem.domain.models.wallet.UserWallet
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject

/**
 * Whether a wallet is backed up for the purpose of lifting the "finalize setup" restrictions (FR-11).
 *
 * A hot wallet counts as backed up once it has ANY backup — a seed phrase ([UserWallet.Hot.backedUp],
 * which tracks only that) OR a cloud backup. Restriction predicates (finalize banner, access-code gate,
 * incomplete badge, analytics, ...) must read this rather than [UserWallet.Hot.backedUp] alone.
 */
class IsWalletBackedUpUseCase @Inject constructor(
    private val cloudBackupRepository: CloudBackupRepository,
) {

    suspend operator fun invoke(wallet: UserWallet): Boolean = when (wallet) {
        is UserWallet.Cold -> wallet.isColdBackedUp()
        is UserWallet.Hot -> wallet.backedUp || cloudBackupRepository.isBackedUp(wallet.walletId.stringValue)
    }

    fun flow(wallet: UserWallet): Flow<Boolean> = when (wallet) {
        is UserWallet.Cold -> flowOf(wallet.isColdBackedUp())
        is UserWallet.Hot -> if (wallet.backedUp) {
            flowOf(true)
        } else {
            cloudBackupRepository.isBackedUpFlow(wallet.walletId.stringValue).distinctUntilChanged()
        }
    }

    private fun UserWallet.Cold.isColdBackedUp(): Boolean = scanResponse.card.backupStatus?.isActive == true
}