package com.tangem.domain.wallets.usecase

import com.tangem.domain.models.scan.CardDTO
import com.tangem.domain.wallets.legacy.UserWalletsListManager
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.core.wallets.UserWalletsListRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Use case that checks if wallet need backup cards
 *
 * @property userWalletsListManager user wallets list manager
 */
class IsNeedToBackupUseCase(
    private val userWalletsListManager: UserWalletsListManager,
    private val userWalletsListRepository: UserWalletsListRepository,
    private val useNewRepository: Boolean,
) {

    operator fun invoke(id: UserWalletId): Flow<Boolean> {
        val userWalletsFlow = if (useNewRepository) {
            userWalletsListRepository.userWallets
        } else {
            userWalletsListManager.userWallets
        }

        return userWalletsFlow
            .map { wallets ->
                val wallet = wallets?.firstOrNull { it.walletId == id }
                if (wallet == null) {
                    false
                } else {
                    wallet is UserWallet.Cold && wallet.scanResponse.card.backupStatus is CardDTO.BackupStatus.NoBackup
                }
            }
    }
}