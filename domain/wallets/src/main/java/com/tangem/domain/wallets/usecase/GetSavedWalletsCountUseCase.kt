package com.tangem.domain.wallets.usecase

import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.models.wallet.UserWallet
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

class GetSavedWalletsCountUseCase(
    private val userWalletsListRepository: UserWalletsListRepository,
) {

    @OptIn(ExperimentalCoroutinesApi::class)
    operator fun invoke(): Flow<List<UserWallet>> {
        return flowOf(Unit)
            .flatMapLatest {
                userWalletsListRepository.load()
                userWalletsListRepository.userWallets.map { wallets -> requireNotNull(wallets) }
            }
    }
}