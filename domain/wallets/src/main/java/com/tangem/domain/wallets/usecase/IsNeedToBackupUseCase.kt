package com.tangem.domain.wallets.usecase

import com.tangem.domain.models.scan.CardDTO
import com.tangem.domain.wallets.legacy.UserWalletsListManager
import com.tangem.domain.wallets.models.UserWalletId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Use case that checks if wallet need backup cards
 *
 * @property userWalletsListManager user wallets list manager
 */
class IsNeedToBackupUseCase(private val userWalletsListManager: UserWalletsListManager) {
// [REDACTED_TODO_COMMENT]
    operator fun invoke(id: UserWalletId): Flow<Boolean> {
        return userWalletsListManager.userWallets
            .map { wallets ->
                val wallet = wallets.firstOrNull { it.walletId == id }
                if (wallet == null) {
                    false
                } else {
                    wallet.scanResponse.card.backupStatus is CardDTO.BackupStatus.NoBackup
                }
            }
    }
}
