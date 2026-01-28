package com.tangem.domain.wallets.usecase

import com.tangem.domain.wallets.repository.WalletsRepository

@Deprecated("Hot wallet feature makes app always save user wallets. Do not use this method")
class ShouldSaveUserWalletsSyncUseCase(private val walletsRepository: WalletsRepository) {

    suspend operator fun invoke(): Boolean = walletsRepository.shouldSaveUserWalletsSync()
}