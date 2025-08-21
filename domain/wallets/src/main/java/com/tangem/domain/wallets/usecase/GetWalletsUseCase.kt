package com.tangem.domain.wallets.usecase

import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.wallets.legacy.UserWalletsListManager
import com.tangem.domain.core.wallets.UserWalletsListRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Use case for getting list of user wallets
 *
 * @property userWalletsListManager user wallets list manager
 *
[REDACTED_AUTHOR]
 */
class GetWalletsUseCase(
    private val userWalletsListManager: UserWalletsListManager,
    private val userWalletsListRepository: UserWalletsListRepository,
    private val useNewListRepository: Boolean,
) {

    @Throws(IllegalArgumentException::class)
    operator fun invoke(): Flow<List<UserWallet>> = if (useNewListRepository) {
        userWalletsListRepository.userWallets.map { requireNotNull(it) }
    } else {
        userWalletsListManager.userWallets
    }

    @Throws(IllegalArgumentException::class)
    fun invokeSync(): List<UserWallet> = if (useNewListRepository) {
        userWalletsListRepository.userWallets.value!!
    } else {
        userWalletsListManager.userWalletsSync
    }
}