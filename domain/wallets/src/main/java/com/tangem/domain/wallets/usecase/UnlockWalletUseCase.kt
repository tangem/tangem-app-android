package com.tangem.domain.wallets.usecase

import arrow.core.Either
import arrow.core.raise.either
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.common.wallets.error.UnlockWalletError
import com.tangem.domain.models.wallet.UserWalletId

class UnlockWalletUseCase(
    private val userWalletsListRepository: UserWalletsListRepository,
    private val nonBiometricUnlockWalletUseCase: NonBiometricUnlockWalletUseCase,
) {

    suspend operator fun invoke(userWalletId: UserWalletId): Either<UnlockWalletError, Unit> = either {
        userWalletsListRepository.unlockAllWallets()
            .mapLeft { nonBiometricUnlockWalletUseCase(userWalletId).bind() }
    }
}