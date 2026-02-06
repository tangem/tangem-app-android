package com.tangem.domain.wallets.usecase

import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.wallets.legacy.UserWalletsListManager
import com.tangem.domain.wallets.legacy.asLockable
import com.tangem.domain.wallets.legacy.isLockedSync
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

class GetSavedWalletsCountUseCase(
    private val userWalletsListManager: UserWalletsListManager,
    private val userWalletsListRepository: UserWalletsListRepository,
    private val useNewRepository: Boolean,
) {

    @OptIn(ExperimentalCoroutinesApi::class)
    operator fun invoke(): Flow<List<UserWallet>> {
        if (useNewRepository) {
            return flowOf(Unit)
                .flatMapLatest {
                    userWalletsListRepository.load()
                    userWalletsListRepository.userWallets.map { wallets -> requireNotNull(wallets) }
                }
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