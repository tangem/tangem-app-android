package com.tangem.domain.wallets.usecase

import com.tangem.domain.common.wallets.UserWalletsListRepository

class HasSecuredWalletsUseCase(private val userWalletsListRepository: UserWalletsListRepository) {

    suspend operator fun invoke(): Boolean {
        return userWalletsListRepository.hasSecuredWallets()
    }
}