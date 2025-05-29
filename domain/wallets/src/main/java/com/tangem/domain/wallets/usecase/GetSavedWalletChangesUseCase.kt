package com.tangem.domain.wallets.usecase

import com.tangem.domain.wallets.legacy.UserWalletsListManager
import com.tangem.domain.wallets.legacy.asLockable
import com.tangem.domain.wallets.legacy.isLockedSync
import com.tangem.domain.wallets.models.UserWallet
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter

class GetSavedWalletChangesUseCase(
    private val userWalletsListManager: UserWalletsListManager,
) {

    operator fun invoke(): Flow<List<UserWallet>> {
        return userWalletsListManager.userWallets
            .filter {
                userWalletsListManager.asLockable() ?: return@filter false
                return@filter if (userWalletsListManager.isLockedSync.not()) {
                    it.isNotEmpty()
                } else {
                    false
                }
            }
            .distinctUntilChanged()
    }
}