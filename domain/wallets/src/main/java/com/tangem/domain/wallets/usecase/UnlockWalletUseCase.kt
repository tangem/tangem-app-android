package com.tangem.domain.wallets.usecase

import arrow.core.Either
import arrow.core.raise.either
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.common.wallets.error.UnlockWalletError
import com.tangem.domain.models.wallet.UserWalletId

/**
 * Use case for unlocking a wallet using biometric authentication.
 * If biometric unlock fails, it falls back to non-biometric unlock methods.
 *
 * ** Does not** return [UnlockWalletError.AlreadyUnlocked] as an error, since the wallet is already unlocked.
 *
 * @see NonBiometricUnlockWalletUseCase
 */
class UnlockWalletUseCase(
    private val userWalletsListRepository: UserWalletsListRepository,
    private val nonBiometricUnlockWalletUseCase: NonBiometricUnlockWalletUseCase,
) {

    suspend operator fun invoke(userWalletId: UserWalletId): Either<UnlockWalletError, Unit> = either {
        userWalletsListRepository.unlock(userWalletId, UserWalletsListRepository.UnlockMethod.Biometric)
            .mapLeft { error ->
                when (error) {
                    UnlockWalletError.AlreadyUnlocked -> Unit
                    else -> nonBiometricUnlockWalletUseCase(userWalletId).bind()
                }
            }
    }
}