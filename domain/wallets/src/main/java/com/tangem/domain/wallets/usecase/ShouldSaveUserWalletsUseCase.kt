package com.tangem.domain.wallets.usecase

import com.tangem.domain.wallets.repository.WalletsRepository
import kotlinx.coroutines.flow.Flow

class ShouldSaveUserWalletsUseCase(private val walletsRepository: WalletsRepository) {

    operator fun invoke(): Flow<Boolean> = walletsRepository.shouldSaveUserWallets()
}