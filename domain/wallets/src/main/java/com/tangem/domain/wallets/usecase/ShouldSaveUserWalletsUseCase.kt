package com.tangem.domain.wallets.usecase

import com.tangem.domain.wallets.repository.WalletsRepository

class ShouldSaveUserWalletsUseCase(private val walletsRepository: WalletsRepository) {

    suspend operator fun invoke(): Boolean = walletsRepository.shouldSaveUserWallets()
}