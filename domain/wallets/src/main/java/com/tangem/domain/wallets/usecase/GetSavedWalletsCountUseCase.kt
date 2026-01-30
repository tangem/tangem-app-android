package com.tangem.domain.wallets.usecase

import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.models.wallet.UserWallet
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class GetSavedWalletsCountUseCase(
    private val userWalletsListRepository: UserWalletsListRepository,
) {

    operator fun invoke(): Flow<List<UserWallet>> {
        return userWalletsListRepository.userWallets.map { requireNotNull(it) }
    }
}