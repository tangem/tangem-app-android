package com.tangem.domain.wallets.usecase

import arrow.core.Either
import arrow.core.raise.either
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.common.wallets.error.UnlockWalletError
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.models.wallet.isLocked
import com.tangem.domain.wallets.repository.WalletsRepository

/**
 * Use case for unlocking a wallet using non-biometric methods.
 * If the wallet is already unlocked, it does nothing.
 *
 * **Does not** return [UnlockWalletError.AlreadyUnlocked] as an error, since the wallet is already unlocked.
 *
 * @see UnlockWalletUseCase
 */
class NonBiometricUnlockWalletUseCase(
    private val userWalletsListRepository: UserWalletsListRepository,
    private val walletsRepository: WalletsRepository,
) {

    suspend operator fun invoke(userWalletId: UserWalletId): Either<UnlockWalletError, Unit> = either {
        val userWallet = userWalletsListRepository.userWalletsSync()
            .find { it.walletId == userWalletId }
            ?: raise(UnlockWalletError.UserWalletNotFound)

        if (!userWallet.isLocked) {
            return@either
        }

        val method = when (userWallet) {
            is UserWallet.Cold -> UserWalletsListRepository.UnlockMethod.Scan()
            is UserWallet.Hot -> UserWalletsListRepository.UnlockMethod.AccessCode
        }

        userWalletsListRepository.unlock(userWalletId, method)
            .onRight {
                if (walletsRepository.useBiometricAuthentication()) {
                    // After successful unlock, set biometric lock if applicable
                    userWalletsListRepository.setLock(
                        userWalletId,
                        UserWalletsListRepository.LockMethod.Biometric,
                    )
                }
            }
            .bind()
    }
}