package com.tangem.domain.wallets.usecase

import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.models.scan.CardDTO
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Use case that checks if wallet need backup cards
 *
 * @property userWalletsListRepository repository for getting user wallets
 */
class IsNeedToBackupUseCase(
    private val userWalletsListRepository: UserWalletsListRepository,
) {

    operator fun invoke(id: UserWalletId): Flow<Boolean> {
        val userWalletsFlow = userWalletsListRepository.userWallets

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