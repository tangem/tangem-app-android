package com.tangem.domain.wallets.usecase

import com.tangem.domain.wallets.legacy.UserWalletsListManager
import com.tangem.domain.wallets.legacy.asLockable
import com.tangem.domain.wallets.legacy.isLockedSync
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.core.wallets.UserWalletsListRepository
import kotlinx.coroutines.flow.*

class GetSavedWalletsCountUseCase(
    private val userWalletsListManager: UserWalletsListManager,
    private val userWalletsListRepository: UserWalletsListRepository,
    private val useNewRepository: Boolean,
) {

    operator fun invoke(): Flow<List<UserWallet>> {
        if (useNewRepository) {
            return userWalletsListRepository.userWallets.map { requireNotNull(it) }
        }

        return userWalletsListManager.savedWalletsCount
            .filter { count ->
                if (count == 0) return@filter true
                userWalletsListManager.asLockable() ?: return@filter false
                return@filter userWalletsListManager.isLockedSync.not()
            }
            .map {
                userWalletsListManager.userWalletsSync
            }
            .distinctUntilChanged()
    }
}