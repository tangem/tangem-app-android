package com.tangem.domain.wallets.usecase

import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.models.wallet.UserWallet
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Use case for getting list of user wallets
 *
 * @property userWalletsListRepository repository for getting list of user wallets
 *
[REDACTED_AUTHOR]
 */
class GetWalletsUseCase(
    private val userWalletsListRepository: UserWalletsListRepository,
) {

    @Throws(IllegalArgumentException::class)
    operator fun invoke(): Flow<List<UserWallet>> = userWalletsListRepository.userWallets.map { requireNotNull(it) }

    @Throws(IllegalArgumentException::class)
    fun invokeSync(): List<UserWallet> = userWalletsListRepository.userWallets.value!!
}